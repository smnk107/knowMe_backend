package com.smnk107.DocMind.dto;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponseDto {

    private UUID id;
    private String fileName;
    private String fileType;
    private Long fileSize;
    private Integer totalChunks;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdatedAt;
}