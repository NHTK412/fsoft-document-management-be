package com.example.document_management.repository;

import com.example.document_management.entity.ChatSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChatSessionRepository extends JpaRepository<ChatSession, Long> {
    List<ChatSession> findByProjectIdAndUserIdOrderByUpdatedAtDesc(Long projectId, Long userId);
    List<ChatSession> findByProjectIdOrderByUpdatedAtDesc(Long projectId);
    void deleteByProjectId(Long projectId);
}
