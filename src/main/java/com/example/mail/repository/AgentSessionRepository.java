package com.example.mail.repository;

import com.example.mail.model.AgentSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AgentSessionRepository extends JpaRepository<AgentSession, Long> {

    boolean existsByTokenId(String tokenId);

    Optional<AgentSession> findByTokenId(String tokenId);

    @Modifying
    @Transactional
    @Query("UPDATE AgentSession agentSession SET agentSession.lastActivity = :lastActivity "
            + "WHERE agentSession.tokenId = :tokenId AND agentSession.status = :status")
    int touchSessionIfStatusMatches(@Param("tokenId") String tokenId,
                                    @Param("status") String status,
                                    @Param("lastActivity") LocalDateTime lastActivity);

    @Modifying
    @Transactional
    @Query("DELETE FROM AgentSession a WHERE a.tokenId = :tokenId")
    int deleteByTokenId(@Param("tokenId") String tokenId);

    @Modifying
    @Transactional
    @Query("DELETE FROM AgentSession a WHERE a.sessionId = :sessionId")
    int deleteBySessionId(@Param("sessionId") Long sessionId);

    List<AgentSession> findByAgentIdAndStatus(Integer agentId, String status);

    List<AgentSession> findByAgentIdOrderByLoginTimeDesc(Integer agentId);

    List<AgentSession> findByStatusIgnoreCase(String status);

    List<AgentSession> findByStatusIgnoreCaseOrderByLastActivityDesc(String status);

    List<AgentSession> findByStatusNotIgnoreCase(String status);

    List<AgentSession> findByStatusNotIgnoreCaseOrderByLastActivityDesc(String status);

    List<AgentSession> findAllByOrderByLastActivityDesc();

    List<AgentSession> findAllByOrderByLoginTimeDesc();

    @Query("SELECT a FROM AgentSession a WHERE a.sessionId IN "
            + "(SELECT MAX(s.sessionId) FROM AgentSession s WHERE s.agentId IS NOT NULL GROUP BY s.agentId) "
            + "ORDER BY a.lastActivity DESC")
    List<AgentSession> findLatestSessionForEachAgent();

    @Query("SELECT a FROM AgentSession a WHERE a.sessionId IN "
            + "(SELECT MAX(s.sessionId) FROM AgentSession s WHERE s.agentId IS NOT NULL GROUP BY s.agentId) "
            + "AND UPPER(a.status) = UPPER(:status) "
            + "ORDER BY a.lastActivity DESC")
    List<AgentSession> findLatestSessionByStatus(@Param("status") String status);

    @Query("SELECT a FROM AgentSession a WHERE a.sessionId IN "
            + "(SELECT MAX(s.sessionId) FROM AgentSession s WHERE s.agentId IS NOT NULL GROUP BY s.agentId) "
            + "AND UPPER(a.status) != UPPER(:status) "
            + "ORDER BY a.lastActivity DESC")
    List<AgentSession> findLatestSessionByStatusNot(@Param("status") String status);
}
