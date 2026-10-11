package com.smnk107.DocMind.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@ToString
public class ChatRequestDto {

    @NotBlank(message = "Question cannot be empty")
    private String question;

    private UUID documentId;

    private Integer topK;

    private Double minSimilarity;

    private String conversationId;

}