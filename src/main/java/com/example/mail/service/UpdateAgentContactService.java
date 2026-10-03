package com.example.mail.service;

import com.example.mail.dto.soap.aacc.UpdateAgentContactRequest;
import com.example.mail.dto.soap.aacc.UpdateAgentContactResponse;
import com.example.mail.repository.UpdateAgentContactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateAgentContactService {

    private static final Logger log = LoggerFactory.getLogger("MAIL_SERVICES_AVAAYA_LOGGER");

    @Autowired
    private UpdateAgentContactRepository updateAgentContactRepository;

    @Transactional
    public UpdateAgentContactResponse updateAgentContact(UpdateAgentContactRequest request) {

    	UpdateAgentContactResponse objResult = new UpdateAgentContactResponse();
        objResult.setMessage("Bad Request");

                if (convertString(request.getStrAgentID())) {
                    log.info("Inside UpdateAgentContact");
                    objResult.setMessage("Valid Input");
                    objResult.setContactResultID(-1);

                    try {
                        // Execute SP: Usp_UpdateAgentContact @AgentID
                        updateAgentContactRepository.updateAgentContactSp(request.getStrAgentID());

                        objResult.setContactResultID(0);
                        log.info("ExecuteNonQuery Successfully for AgentID: {}", request.getStrAgentID());

                    } catch (Exception ex) {
                        log.error("UpdateAgentContact() : strAgentID : " + request.getStrAgentID(), ex);
                    }
                }

        return objResult;
    }

    private boolean convertString(String strAgentID) {
        return strAgentID != null && !strAgentID.trim().isEmpty();
    }
}