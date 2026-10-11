package com.smnk107.DocMind.repository;


import com.smnk107.DocMind.entity.DocumentMetadata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DocumentMetadataRepository
        extends JpaRepository<DocumentMetadata, Long> {

    Optional<DocumentMetadata> findByFileHash(String fileHash);

    boolean existsByFileHash(String fileHash);
}