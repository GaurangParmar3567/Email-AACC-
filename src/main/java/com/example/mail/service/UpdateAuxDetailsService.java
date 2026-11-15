package com.example.mail.service;

import com.example.mail.dto.soap.aacc.UpdateAuxDetailsRequest;
import com.example.mail.dto.soap.aacc.UpdateAuxDetailsResponse;
import com.example.mail.repository.UpdateAuxDetailsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateAuxDetailsService {

    private static final Logger log = LoggerFactory.getLogger("MAIL_SERVICES_AVAAYA_LOGGER");

    @Autowired
    private UpdateAuxDetailsRepository auxRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Value("${db.sp.update-aux-details:USP_UpdateAuxDetails}")
    private String spUpdateAuxDetails;

    @Transactional
    public UpdateAuxDetailsResponse updateAuxDetails(UpdateAuxDetailsRequest request) {
        log.info("updateAuxDetails() - Inside UpdateAuxDetails");

        try {
            log.info("updateAuxDetails() - Event Start");

            // Execute Stored Procedure via dynamic SP name with repository fallback
            try {
                jdbcTemplate.update("EXEC " + spUpdateAuxDetails + " ?, ?, ?, ?",
                        request.getAgentID(),
                        request.getAuxCode(),
                        request.getStartTime(),
                        request.getEndTime());
                log.info("updateAuxDetails() - ExecuteNonQuery Successfully for AgentID: {} using {}", request.getAgentID(), spUpdateAuxDetails);
            } catch (Exception spEx) {
                log.warn("updateAuxDetails() - Direct execution of {} failed, falling back to repository: {}", spUpdateAuxDetails, spEx.getMessage());
                auxRepository.updateAuxDetails(
                    request.getAgentID(),
                    request.getAuxCode(),
                    request.getStartTime(),
                    request.getEndTime()
                );
                log.info("updateAuxDetails() - ExecuteNonQuery Successfully for AgentID: {} via repository fallback", request.getAgentID());
            }

            return new UpdateAuxDetailsResponse("UpdateAuxDetails Triggered");

        } catch (Exception ex) {
            log.error("updateAuxDetails() - AgentID : " + request.getAgentID(), ex);
            return new UpdateAuxDetailsResponse("Bad Request");
        }
    }
}