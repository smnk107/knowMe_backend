package com.smnk107.DocMind.service;


import com.smnk107.DocMind.exceptions.DocumentProcessingException;
import com.smnk107.DocMind.exceptions.UnsupportedFileTypeException;
import lombok.extern.slf4j.Slf4j;
import org.apache.tika.Tika;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.tika.TikaDocumentReader;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class ParserService {

    private static final Set<String> SUPPORTED_TYPES = Set.of(
            "application/pdf",
            "text/plain",
            "text/markdown",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final Tika tika = new Tika();

    public List<Document> parse(MultipartFile file) {

        String fileName = file.getOriginalFilename();

        log.info("Starting document parsing: fileName={}, size={}",
                fileName,
                file.getSize());

        validateFile(file);

        try {
            /*
             * MultipartFile -> Resource
             */
            Resource resource = new ByteArrayResource(file.getBytes()) {

                @Override
                public String getFilename() {
                    return fileName;
                }
            };

            log.debug("MultipartFile converted to Resource: fileName={}",
                    fileName);

            /*
             * Detect actual MIME type using Apache Tika.
             */
            String detectedType = tika.detect(resource.getInputStream());

            log.info("Detected document type: fileName={}, type={}",
                    fileName,
                    detectedType);

            validateFileType(detectedType, fileName);

            /*
             * Generic parsing method.
             */
            List<Document> documents = parseGenericFile(resource);

            if (documents == null || documents.isEmpty()) {
                log.warn("No content extracted from document: {}",
                        fileName);

                throw new DocumentProcessingException(
                        "No content could be extracted from document: "
                                + fileName
                );
            }

            log.info("Document parsing completed: fileName={}, documents={}",
                    fileName,
                    documents.size());

            return documents;

        } catch (UnsupportedFileTypeException |
                 DocumentProcessingException ex) {

            throw ex;

        } catch (IOException ex) {

            log.error("Failed to read document: fileName={}",
                    fileName,
                    ex);

            throw new DocumentProcessingException(
                    "Failed to read document: " + fileName,
                    ex
            );

        } catch (Exception ex) {

            log.error("Unexpected error while parsing document: fileName={}",
                    fileName,
                    ex);

            throw new DocumentProcessingException(
                    "Failed to parse document: " + fileName,
                    ex
            );
        }
    }

    private void validateFile(MultipartFile file) {

        if (file == null) {
            log.warn("Upload request contains null file");

            throw new DocumentProcessingException(
                    "File must not be null"
            );
        }

        if (file.isEmpty()) {
            log.warn("Empty file uploaded: fileName={}",
                    file.getOriginalFilename());

            throw new DocumentProcessingException(
                    "File must not be empty"
            );
        }

        if (file.getOriginalFilename() == null ||
                file.getOriginalFilename().isBlank()) {

            log.warn("Uploaded file has no filename");

            throw new DocumentProcessingException(
                    "File name must not be empty"
            );
        }
    }

    private void validateFileType(
            String detectedType,
            String fileName) {

        if (!SUPPORTED_TYPES.contains(detectedType)) {

            log.warn(
                    "Unsupported file type: fileName={}, detectedType={}",
                    fileName,
                    detectedType
            );

            throw new UnsupportedFileTypeException(
                    "Unsupported file type: " + detectedType
            );
        }
    }

    private List<Document> parseGenericFile(Resource resource) {

        log.debug("Parsing generic resource: {}",
                resource.getFilename());

        TikaDocumentReader tikaDocumentReader = new TikaDocumentReader(resource);
        return tikaDocumentReader.read();
        /*
         * TODO:
         * Implement Tika-based parsing here.
         *
         * For example:
         * PDF  -> PDF parser
         * DOCX -> DOCX parser
         * TXT -> text parser
         */

    }
}