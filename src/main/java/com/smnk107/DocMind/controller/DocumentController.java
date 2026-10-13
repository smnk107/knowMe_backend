package com.smnk107.DocMind.controller;

import com.smnk107.DocMind.dto.ChatRequestDto;
import com.smnk107.DocMind.dto.ChatResponseDto;
import com.smnk107.DocMind.dto.DocumentResponseDto;
import com.smnk107.DocMind.service.DocumentMetadataService;
import com.smnk107.DocMind.service.RagService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

//@CrossOrigin(origins = "http://localhost:3000")https://know-me-tau-one.vercel.app/
@CrossOrigin(origins = "https://know-soumen.vercel.app")
@RestController
@RequestMapping("/api/v1/documents")
@Tag(name = "Document Management")
@Slf4j
@RequiredArgsConstructor
public class DocumentController {

    private final DocumentMetadataService documentMetadataService;
    private final RagService ragService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ResponseEntity<DocumentResponseDto> uploadDocument(@RequestParam("file") MultipartFile file){

        DocumentResponseDto documentResponseDto = documentMetadataService.uploadAndProcess(file);
        return ResponseEntity.ok(documentResponseDto);
    }

    @PostMapping("/ask")
    ResponseEntity<ChatResponseDto> askQuestion(@RequestBody ChatRequestDto chatRequestDto){

        log.info("DTO = {}", chatRequestDto);

        log.info("In side ask controller---question {}", chatRequestDto.getQuestion());
        ChatResponseDto chatResponseDto = ragService.askQuestion(chatRequestDto);
        return ResponseEntity.ok(chatResponseDto);
    }


    @PostMapping("/query")
    ResponseEntity<Flux<String> > streamQuestion(@RequestBody ChatRequestDto chatRequestDto){

        log.info("DTO = {}", chatRequestDto);

        log.info("In side ask controller---question {}", chatRequestDto.getQuestion());
        Flux<String> chatResponseDto = ragService.streamQuestionAnswer(chatRequestDto);
        return ResponseEntity.ok(chatResponseDto);
    }

    @PostMapping("/hello")
    public ResponseEntity<String> askQuestion(@RequestBody String body) {

        log.info("RAW BODY = [{}]", body);

        return ResponseEntity.ok(body);
    }

    @PostMapping("/aski")
    public ResponseEntity<String> askQuestions(@RequestBody String body) {

        log.info("RAW BODY = [{}]", body);

        return ResponseEntity.ok(body);
    }

    @GetMapping("/debug-jackson")
    ResponseEntity<String> debugJackson() {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            String json = "{\"question\":\"test\",\"topK\":5}";
            ChatRequestDto dto = mapper.readValue(json, ChatRequestDto.class);
            return ResponseEntity.ok("question=" + dto.getQuestion() + ", topK=" + dto.getTopK());
        } catch (Exception e) {
            return ResponseEntity.ok("ERROR: " + e.getMessage());
        }
    }
}
