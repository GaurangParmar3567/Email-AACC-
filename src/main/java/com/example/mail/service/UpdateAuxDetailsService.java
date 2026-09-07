package com.example.mail.service;

import com.example.mail.dto.soap.aacc.UpdateAuxDetailsRequest;
import com.example.mail.dto.soap.aacc.UpdateAuxDetailsResponse;
import com.example.mail.repository.UpdateAuxDetailsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UpdateAuxDetailsService {

    private static final Logger log = LoggerFactory.getLogger("MAIL_SERVICES_AVAAYA_LOGGER");

    @Autowired
    private UpdateAuxDetailsRepository auxRepository;

    @Transactional
    public UpdateAuxDetailsResponse updateAuxDetails(UpdateAuxDetailsRequest request) {
        log.info("Inside UpdateAuxDetails");

        try {
            log.info("UpdateAuxDetails() Event Start");

            // Execute Stored Procedure USP_UpdateAuxDetails via Repository
            auxRepository.updateAuxDetails(
                request.getAgentID(),
                request.getAuxCode(),
                request.getStartTime(),
                request.getEndTime()
            );

            log.info("ExecuteNonQuery Successfully for AgentID: {}", request.getAgentID());
            return new UpdateAuxDetailsResponse("UpdateAuxDetails Triggered");

        } catch (Exception ex) {
            log.error("UpdateAuxDetails() : AgentID : " + request.getAgentID(), ex);
            return new UpdateAuxDetailsResponse("Bad Request");
        }
    }
}