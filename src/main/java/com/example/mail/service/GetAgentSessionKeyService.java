package com.example.mail.service;

import com.example.mail.dto.soap.aacc.GetAgentSessionKeyRequest;
import com.example.mail.dto.soap.aacc.GetAgentSessionKeyResponse;
import com.example.mail.dto.soap.aacc.GetAgentSessionKeyResult;
import com.example.mail.repository.GetAgentSessionKeyRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetAgentSessionKeyService {

    private static final Logger log = LoggerFactory.getLogger("MAIL_SERVICES_AVAAYA_LOGGER");

    @Autowired
    private GetAgentSessionKeyRepository agentAuthorizationRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${db.sp.get-avaya-session:Usp_GetAvayaSession}")
    private String spGetAvayaSession;

    @Transactional
    public GetAgentSessionKeyResponse getAgentSessionKey(GetAgentSessionKeyRequest request) {
	    String message = "Bad Request";
        try {
            try {
                jdbcTemplate.update("EXEC " + spGetAvayaSession + " ?, ?",
                        request.getStrAvayaAgentID(), request.getStrSessionKey());
                log.info("getAgentSessionKey() - ExecuteNonQuery Successfully via {}", spGetAvayaSession);
            } catch (Exception spEx) {
                log.warn("Direct execution of {} failed, falling back to repository: {}", spGetAvayaSession, spEx.getMessage());
                agentAuthorizationRepository.getAgentSessionKeySp(
                        request.getStrAvayaAgentID(), request.getStrSessionKey());
                log.info("getAgentSessionKey() - ExecuteNonQuery Successfully via repository fallback");
            }
            message = "GetAgentSessionKey Triggered Successfully";
        } catch (Exception exception) {
            log.error("getAgentSessionKey() - Request received from agent: {}",
                    request.getStrAvayaAgentID(), exception);
        }
        return new GetAgentSessionKeyResponse(new GetAgentSessionKeyResult(
                request.getStrAvayaAgentID(), request.getStrSessionKey(), message));
    }

}