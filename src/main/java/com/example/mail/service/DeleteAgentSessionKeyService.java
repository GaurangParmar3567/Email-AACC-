package com.example.mail.service;

import com.example.mail.dto.soap.aacc.DeleteAgentSessionKeyRequest;
import com.example.mail.dto.soap.aacc.DeleteAgentSessionKeyResponse;
import com.example.mail.dto.soap.aacc.DeleteAgentSessionKeyResult;
import com.example.mail.repository.DeleteAgentSessionKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DeleteAgentSessionKeyService {

    private static final Logger log = LoggerFactory.getLogger("MAIL_SERVICES_AVAAYA_LOGGER");

    @Autowired
    private DeleteAgentSessionKeyRepository agentAuthorizationRepository;

    @Transactional
    public DeleteAgentSessionKeyResponse deleteAgentSessionKey(DeleteAgentSessionKeyRequest request) {
        String message = "Bad Request";
        try {
            agentAuthorizationRepository.deleteAvayaSessionSp(
                    request.getStrAvayaAgentID(), request.getStrSessionKey());
            log.info("ExecuteNonQuery Successfully");
            message = "DeleteAgentSessionKey Triggered Successfully";
        } catch (Exception exception) {
            log.error("DeleteAgentSessionKey request received from agent: {}",
                    request.getStrAvayaAgentID(), exception);
        }
        return new DeleteAgentSessionKeyResponse(new DeleteAgentSessionKeyResult(
                request.getStrAvayaAgentID(), request.getStrSessionKey(), message));
    }

}