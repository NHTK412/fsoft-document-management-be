package com.example.document_management.repository;

import com.example.document_management.entity.UserSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserSessionRepository extends JpaRepository<UserSession, Long> {
    List<UserSession> findByUserIdOrderByLastActiveDesc(Long userId);
    Optional<UserSession> findByIdAndUserId(Long id, Long userId);
    void deleteByUserIdAndIdNot(Long userId, Long currentSessionId);
    void deleteByUserId(Long userId);
}
