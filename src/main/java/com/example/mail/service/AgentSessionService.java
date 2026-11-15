package com.example.mail.service;

import com.example.mail.dto.response.AgentSessionResponseDTO;
import com.example.mail.model.AgentSession;
import com.example.mail.model.UserMaster;
import com.example.mail.repository.AgentSessionRepository;
import com.example.mail.repository.UserMasterRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;

@Service
public class AgentSessionService {

    private static final Logger log = LoggerFactory.getLogger("MAIL_SERVICES_AUTH_LOGGER");

    @Autowired
    private AgentSessionRepository repository;

    @Autowired(required = false)
    private UserMasterRepo userMasterRepo;

    public AgentSessionService() {
    }

    public AgentSessionService(AgentSessionRepository repository) {
        this.repository = repository;
        this.userMasterRepo = null;
    }

    @Autowired
    public AgentSessionService(AgentSessionRepository repository,
                               @Autowired(required = false) UserMasterRepo userMasterRepo) {
        this.repository = repository;
        this.userMasterRepo = userMasterRepo;
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

    /**
     * Delete session from AgentSessionRepository comparing with tokenId (with numeric sessionId fallback)
     */
    @Transactional
    public int deleteSessionByTokenId(String tokenId) {
        if (tokenId == null || tokenId.trim().isEmpty()) {
            return 0;
        }
        String token = tokenId.trim();
        log.info("deleteSessionByTokenId() - Deleting session from AgentSessionRepository where tokenId='{}'", token);

        // 1. Delete comparing with tokenId directly
        int deleted = repository.deleteByTokenId(token);
        if (deleted > 0) {
            log.info("deleteSessionByTokenId() - Successfully deleted {} session(s) comparing with tokenId='{}'", deleted, token);
            return deleted;
        }

        // 2. Fallback: Check if tokenId was passed as numeric sessionId
        try {
            long numericSessionId = Long.parseLong(token);
            deleted = repository.deleteBySessionId(numericSessionId);
            if (deleted > 0) {
                log.info("deleteSessionByTokenId() - Successfully deleted session by numeric sessionId={}", numericSessionId);
                return deleted;
            }
        } catch (NumberFormatException ignored) {
        }

        // 3. Fallback: Check if passed token was agentId
        try {
            int agentId = Integer.parseInt(token);
            List<AgentSession> sessions = repository.findByAgentIdAndStatus(agentId, "Online");
            if (sessions != null && !sessions.isEmpty()) {
                for (AgentSession s : sessions) {
                    if (s.getTokenId() != null) {
                        deleted += repository.deleteByTokenId(s.getTokenId());
                    } else if (s.getSessionId() != null) {
                        deleted += repository.deleteBySessionId(s.getSessionId());
                    }
                }
                log.info("deleteSessionByTokenId() - Deleted {} active session(s) for agentId={}", deleted, agentId);
                return deleted;
            }
        } catch (NumberFormatException ignored) {
        }

        log.warn("deleteSessionByTokenId() - No session found to delete for tokenId='{}'", token);
        return 0;
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
        Optional<AgentSession> byToken = repository.findByTokenId(trimmed);
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
            Optional<AgentSession> bySessionId = repository.findById(numericValue);
            if (bySessionId.isPresent()) {
                AgentSession session = bySessionId.get();
                session.setLogoutTime(LocalDateTime.now());
                session.setLastActivity(LocalDateTime.now());
                session.setStatus("LoggedOut");
                return repository.save(session);
            }

            // Check if it's an agentId with active session
            List<AgentSession> activeSessions = repository.findByAgentIdAndStatus((int) numericValue, "Online");
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
            List<AgentSession> latestSessions = repository.findByAgentIdOrderByLoginTimeDesc((int) numericValue);
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

    /**
     * Retrieve raw latest AgentSession records per agent filtered by online, offline, or all.
     */
    public List<AgentSession> getRawAgentSessionsByStatus(String statusFilter) {
        if (statusFilter == null || statusFilter.trim().isEmpty() || "all".equalsIgnoreCase(statusFilter.trim())) {
            return repository.findLatestSessionForEachAgent();
        }

        String filter = statusFilter.trim().toLowerCase();
        if ("online".equals(filter)) {
            return repository.findLatestSessionByStatus("Online");
        } else if ("offline".equals(filter)) {
            return repository.findLatestSessionByStatusNot("Online");
        } else {
            return repository.findLatestSessionByStatus(statusFilter.trim());
        }
    }

    /**
     * Retrieve live agents joined with UserMaster, taking the latest session for each agent.
     * Supports filtering by online or offline.
     */
    public List<AgentSessionResponseDTO> getAgentSessionsWithDetails(String statusFilter) {
        // 1. Fetch latest session for each agent
        List<AgentSession> latestSessions = repository.findLatestSessionForEachAgent();
        Map<Integer, AgentSession> latestSessionByAgentId = new HashMap<>();
        if (latestSessions != null) {
            for (AgentSession s : latestSessions) {
                if (s.getAgentId() != null) {
                    latestSessionByAgentId.put(s.getAgentId(), s);
                }
            }
        }

        // 2. Fetch all users from UserMaster
        List<UserMaster> allUsers = Collections.emptyList();
        if (userMasterRepo != null) {
            try {
                allUsers = userMasterRepo.findAll();
            } catch (Exception ex) {
                log.warn("getAgentSessionsWithDetails() - Could not fetch all UserMaster records: {}", ex.getMessage());
            }
        }

        Set<Integer> processedAgentIds = new HashSet<>();
        List<AgentSessionResponseDTO> result = new ArrayList<>();

        // Join each user from UserMaster with their latest session
        for (UserMaster user : allUsers) {
            if (user == null) {
                continue;
            }
            Integer agentId = null;
            if (user.getAgentId() != null) {
                agentId = user.getAgentId().intValue();
            } else if (user.getId() != null) {
                agentId = user.getId().intValue();
            }

            if (agentId == null) {
                continue;
            }
            processedAgentIds.add(agentId);

            AgentSession session = latestSessionByAgentId.get(agentId);

            AgentSessionResponseDTO dto = new AgentSessionResponseDTO();
            dto.setAgentId(agentId);
            dto.setFirstName(user.getFirstName());
            dto.setLastName(user.getLastName());
            String first = user.getFirstName() != null ? user.getFirstName().trim() : "";
            String last = user.getLastName() != null ? user.getLastName().trim() : "";
            dto.setFullName((first + " " + last).trim());
            dto.setSkillId(user.getSkillId());

            if (session != null) {
                dto.setSessionId(session.getSessionId());
                dto.setStatus(session.getStatus());
                dto.setLoginTime(session.getLoginTime());
                dto.setLogoutTime(session.getLogoutTime());
                dto.setLastActivity(session.getLastActivity());
                dto.setIpAddress(session.getIpAddress());
                dto.setDeviceName(session.getDeviceName());
                dto.setTokenId(session.getTokenId());
            } else {
                dto.setStatus("Offline");
            }

            result.add(dto);
        }

        // 3. For any session in agent_session whose agentId was not in UserMaster
        for (Map.Entry<Integer, AgentSession> entry : latestSessionByAgentId.entrySet()) {
            Integer agentId = entry.getKey();
            if (!processedAgentIds.contains(agentId)) {
                AgentSession session = entry.getValue();
                AgentSessionResponseDTO dto = new AgentSessionResponseDTO();
                dto.setAgentId(agentId);
                dto.setSessionId(session.getSessionId());
                dto.setStatus(session.getStatus());
                dto.setLoginTime(session.getLoginTime());
                dto.setLogoutTime(session.getLogoutTime());
                dto.setLastActivity(session.getLastActivity());
                dto.setIpAddress(session.getIpAddress());
                dto.setDeviceName(session.getDeviceName());
                dto.setTokenId(session.getTokenId());
                result.add(dto);
            }
        }

        // 4. Apply status filter if specified
        if (statusFilter != null && !statusFilter.trim().isEmpty() && !"all".equalsIgnoreCase(statusFilter.trim())) {
            String filter = statusFilter.trim().toLowerCase();
            List<AgentSessionResponseDTO> filtered = new ArrayList<>();
            for (AgentSessionResponseDTO dto : result) {
                String status = dto.getStatus();
                if ("online".equals(filter)) {
                    if (status != null && status.equalsIgnoreCase("Online")) {
                        filtered.add(dto);
                    }
                } else if ("offline".equals(filter)) {
                    if (status == null || !status.equalsIgnoreCase("Online")) {
                        filtered.add(dto);
                    }
                } else {
                    if (status != null && status.equalsIgnoreCase(filter)) {
                        filtered.add(dto);
                    }
                }
            }
            result = filtered;
        }

        // 5. Sort: Online agents first, then by lastActivity / loginTime descending
        result.sort((a, b) -> {
            boolean aOnline = "Online".equalsIgnoreCase(a.getStatus());
            boolean bOnline = "Online".equalsIgnoreCase(b.getStatus());
            if (aOnline != bOnline) {
                return aOnline ? -1 : 1;
            }
            if (a.getLastActivity() != null && b.getLastActivity() != null) {
                return b.getLastActivity().compareTo(a.getLastActivity());
            }
            if (a.getLoginTime() != null && b.getLoginTime() != null) {
                return b.getLoginTime().compareTo(a.getLoginTime());
            }
            return 0;
        });

        return result;
    }
}
