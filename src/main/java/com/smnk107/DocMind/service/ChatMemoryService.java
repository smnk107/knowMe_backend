package com.smnk107.DocMind.service;

import com.smnk107.DocMind.entity.ChatMessage;
import com.smnk107.DocMind.enums.MessageRole;
import com.smnk107.DocMind.repository.ChatMessageRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ChatMemoryService {

    private final ChatMessageRepository repository;

    public void saveUserMessage(UUID conversationId, String content) {
        save(conversationId, MessageRole.USER, content);
    }

    public void saveAssistantMessage(UUID conversationId, String content) {
        save(conversationId, MessageRole.ASSISTANT, content);
    }

    private void save(
            UUID conversationId,
            MessageRole role,
            String content) {

        ChatMessage message = ChatMessage.builder()
                .conversationId(conversationId)
                .role(role)
                .content(content)
                .createdAt(LocalDateTime.now())
                .build();

        repository.save(message);
    }

    public List<ChatMessage> getHistory(UUID conversationId) {
        List<ChatMessage> messages =
                repository.findTop20ByConversationIdOrderByCreatedAtDesc(
                        conversationId
                );

        Collections.reverse(messages);

        return messages;
    }
}