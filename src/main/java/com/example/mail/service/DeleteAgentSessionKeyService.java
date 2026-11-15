package com.example.mail.service;

import com.example.mail.dto.soap.aacc.DeleteAgentSessionKeyRequest;
import com.example.mail.dto.soap.aacc.DeleteAgentSessionKeyResponse;
import com.example.mail.dto.soap.aacc.DeleteAgentSessionKeyResult;
import com.example.mail.repository.DeleteAgentSessionKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteAgentSessionKeyService {

    private static final Logger log = LoggerFactory.getLogger("MAIL_SERVICES_AVAAYA_LOGGER");

    @Autowired
    private DeleteAgentSessionKeyRepository agentAuthorizationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${db.sp.delete-avaya-session:Usp_DeleteAvayaSession}")
    private String spDeleteAvayaSession;

    @Transactional
    public DeleteAgentSessionKeyResponse deleteAgentSessionKey(DeleteAgentSessionKeyRequest request) {
        String message = "Bad Request";
        try {
            try {
                jdbcTemplate.update("EXEC " + spDeleteAvayaSession + " ?, ?",
                        request.getStrAvayaAgentID(), request.getStrSessionKey());
                log.info("deleteAgentSessionKey() - ExecuteNonQuery Successfully via {}", spDeleteAvayaSession);
            } catch (Exception spEx) {
                log.warn("Direct execution of {} failed, falling back to repository: {}", spDeleteAvayaSession, spEx.getMessage());
                agentAuthorizationRepository.deleteAvayaSessionSp(
                        request.getStrAvayaAgentID(), request.getStrSessionKey());
                log.info("deleteAgentSessionKey() - ExecuteNonQuery Successfully via repository fallback");
            }
            message = "DeleteAgentSessionKey Triggered Successfully";
        } catch (Exception exception) {
            log.error("deleteAgentSessionKey() - Request received from agent: {}",
                    request.getStrAvayaAgentID(), exception);
        }
        return new DeleteAgentSessionKeyResponse(new DeleteAgentSessionKeyResult(
                request.getStrAvayaAgentID(), request.getStrSessionKey(), message));
    }

}