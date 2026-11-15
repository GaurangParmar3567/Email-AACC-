package com.example.mail.controller;

import com.example.mail.dto.response.AgentSessionResponseDTO;
import com.example.mail.model.AgentSession;
import com.example.mail.service.AgentSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/agent-sessions", "/api/agents/status", "/api/agents/sessions", "/email-service/sbi/agent-sessions"})
@CrossOrigin(origins = "*")
public class AgentSessionController {

    private static final Logger logger = LoggerFactory.getLogger("MAIL_SERVICES_AUTH_LOGGER");

    @Autowired
    private AgentSessionService agentSessionService;

    public AgentSessionController() {
    }

    @Autowired
    public AgentSessionController(AgentSessionService agentSessionService) {
        this.agentSessionService = agentSessionService;
    }

    /**
     * Brings all agents based on online or offline in agent_session table.
     * Supports filter parameter: ?status=online or ?status=offline or ?filter=online
     * If no parameter is provided, brings all agents.
     *
     * Example URLs:
     *   GET /api/agent-sessions
     *   GET /api/agent-sessions?status=online
     *   GET /api/agent-sessions?status=offline
     *   GET /api/agent-sessions?filter=online
     *   GET /api/agent-sessions?filter=offline
     */
    @GetMapping
    public ResponseEntity<List<AgentSessionResponseDTO>> getAgentsByStatus(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "filter", required = false) String filter) {

        String effectiveFilter = (status != null && !status.trim().isEmpty()) ? status.trim() : filter;
        logger.info("getAgentsByStatus() - Fetching agent sessions with filter: {}",
                (effectiveFilter != null && !effectiveFilter.trim().isEmpty()) ? effectiveFilter.trim() : "ALL");

        List<AgentSessionResponseDTO> agents = agentSessionService.getAgentSessionsWithDetails(effectiveFilter);
        logger.info("getAgentsByStatus() - Retrieved {} agent records", agents.size());
        return ResponseEntity.ok(agents);
    }

    /**
     * Shortcut endpoint to bring only online agents.
     * GET /api/agent-sessions/online
     */
    @GetMapping("/online")
    public ResponseEntity<List<AgentSessionResponseDTO>> getOnlineAgents() {
        logger.info("getOnlineAgents() - Fetching online agents");
        List<AgentSessionResponseDTO> agents = agentSessionService.getAgentSessionsWithDetails("online");
        return ResponseEntity.ok(agents);
    }

    /**
     * Shortcut endpoint to bring only offline / logged out agents.
     * GET /api/agent-sessions/offline
     */
    @GetMapping("/offline")
    public ResponseEntity<List<AgentSessionResponseDTO>> getOfflineAgents() {
        logger.info("getOfflineAgents() - Fetching offline agents");
        List<AgentSessionResponseDTO> agents = agentSessionService.getAgentSessionsWithDetails("offline");
        return ResponseEntity.ok(agents);
    }

    /**
     * Returns raw AgentSession table entities without DTO mapping.
     * Supports ?status=online, ?status=offline, ?filter=online
     * GET /api/agent-sessions/raw
     */
    @GetMapping("/raw")
    public ResponseEntity<List<AgentSession>> getRawAgentSessions(
            @RequestParam(value = "status", required = false) String status,
            @RequestParam(value = "filter", required = false) String filter) {

        String effectiveFilter = (status != null && !status.trim().isEmpty()) ? status.trim() : filter;
        logger.info("getRawAgentSessions() - Fetching raw agent_session records with filter: {}",
                (effectiveFilter != null && !effectiveFilter.trim().isEmpty()) ? effectiveFilter.trim() : "ALL");

        List<AgentSession> sessions = agentSessionService.getRawAgentSessionsByStatus(effectiveFilter);
        return ResponseEntity.ok(sessions);
    }
}

