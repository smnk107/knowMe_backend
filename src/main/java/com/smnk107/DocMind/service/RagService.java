package com.smnk107.DocMind.service;

import com.smnk107.DocMind.config.AppProperties;
import com.smnk107.DocMind.dto.*;
import com.smnk107.DocMind.entity.ChatMessage;
import com.smnk107.DocMind.enums.MessageRole;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter;
import org.springframework.ai.vectorstore.filter.FilterExpressionBuilder;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagService {


    private final VectorStore vectorStore;
    private final ChatClient chatClient;
    private final AppProperties appProperties;
    private final ChatMemoryService chatMemoryService;



    //to ask any thing related to document
    public ChatResponseDto askQuestion(ChatRequestDto request) {

        long startTime = System.currentTimeMillis();

        log.info("Processing query: '{}', scoped documentId: {}", request.getQuestion(), request.getDocumentId());

        UUID conversationId =
                getOrCreateConversationId(request.getConversationId());

        log.info(
                "Processing query: '{}' conversationId={}",
                request.getQuestion(),
                conversationId
        );

        // 1. Get previous conversation
        List<ChatMessage> history =
                chatMemoryService.getHistory(conversationId);

        List<Document> similarDocuments = this.retrieveRelevantDocuments(
                request.getQuestion(),
                request.getDocumentId(),
                request.getTopK(),
                request.getMinSimilarity()
        );


        List<CitationDto> citationDtos = similarDocuments.stream().map(this::mapToCitation).toList();

        String contextText = buildContextString(similarDocuments);


        //you have to use conversationId to remember the conversation

        String prompt = buildPrompt(
                request.getQuestion(),
                contextText,
                history
        );

        String answer = this.chatClient
                .prompt()
                .user(prompt)
                .call()
                .content();

        // 5. Save current conversation
        chatMemoryService.saveUserMessage(
                conversationId,
                request.getQuestion()
        );

        chatMemoryService.saveAssistantMessage(
                conversationId,
                answer
        );



        long responseTime = System.currentTimeMillis() - startTime;
        log.info("Completed Q&A in {} ms with {} citations", responseTime, citationDtos.size());
        return ChatResponseDto.builder()
                .answer(answer)
                //.conversationId(request.getConversationId() != null ? request.getConversationId() : UUID.randomUUID().toString())
                .citations(citationDtos)
                .responseTimeMs(responseTime).build();


    }

    private UUID getOrCreateConversationId(String conversationId) {

        if (conversationId == null || conversationId.isBlank()) {
            return UUID.randomUUID();
        }

        return UUID.fromString(conversationId);
    }


    public Flux<String> streamQuestionAnswer(ChatRequestDto requestDto) {

        UUID conversationId = getOrCreateConversationId(requestDto.getConversationId());


        List<ChatMessage> history =
                chatMemoryService.getHistory(conversationId);
        //requestDto.setConversationId(conversationId);

        List<Document> similarDocuments = this.retrieveRelevantDocuments(
                requestDto.getQuestion(),
                requestDto.getDocumentId(),
                requestDto.getTopK(),
                requestDto.getMinSimilarity()
        );


        List<CitationDto> citationDtos = similarDocuments.stream().map(this::mapToCitation).toList();

        String contextText = buildContextString(similarDocuments);


        //you have to use conversationId to remember the conversation
        String prompt = buildPrompt(
                requestDto.getQuestion(),
                contextText,
                history
        );

        StringBuilder assistantAnswer = new StringBuilder();

        // 4. Stream answer
        return chatClient.prompt()
                .user(prompt)
                .stream()
                .content()
                // Every token/chunk
                .doOnNext(chunk -> assistantAnswer.append(chunk))
                // Stream completed
                .doOnComplete(() -> {

                    ChatMessage assistantMessage = new ChatMessage();

                    assistantMessage.setConversationId(conversationId);
                    assistantMessage.setRole(MessageRole.ASSISTANT);
                    assistantMessage.setContent(assistantAnswer.toString());
                    assistantMessage.setCreatedAt(LocalDateTime.now());

                    chatMemoryService.saveUserMessage(
                            conversationId,
                            requestDto.getQuestion()
                    );

                    chatMemoryService.saveAssistantMessage(
                            conversationId,
                            assistantAnswer.toString()
                    );
                });
    }


    private String buildPrompt(
            @NotBlank(message = "Question cannot be empty") String question,
            String contextText,List<ChatMessage> history) {

        String historyText = history == null || history.isEmpty()
                ? "No previous conversation."
                : history.stream()
                .map(message ->
                        message.getRole() + ": " + message.getContent()
                )
                .collect(Collectors.joining("\n"));

        log.info("History added : {}",historyText);

        return String.format("""
            You are Soumen Mallick's AI version.

            Your job is to answer questions as if someone is talking directly
            to Soumen.

            PERSONALITY AND BEHAVIOR:
            - Speak naturally and conversationally.
            - Answer in first person when the question is about Soumen.
            - Use "I", "my", and "me" when appropriate.
            - Do not describe yourself as an AI unless specifically asked.
            - Do not invent personal information.
            - Use the provided knowledge/context as the source of truth
              about Soumen.
            - Check the Conversation History and use it to answer optimally.
            - Keep the answer compact and to the point.

            CONVERSATION HISTORY:
            ---------------------
            %s
            ---------------------

            KNOWLEDGE:
            The context below contains information about Soumen.

            If the answer can be found in the context, use that information.
            If the context does not contain enough information, say that you
            don't have enough information about that aspect of Soumen rather
            than making something up.

            IMPORTANT:
            - Use conversation history to understand references to previous messages.
            - Distinguish between Soumen's personal information and general knowledge.
            - Do not treat previous assistant responses as more authoritative
              than the personal knowledge/context.

            PERSONAL CONTEXT:
            ---------------------
            %s
            ---------------------

            CURRENT QUESTION:
            ---------------------
            %s
            ---------------------
            """,
                historyText ,
                contextText != null ? contextText : "",
                question);
    }

//    This method takes the chunks returned by similarity search and converts them into one big context string that you can send to the LLM.
    private String buildContextString(List<Document> similarDocuments) {
        if (similarDocuments == null || similarDocuments.isEmpty()) {
            return "";
        }

        return similarDocuments.stream().map(doc -> {
            String fileName = (String) doc.getMetadata().getOrDefault("fileName", "Unknown File");
            Object page = doc.getMetadata().getOrDefault("pageNumber", "N/A");
            return String.format("[Source: %s | Page: %s]\n%s", fileName, page, doc.getText());
        }).collect(Collectors.joining("\n\n---\n\n"));

    }


    public SearchResultDto searchSimilarChunks(SearchRequestDto request) {

        java.util.List<Document> matchedDocs = retrieveRelevantDocuments(request.getQuery(), request.getDocumentId(), request.getTopK(), request.getSimilaritySearch());

        List<CitationDto> citations = matchedDocs.stream().map(this::mapToCitation).toList();

        return SearchResultDto.builder().query(request.getQuery()).totalMatches(citations.size()).matches(citations).build();

    }

    // Citation = Document(returned by vectorstore ) + metadata
    private CitationDto mapToCitation(Document document) {

        Map<String, Object> meta = document.getMetadata();
        UUID docId = null;
        if (meta.get("documentId") != null) {
            try {
                docId = UUID.fromString(meta.get("documentId").toString());
            } catch (Exception ignore) {

            }
        }
        Integer chunkIndex = null;
        if (meta.get("chunkIndex") instanceof Number n) {
            chunkIndex = n.intValue();
        }

        Integer pageNumber = null;
        if (meta.get("pageNumber") instanceof Number n) {
            pageNumber = n.intValue();
        } else if (meta.get("page_number") instanceof Number n) {
            pageNumber = n.intValue();
        }

        Double score = null;
        if (meta.get("distance") instanceof Number n) {
            score = 1.0 - n.doubleValue();
        }
        return CitationDto.builder().documentId(docId).fileName((String) meta.getOrDefault("fileName", "Unknown")).chunkIndex(chunkIndex).pageNumber(pageNumber).snippet(document.getText()).similarityScore(score).metadata(meta).build();
    }

    //    this is very important document that fetches the similar result from vector db
    private List<Document> retrieveRelevantDocuments(@NotBlank(message = "Query cannot be empty") String query, UUID documentId, Integer topK, Double similaritySearch) {

        int effectiveTopK = (topK != null && topK > 0) ? topK : appProperties.getRag().getTopK();

        double effectiveSimilarity = (similaritySearch != null) ? similaritySearch : appProperties.getRag().getSimilarityThreshold();

        SearchRequest.Builder searchRequestBuilder = SearchRequest.builder().
                query(query).
                topK(effectiveTopK);

        if (effectiveSimilarity > 0.0) {
            searchRequestBuilder.similarityThreshold(effectiveSimilarity);
        }

        FilterExpressionBuilder b = new FilterExpressionBuilder();
        Filter.Expression filterExpression;
//        if (documentId != null) {
//            log.info("Filtering from document :");
//
//            filterExpression =
//                   // b.and(
////                            introduced userId filter
//                           // b.eq("userId", user.getId().toString()),
//                            b.eq("documentId", documentId.toString()//)
//
//                    ).build();
//            searchRequestBuilder.filterExpression(filterExpression);
//
//
//        }
//        else {
//                // introduced userId filter
//                filterExpression = b.eq("userId", user.getId().toString()).build();


//        }
//        searchRequestBuilder.filterExpression(filterExpression);


        try {
            List<Document> documents = vectorStore.similaritySearch(searchRequestBuilder.build());
            log.info("Retrieved {} chunks for query: '{}' (scoped docId: {})", documents.size(), query, documentId);
            return documents;
        } catch (Exception e) {
            log.error("Similarity search failed for query: '{}'", query, e);
            return Collections.emptyList();
        }


    }


}
