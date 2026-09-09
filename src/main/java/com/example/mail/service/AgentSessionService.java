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
}
