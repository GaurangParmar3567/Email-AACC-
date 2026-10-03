package com.example.mail.service;

import com.example.mail.dto.soap.checker.ObjMail;
import com.example.mail.model.MakerTransferStatus;
import com.example.mail.repository.ContactActionRepository;
import com.example.mail.repository.MakerTransferStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.SqlOutParameter;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;
import java.sql.Types;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

@Service
public class MakerTransferStatusService {

    private static final Logger logger = LoggerFactory.getLogger(MakerTransferStatusService.class);
    private final MakerTransferStatusRepository makerTransferStatusRepository;
    private final ContactActionRepository contactActionRepository;
    private final JdbcTemplate jdbcTemplate;

    public MakerTransferStatusService(MakerTransferStatusRepository makerTransferStatusRepository,
                                      ContactActionRepository contactActionRepository,
                                      JdbcTemplate jdbcTemplate) {
        this.makerTransferStatusRepository = makerTransferStatusRepository;
        this.contactActionRepository = contactActionRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Persists the maker-to-checker handoff details matching DemsMailSend behavior using project tables.
     */
    @Transactional
    public MakerTransferStatus saveMakerTransferDetails(ObjMail mail) {
        logger.info("saveMakerTransferDetails() - Preparing maker transfer save for ContactID {} and AgentID {}",
            mail.getContactId(), mail.getAgentId());
        MakerTransferStatus transferStatus = new MakerTransferStatus();
        transferStatus.setFromEmail(mail.getFromEmail());
        transferStatus.setToEmail(mail.getToEmail());
        transferStatus.setSubject(mail.getSubject());
        transferStatus.setBodyContent(mail.getBodyContent());
        transferStatus.setContactId(mail.getContactId());
        transferStatus.setAgentId(mail.getAgentId());
        transferStatus.setClosedReason(mail.getClosedReason());
        transferStatus.setComment(mail.getComment());

        Long actionId = mail.getActionId();
        if (actionId == null || actionId <= 0) {
            try {
                Long contactIdLong = mail.getContactId() != null ? Long.parseLong(mail.getContactId().trim()) : null;
                Long agentIdLong = mail.getAgentId() != null ? Long.parseLong(mail.getAgentId().trim()) : null;

                if (contactIdLong != null && agentIdLong != null) {
                    actionId = contactActionRepository.findMaxActionIdByContactIdAndAgentId(contactIdLong, agentIdLong);
                }
                if (actionId == null && contactIdLong != null) {
                    actionId = contactActionRepository.findMaxActionIdByContactId(contactIdLong);
                }
                if (actionId != null && actionId > 0) {
                    logger.info("saveMakerTransferDetails() - Resolved ActionID {} from contact_actions for ContactID {}", actionId, mail.getContactId());
                }
            } catch (Exception ex) {
                logger.debug("saveMakerTransferDetails() - Could not resolve ActionID from contact_actions: {}", ex.getMessage());
            }
        }
        if (actionId == null || actionId <= 0) {
            actionId = System.currentTimeMillis() % 10000000L + 1000000L;
        }
        transferStatus.setActionId(actionId);
        transferStatus.setCreatedDate(LocalDateTime.now());
        transferStatus.setAnsweredDateTime(parseAnsweredDateTime(mail.getAnsweredDateTime()));

        String agentId = requireAgentId(mail.getAgentId());

        logger.debug("saveMakerTransferDetails() - Calling USP_SaveMakerTransferDetails for ContactID {}", transferStatus.getContactId());
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
            logger.error("saveMakerTransferDetails() - USP_SaveMakerTransferDetails failed for ContactID {}. ERRORNO={}, ERRORMSG={}",
                transferStatus.getContactId(), errorNumber, errorMessage);
            throw new IllegalStateException("USP_SaveMakerTransferDetails failed ("
                    + errorNumber + "): " + errorMessage);
        }

        Number mailIdValue = numberValue(output, "MailID");
        if (mailIdValue == null || mailIdValue.longValue() <= 0) {
            logger.error("saveMakerTransferDetails() - USP_SaveMakerTransferDetails returned no valid MailID for ContactID {}. Output keys={}",
                transferStatus.getContactId(), output.keySet());
            throw new IllegalStateException("USP_SaveMakerTransferDetails did not return a MailID");
        }
        Long mailId = mailIdValue.longValue();
        transferStatus.setMailId(mailId);
        mail.setMailId(mailId);
        logger.info("saveMakerTransferDetails() - Saved maker transfer MailID {} for ContactID {}", mailId, transferStatus.getContactId());

        if (mailId > 0 && mail.getAttachmentFiles() != null && !mail.getAttachmentFiles().isEmpty()) {
            saveMakerAttachmentDetails(mailId, mail.getAttachmentFiles());
        }

        return transferStatus;
    }

