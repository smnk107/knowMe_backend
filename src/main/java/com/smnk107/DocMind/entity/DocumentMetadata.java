package com.smnk107.DocMind.entity;

import com.smnk107.DocMind.enums.DocumentStatus;
import jakarta.persistence.*;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "document_metadata")
@Builder
@Getter
@Setter
public class DocumentMetadata {


    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false)
    private String fileName;

    @Column(nullable = false)
    private String fileType;

    private Long fileSize;

    //@Column(nullable = false, unique = true)
    private String fileHash;

    private Integer totalChunks;
    private Integer pageCount;

    @Column(nullable = false)
    private LocalDateTime createdAt;

    @Enumerated(EnumType.STRING)
    private DocumentStatus status;

    private LocalDateTime lastUpdatedAt;
    private String errorMessage;
}
