package com.example.mail.repository;

import com.example.mail.model.AgentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AgentSessionRepository extends JpaRepository<AgentSession, Long> {
    boolean existsByTokenId(String tokenId);
    Optional<AgentSession> findByTokenId(String tokenId);
    Optional<AgentSession> findByTokenIdAndStatus(String tokenId, String status);
    List<AgentSession> findByAgentIdAndStatus(Integer agentId, String status);
    List<AgentSession> findByAgentIdOrderByLoginTimeDesc(Integer agentId);
}