    /**
     * Persists maker attachment details using stored procedure USP_SaveMakerAttachmentDetails,
     * matching the legacy DemsMailSend SaveMakerAttachmentDetails implementation.
     */
    public void saveMakerAttachmentDetails(Long mailId, List<String> attachmentFiles) {
        if (attachmentFiles == null || attachmentFiles.isEmpty()) {
            return;
        }
        logger.info("saveMakerAttachmentDetails() - Processing {} maker attachment(s) for MailID {}", attachmentFiles.size(), mailId);
        for (String pathName : attachmentFiles) {
            if (pathName == null || pathName.trim().isEmpty()) {
                continue;
            }
            try {
                SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                        .withProcedureName("USP_SaveMakerAttachmentDetails")
                        .declareParameters(
                                new SqlParameter("MailID", Types.BIGINT),
                                new SqlParameter("PathName", Types.VARCHAR),
                                new SqlOutParameter("ERRORNO", Types.INTEGER),
                                new SqlOutParameter("ERRORMSG", Types.VARCHAR),
                                new SqlOutParameter("AttachmentId", Types.BIGINT)
                        );

                MapSqlParameterSource inParams = new MapSqlParameterSource()
                        .addValue("MailID", mailId)
                        .addValue("PathName", pathName.trim());

                Map<String, Object> out = jdbcCall.execute(inParams);
                logger.info("saveMakerAttachmentDetails() - Successfully executed USP_SaveMakerAttachmentDetails for MailID: {}, Path: {}, AttachmentId: {}",
                        mailId, pathName, out.get("AttachmentId"));
            } catch (Exception ex) {
                logger.warn("saveMakerAttachmentDetails() - USP_SaveMakerAttachmentDetails call for MailID {} and Path '{}' (procedure may not be created yet): {}",
                        mailId, pathName, ex.getMessage());
            }
        }
    }

    private String stringValue(Map<String, Object> values, String name) {
        Object value = values.get(name);
        return value == null ? null : value.toString();
    }

    private String requireAgentId(String agentId) {
        if (agentId == null || agentId.trim().isEmpty()) {
            throw new IllegalArgumentException("AgentID is required");
        }
        return agentId.trim();
    }

    private Number numberValue(Map<String, Object> values, String name) {
        Object value = values.get(name);
        return value instanceof Number ? (Number) value : null;
    }

    private LocalDateTime parseAnsweredDateTime(String answeredDateTime) {
        if (answeredDateTime == null || answeredDateTime.trim().isEmpty()) {
            return LocalDateTime.now();
        }
        String val = answeredDateTime.trim();
        if (val.startsWith("<![CDATA[") && val.endsWith("]]>")) {
            val = val.substring(9, val.length() - 3).trim();
        }
        try {
            return LocalDateTime.parse(val, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception exception) {
            try {
                return LocalDateTime.parse(val, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
            } catch (Exception ex) {
                try {
                    return LocalDateTime.parse(val, DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss"));
                } catch (Exception ex2) {
                    logger.warn("parseDateTime() - Invalid AnsweredDateTime '{}'; storing the current time instead", answeredDateTime);
                    return LocalDateTime.now();
                }
            }
        }
    }
}
