package com.example.document_management.repository;

import com.example.document_management.entity.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatMessageRepository extends JpaRepository<ChatMessage, Long> {
    List<ChatMessage> findByChatSessionIdOrderByCreatedAtAsc(Long sessionId);
    long countByChatSessionId(Long sessionId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.chatSession.project.id = :projectId")
    long countByProjectId(@org.springframework.data.repository.query.Param("projectId") Long projectId);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(m) FROM ChatMessage m WHERE m.chatSession.project.id = :projectId AND m.createdAt >= :since")
    long countByProjectIdAndCreatedAtAfter(@org.springframework.data.repository.query.Param("projectId") Long projectId, @org.springframework.data.repository.query.Param("since") java.time.Instant since);
}
