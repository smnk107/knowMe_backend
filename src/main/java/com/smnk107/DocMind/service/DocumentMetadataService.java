package com.smnk107.DocMind.service;


import com.smnk107.DocMind.dto.DocumentResponseDto;
import com.smnk107.DocMind.entity.DocumentMetadata;
import com.smnk107.DocMind.enums.DocumentStatus;
import com.smnk107.DocMind.exceptions.DocumentProcessingException;
import com.smnk107.DocMind.exceptions.DuplicateResourceException;
import com.smnk107.DocMind.exceptions.FileSizeExceededException;
import com.smnk107.DocMind.exceptions.UnsupportedFileTypeException;
import com.smnk107.DocMind.repository.DocumentMetadataRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentMetadataService {

    private final DocumentMetadataRepository documentMetadataRepository;
    private final ParserService parserService;
    private final IngestionService ingestionService;


    public DocumentResponseDto uploadAndProcess(MultipartFile file) {

        log.info("Starting document upload and processing: {}",
                file.getOriginalFilename());

        try {

            // 1. Create metadata
            DocumentMetadata metadata = createMetadata(file);

            // 2. Save metadata
            metadata = documentMetadataRepository.save(metadata);

            log.info("Document metadata saved. id={}, fileName={}",
                    metadata.getId(),
                    metadata.getFileName());

            // 3. Parse document
            List<Document> documents = parserService.parse(file);

            log.info("Document parsed successfully. id={}, parsedDocuments={}",
                    metadata.getId(),
                    documents.size());

            // 4. Ingest parsed documents
            int totalChunks =
                    ingestionService.ingest(metadata, documents);

            log.info("Document ingestion completed. id={}, chunks={}",
                    metadata.getId(),
                    totalChunks);

            // 5. Update metadata with chunk count
            metadata.setTotalChunks(totalChunks);
            metadata.setLastUpdatedAt(LocalDateTime.now());

            metadata = documentMetadataRepository.save(metadata);

            log.info("Document processing completed successfully. id={}",
                    metadata.getId());

            return mapToDto(metadata);

        } catch (DuplicateResourceException ex) {

            log.warn("Duplicate document: {}",
                    file.getOriginalFilename());

            throw ex;

        } catch (DocumentProcessingException ex) {

            log.error("Document processing failed: {}",
                    file.getOriginalFilename(), ex);

            throw ex;

        } catch (Exception ex) {

            log.error("Unexpected error while processing document: {}",
                    file.getOriginalFilename(), ex);

            throw new DocumentProcessingException(
                    "Failed to process document: "
                            + file.getOriginalFilename(),
                    ex
            );
        }
    }

    private DocumentMetadata createMetadata(MultipartFile file) {

        // Calculate hash / validate / duplicate check here

        return DocumentMetadata.builder()
                .fileName(file.getOriginalFilename())
                .fileType(file.getContentType())
                .fileSize(file.getSize())
                .createdAt(LocalDateTime.now())
                .lastUpdatedAt(LocalDateTime.now())
                .totalChunks(0)
                .status(DocumentStatus.UPLOADING)
                .build();
    }

    private DocumentResponseDto mapToDto(DocumentMetadata metadata) {

        return DocumentResponseDto.builder()
                .id(metadata.getId())
                .fileName(metadata.getFileName())
                .fileType(metadata.getFileType())
                .fileSize(metadata.getFileSize())
                .totalChunks(metadata.getTotalChunks())
                .createdAt(metadata.getCreatedAt())
                .lastUpdatedAt(metadata.getLastUpdatedAt())

                .build();
    }
}
