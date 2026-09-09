package com.example.mail.repository;

import com.example.mail.model.AgentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AgentSessionRepository extends JpaRepository<AgentSession, Long> {
    boolean existsByTokenId(String tokenId);
}
