package com.smnk107.DocMind.repository;

import com.smnk107.DocMind.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ChatMessageRepository
        extends JpaRepository<ChatMessage, UUID> {

    List<ChatMessage> findTop20ByConversationIdOrderByCreatedAtDesc(
            UUID conversationId
    );
}
