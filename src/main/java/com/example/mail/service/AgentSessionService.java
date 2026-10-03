package com.example.mail.service;

import com.example.mail.model.AgentSession;
import com.example.mail.repository.AgentSessionRepository;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;

@Service
public class AgentSessionService {

    private final AgentSessionRepository repository;

    public AgentSessionService(AgentSessionRepository repository) {
        this.repository = repository;
    }

    public AgentSession createSession(String agentLoginId, Integer agentId, String ipAddress, String deviceName, String tokenId) {
        AgentSession session = new AgentSession();
        session.setAgentId(agentId != null ? agentId : 0);
        session.setLoginTime(LocalDateTime.now());
        session.setLastActivity(LocalDateTime.now());
        session.setStatus("Online");
        session.setIpAddress(ipAddress);
        session.setDeviceName(deviceName);
        session.setTokenId(createUniqueSessionIdString(agentLoginId, agentId, ipAddress, deviceName, tokenId));
        return repository.save(session);
    }

    public String createUniqueSessionIdString(String agentLoginId, Integer agentId, String ipAddress, String deviceName, String tokenId) {
        String base = String.valueOf(System.currentTimeMillis())
                + "|" + (agentLoginId != null ? agentLoginId : "")
                + "|" + (agentId != null ? agentId : 0)
                + "|" + (ipAddress != null ? ipAddress : "")
                + "|" + (deviceName != null ? deviceName : "")
                + "|" + (tokenId != null ? tokenId : "");

        String candidate = hash(base).substring(0, 16);
        int suffix = 1;
        while (repository.existsByTokenId(candidate)) {
            candidate = "AGT-" + hash(base + "|" + suffix).substring(0, 16).toUpperCase();
            suffix++;
        }
        return candidate;
    }

    private String hash(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encoded = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : encoded) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 not available", ex);
        }
    }

    public AgentSession updateLogout(Long sessionId) {
        AgentSession session = repository.findById(sessionId).orElse(null);
        if (session == null) {
            return null;
        }
        session.setLogoutTime(LocalDateTime.now());
        session.setLastActivity(LocalDateTime.now());
        session.setStatus("LoggedOut");
        return repository.save(session);
    }

    public AgentSession updateLogoutBySessionOrToken(String sessionOrToken) {
        return logoutAgentByUsernameOrId(sessionOrToken);
    }

    public AgentSession logoutAgentByUsernameOrId(String identifier) {
        if (identifier == null || identifier.trim().isEmpty()) {
            return null;
        }
        String trimmed = identifier.trim();

        // 1. Try by tokenId
        java.util.Optional<AgentSession> byToken = repository.findByTokenId(trimmed);
        if (byToken.isPresent()) {
            AgentSession session = byToken.get();
            session.setLogoutTime(LocalDateTime.now());
            session.setLastActivity(LocalDateTime.now());
            session.setStatus("LoggedOut");
            return repository.save(session);
        }

        // 2. Try as numeric sessionId or agentId
        try {
            long numericValue = Long.parseLong(trimmed);

            // Check if it's sessionId directly
            java.util.Optional<AgentSession> bySessionId = repository.findById(numericValue);
            if (bySessionId.isPresent()) {
                AgentSession session = bySessionId.get();
                session.setLogoutTime(LocalDateTime.now());
                session.setLastActivity(LocalDateTime.now());
                session.setStatus("LoggedOut");
                return repository.save(session);
            }

            // Check if it's an agentId with active session
            java.util.List<AgentSession> activeSessions = repository.findByAgentIdAndStatus((int) numericValue, "Online");
            if (activeSessions != null && !activeSessions.isEmpty()) {
                AgentSession lastSession = null;
                for (AgentSession s : activeSessions) {
                    s.setLogoutTime(LocalDateTime.now());
                    s.setLastActivity(LocalDateTime.now());
                    s.setStatus("LoggedOut");
                    lastSession = repository.save(s);
                }
                return lastSession;
            }

            // Fallback: latest session for this agentId
            java.util.List<AgentSession> latestSessions = repository.findByAgentIdOrderByLoginTimeDesc((int) numericValue);
            if (latestSessions != null && !latestSessions.isEmpty()) {
                AgentSession session = latestSessions.get(0);
                session.setLogoutTime(LocalDateTime.now());
                session.setLastActivity(LocalDateTime.now());
                session.setStatus("LoggedOut");
                return repository.save(session);
            }
        } catch (NumberFormatException ignored) {
        }

        return null;
    }
}
