package com.example.mail.service;

import com.example.mail.dto.soap.aacc.GetAgentSessionKeyRequest;
import com.example.mail.dto.soap.aacc.GetAgentSessionKeyResponse;
import com.example.mail.dto.soap.aacc.GetAgentSessionKeyResult;
import com.example.mail.repository.GetAgentSessionKeyRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GetAgentSessionKeyService {

    private static final Logger log = LoggerFactory.getLogger("MAIL_SERVICES_AVAAYA_LOGGER");

    @Autowired
    private GetAgentSessionKeyRepository agentAuthorizationRepository;

    @Transactional
    public GetAgentSessionKeyResponse getAgentSessionKey(GetAgentSessionKeyRequest request) {
	    String message = "Bad Request";
        try {
            agentAuthorizationRepository.getAgentSessionKeySp(
                    request.getStrAvayaAgentID(), request.getStrSessionKey());
            log.info("ExecuteNonQuery Successfully");
            message = "GetAgentSessionKey Triggered Successfully";
        } catch (Exception exception) {
            log.error("GetAgentSessionKey request received from agent: {}",
                    request.getStrAvayaAgentID(), exception);
        }
        return new GetAgentSessionKeyResponse(new GetAgentSessionKeyResult(
                request.getStrAvayaAgentID(), request.getStrSessionKey(), message));
    }

}