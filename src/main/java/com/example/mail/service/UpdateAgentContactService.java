package com.example.mail.service;

import com.example.mail.dto.soap.aacc.UpdateAgentContactRequest;
import com.example.mail.dto.soap.aacc.UpdateAgentContactResponse;
import com.example.mail.repository.UpdateAgentContactRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateAgentContactService {

    private static final Logger log = LoggerFactory.getLogger("MAIL_SERVICES_AVAAYA_LOGGER");

    @Autowired
    private UpdateAgentContactRepository updateAgentContactRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${db.sp.update-agent-contact:Usp_UpdateAgentContact}")
    private String spUpdateAgentContact;

    @Transactional
    public UpdateAgentContactResponse updateAgentContact(UpdateAgentContactRequest request) {

    	UpdateAgentContactResponse objResult = new UpdateAgentContactResponse();
        objResult.setMessage("Bad Request");

                if (convertString(request.getStrAgentID())) {
                    log.info("Inside UpdateAgentContact");
                    objResult.setMessage("Valid Input");
                    objResult.setContactResultID(-1);

                    try {
                        try {
                            jdbcTemplate.update("EXEC " + spUpdateAgentContact + " ?", request.getStrAgentID().trim());
                            objResult.setContactResultID(0);
                            log.info("ExecuteNonQuery Successfully for AgentID: {} using {}", request.getStrAgentID(), spUpdateAgentContact);
                        } catch (Exception spEx) {
                            log.warn("Direct execution of {} failed, falling back to repository: {}", spUpdateAgentContact, spEx.getMessage());
                            updateAgentContactRepository.updateAgentContactSp(request.getStrAgentID().trim());
                            objResult.setContactResultID(0);
                            log.info("ExecuteNonQuery Successfully for AgentID: {} via repository fallback", request.getStrAgentID());
                        }

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