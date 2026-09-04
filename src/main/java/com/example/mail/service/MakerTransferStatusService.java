package com.example.mail.service;

import com.example.mail.dto.sendMail.ObjMail;
import com.example.mail.model.MakerTransferStatus;
import com.example.mail.repository.MakerTransferStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.sql.Timestamp;
import java.util.Map;

@Service
public class MakerTransferStatusService {

    private static final Logger logger = LoggerFactory.getLogger(MakerTransferStatusService.class);
    private final MakerTransferStatusRepository makerTransferStatusRepository;

    public MakerTransferStatusService(MakerTransferStatusRepository makerTransferStatusRepository) {
        this.makerTransferStatusRepository = makerTransferStatusRepository;
    }

    /**
     * Persists the maker-to-checker handoff details.  This deliberately does
     * not perform the legacy host-name or Authorization-header validation.
     */
    @Transactional
    public MakerTransferStatus saveMakerTransferDetails(ObjMail mail) {
        MakerTransferStatus transferStatus = new MakerTransferStatus();
        transferStatus.setFromEmail(mail.getFromEmail());
        transferStatus.setToEmail(mail.getToEmail());
        transferStatus.setSubject(mail.getSubject());
        transferStatus.setBodyContent(mail.getBodyContent());
        transferStatus.setContactId(mail.getContactId());
        transferStatus.setAgentId(mail.getAgentId());
        transferStatus.setClosedReason(mail.getClosedReason());
        transferStatus.setComment(mail.getComment());
        transferStatus.setActionId(mail.getActionId());
        transferStatus.setCreatedDate(LocalDateTime.now());
        transferStatus.setAnsweredDateTime(parseAnsweredDateTime(mail.getAnsweredDateTime()));

        Long agentId = parseAgentId(mail.getAgentId());

        Map<String, Object> output = makerTransferStatusRepository.executeSaveMakerTransferDetails(
                transferStatus.getFromEmail(),
                transferStatus.getToEmail(),
                transferStatus.getSubject(),
                transferStatus.getBodyContent(),
                transferStatus.getContactId(),
                agentId,
                transferStatus.getClosedReason(),
                transferStatus.getComment(),
                transferStatus.getActionId(),
                Timestamp.valueOf(transferStatus.getAnsweredDateTime()));

        Number errorNumber = numberValue(output, "ERRORNO");
        String errorMessage = stringValue(output, "ERRORMSG");
        if (errorNumber != null && errorNumber.intValue() != 0) {
            throw new IllegalStateException("USP_SaveMakerTransferDetails failed ("
                    + errorNumber + "): " + errorMessage);
        }

        Number mailIdValue = numberValue(output, "MailID");
        Long mailId = mailIdValue == null ? null : mailIdValue.longValue();
        transferStatus.setMailId(mailId);
        mail.setMailId(mailId);
        logger.info("Saved maker transfer MailID {} for ContactID {}", mailId, transferStatus.getContactId());
        return transferStatus;
    }

    private String stringValue(Map<String, Object> values, String name) {
        Object value = values.get(name);
        return value == null ? null : value.toString();
    }

    private Long parseAgentId(String agentId) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("AgentID is required and must be numeric");
        }
        try {
            return Long.valueOf(agentId.trim());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("AgentID must be numeric: " + agentId);
        }
    }

    private Number numberValue(Map<String, Object> values, String name) {
        Object value = values.get(name);
        return value instanceof Number ? (Number) value : null;
    }

    private LocalDateTime parseAnsweredDateTime(String answeredDateTime) {
        if (answeredDateTime == null || answeredDateTime.trim().isEmpty()) {
            return LocalDateTime.now();
        }
        try {
            return LocalDateTime.parse(answeredDateTime, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception exception) {
            logger.warn("Invalid AnsweredDateTime '{}'; storing the current time instead", answeredDateTime);
            return LocalDateTime.now();
        }
    }
}
