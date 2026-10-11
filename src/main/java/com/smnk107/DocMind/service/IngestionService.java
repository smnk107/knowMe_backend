package com.smnk107.DocMind.service;

import com.smnk107.DocMind.config.AppProperties;
import com.smnk107.DocMind.entity.DocumentMetadata;
import com.smnk107.DocMind.enums.DocumentStatus;
import com.smnk107.DocMind.exceptions.DocumentProcessingException;
import com.smnk107.DocMind.repository.DocumentMetadataRepository;
import lombok.AllArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class IngestionService {


    private final DocumentMetadataRepository documentMetadataRepo;
    private final VectorStore vectorStore;
    private final AppProperties appProperties;


    public int ingest(
            DocumentMetadata metadata,
            List<Document> documents) {

        UUID documentId = metadata.getId();

        log.info(
                "Starting document ingestion: documentId={}, fileName={}",
                documentId,
                metadata.getFileName()
        );

        try {

            // --------------------------------------------------
            // 1. Validate parsed documents
            // --------------------------------------------------

            if (documents == null || documents.isEmpty()) {

                log.warn(
                        "No parsed documents available: documentId={}",
                        documentId
                );

                markFailed(
                        metadata,
                        "Document appears to be empty or unscannable"
                );

                return 0;
            }

            log.info(
                    "Parsed documents received: documentId={}, count={}",
                    documentId,
                    documents.size()
            );

            // --------------------------------------------------
            // 2. Create TokenTextSplitter
            // --------------------------------------------------

            AppProperties.RagProperties rag =
                    appProperties.getRag();

            TokenTextSplitter splitter =
                    TokenTextSplitter.builder()
                            .withChunkSize(rag.getChunkSize())
                            .withMinChunkSizeChars(
                                    rag.getMinChunkSizeChars()
                            )
                            .withMinChunkLengthToEmbed(
                                    rag.getMinChunkLengthToEmbed()
                            )
                            .withMaxNumChunks(
                                    rag.getMaxNumChunks()
                            )
                            .withKeepSeparator(
                                    rag.isKeepSeparator()
                            )
                            .withPunctuationMarks(
                                    rag.getPunctuationList()
                            )
                            .build();

            log.debug(
                    "TokenTextSplitter configured: chunkSize={}, " +
                            "minChunkSizeChars={}, minChunkLengthToEmbed={}, " +
                            "maxNumChunks={}",
                    rag.getChunkSize(),
                    rag.getMinChunkSizeChars(),
                    rag.getMinChunkLengthToEmbed(),
                    rag.getMaxNumChunks()
            );

            // --------------------------------------------------
            // 3. Split documents into chunks
            // --------------------------------------------------

            List<Document> chunks = splitter.apply(documents);

            if (chunks == null || chunks.isEmpty()) {

                log.warn(
                        "No chunks generated: documentId={}",
                        documentId
                );

                markFailed(
                        metadata,
                        "No chunks could be generated from document"
                );

                return 0;
            }

            log.info(
                    "Document split into chunks: documentId={}, chunks={}",
                    documentId,
                    chunks.size()
            );

            // --------------------------------------------------
            // 4. Enrich chunk metadata
            // --------------------------------------------------

            List<Document> enrichedChunks =
                    enrichChunks(metadata, chunks);

            log.debug(
                    "Chunk metadata enrichment completed: " +
                            "documentId={}, chunks={}",
                    documentId,
                    enrichedChunks.size()
            );

            // --------------------------------------------------
            // 5. Store chunks in Vector Store
            // --------------------------------------------------

            log.info(
                    "Storing chunks in vector store: documentId={}, chunks={}",
                    documentId,
                    enrichedChunks.size()
            );

            vectorStore.add(enrichedChunks);

            log.info(
                    "Chunks stored successfully: documentId={}, chunks={}",
                    documentId,
                    enrichedChunks.size()
            );

            // --------------------------------------------------
            // 6. Update metadata
            // --------------------------------------------------

            metadata.setTotalChunks(enrichedChunks.size());
            metadata.setStatus(DocumentStatus.PROCESSED);
            metadata.setErrorMessage(null);
            metadata.setLastUpdatedAt(LocalDateTime.now());

            documentMetadataRepo.save(metadata);

            log.info(
                    "Document ingestion completed successfully: " +
                            "documentId={}, totalChunks={}",
                    documentId,
                    enrichedChunks.size()
            );

            return enrichedChunks.size();

        } catch (DocumentProcessingException ex) {

            log.error(
                    "Document processing failed during ingestion: " +
                            "documentId={}, fileName={}",
                    documentId,
                    metadata.getFileName(),
                    ex
            );

            markFailed(
                    metadata,
                    ex.getMessage()
            );

            throw ex;

        } catch (Exception ex) {

            log.error(
                    "Unexpected error during document ingestion: " +
                            "documentId={}, fileName={}",
                    documentId,
                    metadata.getFileName(),
                    ex
            );

            String errorMessage =
                    "Failed to ingest document: "
                            + metadata.getFileName();

            markFailed(metadata, errorMessage);

            throw new DocumentProcessingException(
                    errorMessage,
                    ex
            );
        }
    }

    private List<Document> enrichChunks(
            DocumentMetadata metadata,
            List<Document> chunks) {

        List<Document> enrichedChunks =
                new ArrayList<>(chunks.size());

        for (int i = 0; i < chunks.size(); i++) {

            Document chunk = chunks.get(i);

            Map<String, Object> enrichedMetadata =
                    new HashMap<>(chunk.getMetadata());

            enrichedMetadata.put(
                    "documentId",
                    metadata.getId().toString()
            );

            enrichedMetadata.put(
                    "fileName",
                    metadata.getFileName()
            );

            enrichedMetadata.put(
                    "contentType",
                    metadata.getFileType()
            );

            enrichedMetadata.put(
                    "chunkIndex",
                    i
            );

            // Preserve page number if supplied by parser
            Object pageNumber =
                    chunk.getMetadata().get("page_number");

            if (pageNumber == null) {
                pageNumber =
                        chunk.getMetadata().get("pageNumber");
            }

            if (pageNumber != null) {
                enrichedMetadata.put(
                        "pageNumber",
                        pageNumber
                );
            }

            Document enrichedDocument =
                    new Document(
                            chunk.getText(),
                            enrichedMetadata
                    );

            enrichedChunks.add(enrichedDocument);
        }

        return enrichedChunks;
    }

    private void markFailed(
            DocumentMetadata metadata,
            String errorMessage) {

        metadata.setStatus(DocumentStatus.FAILED);
        metadata.setErrorMessage(errorMessage);
        metadata.setLastUpdatedAt(LocalDateTime.now());

        documentMetadataRepo.save(metadata);

        log.warn(
                "Document marked as FAILED: documentId={}, reason={}",
                metadata.getId(),
                errorMessage
        );
    }
}
