package com.example.mail.service;

import com.example.mail.dto.soap.checker.ObjMail;
import com.example.mail.model.MailSendStatus;
import com.example.mail.repository.ContactActionRepository;
import com.example.mail.repository.MailSendStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.List;
import java.util.Map;

@Service
public class MailSendStatusService {

    private final Logger logger = LoggerFactory.getLogger("MAIL_SERVICES_AVAAYA_LOGGER");
    private final MailSendStatusRepository mailSendStatusRepository;
    private final ContactActionRepository contactActionRepository;
    private final JdbcTemplate jdbcTemplate;

    @Value("${db.sp.save-sent-mail:USP_SaveMailSendDetails}")
    private String spSaveMailSend;

    @Value("${db.sp.save-sent-attachment:USP_SaveMailAttachmentDetails}")
    private String spSaveMailAttachment;

    public MailSendStatusService(MailSendStatusRepository mailSendStatusRepository,
                                 ContactActionRepository contactActionRepository,
                                 JdbcTemplate jdbcTemplate) {
        this.mailSendStatusRepository = mailSendStatusRepository;
        this.contactActionRepository = contactActionRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Saves sent mail details matching legacy DemsMailSend SaveSentmailDetails behavior.
     * Attempts to invoke USP_SaveMailSendDetails; falls back to the MailSendStatus repository
     * if the stored procedure has not been created yet in SQL Server.
     */
    @Transactional
    public MailSendStatus saveSentMailDetails(MailSendStatus mailSendStatus, ObjMail objMail) {
        // 1. Resolve ActionID from contact_actions table if not supplied
        Long actionId = mailSendStatus.getActionId();
        if (actionId == null || actionId <= 0) {
            try {
                Long contactIdLong = objMail.getContactId() != null ? Long.parseLong(objMail.getContactId().trim()) : null;
                Long agentIdLong = objMail.getAgentId() != null ? Long.parseLong(objMail.getAgentId().trim()) : null;

                if (contactIdLong != null && agentIdLong != null) {
                    actionId = contactActionRepository.findMaxActionIdByContactIdAndAgentId(contactIdLong, agentIdLong);
                }
                if (actionId == null && contactIdLong != null) {
                    actionId = contactActionRepository.findMaxActionIdByContactId(contactIdLong);
                }
                if (actionId != null && actionId > 0) {
                    logger.info("saveSentMailDetails() - Resolved ActionID {} from contact_actions for ContactID {}", actionId, objMail.getContactId());
                }
            } catch (Exception ex) {
                logger.debug("saveSentMailDetails() - Could not resolve ActionID from contact_actions: {}", ex.getMessage());
            }
        }
        if (actionId == null || actionId <= 0) {
            actionId = System.currentTimeMillis() % 10000000L + 1000000L;
        }
        mailSendStatus.setActionId(actionId);
        objMail.setActionId(actionId);

        // 2. Attempt execution via configured stored procedure
        Long mailId = null;
        try {
            SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                    .withProcedureName(spSaveMailSend)
                    .declareParameters(
                            new SqlParameter("FromEmail", Types.VARCHAR),
                            new SqlParameter("ToEmail", Types.VARCHAR),
                            new SqlParameter("CCEmail", Types.VARCHAR),
                            new SqlParameter("BCCEmail", Types.VARCHAR),
                            new SqlParameter("Subject", Types.VARCHAR),
                            new SqlParameter("BodyContent", Types.VARCHAR),
                            new SqlParameter("IsSent", Types.BOOLEAN),
                            new SqlParameter("Remarks", Types.VARCHAR),
                            new SqlParameter("ContactID", Types.VARCHAR),
                            new SqlParameter("AgentID", Types.VARCHAR),
                            new SqlParameter("ClosedReason", Types.VARCHAR),
                            new SqlParameter("Comment", Types.VARCHAR),
                            new SqlParameter("ActionID", Types.BIGINT),
                            new SqlParameter("AnswerDT", Types.TIMESTAMP),
                            new SqlOutParameter("ERRORNO", Types.INTEGER),
                            new SqlOutParameter("ERRORMSG", Types.VARCHAR),
                            new SqlOutParameter("MailID", Types.BIGINT)
                    );

            Timestamp answerDt = mailSendStatus.getAnsweredDateTime() != null
                    ? Timestamp.valueOf(mailSendStatus.getAnsweredDateTime())
                    : Timestamp.valueOf(LocalDateTime.now());

            MapSqlParameterSource inParams = new MapSqlParameterSource()
                    .addValue("FromEmail", mailSendStatus.getFromEmail())
                    .addValue("ToEmail", mailSendStatus.getToEmail())
                    .addValue("CCEmail", mailSendStatus.getCcEmail() != null ? mailSendStatus.getCcEmail() : "")
                    .addValue("BCCEmail", mailSendStatus.getBccEmail() != null ? mailSendStatus.getBccEmail() : "")
                    .addValue("Subject", mailSendStatus.getSubject() != null ? mailSendStatus.getSubject() : "")
                    .addValue("BodyContent", mailSendStatus.getBodyContent() != null ? mailSendStatus.getBodyContent() : "")
                    .addValue("IsSent", Boolean.TRUE.equals(mailSendStatus.getIsSent()))
                    .addValue("Remarks", mailSendStatus.getRemarks())
                    .addValue("ContactID", mailSendStatus.getContactId())
                    .addValue("AgentID", mailSendStatus.getAgentId())
                    .addValue("ClosedReason", mailSendStatus.getClosedReason())
                    .addValue("Comment", mailSendStatus.getComment())
                    .addValue("ActionID", mailSendStatus.getActionId())
                    .addValue("AnswerDT", answerDt);

            Map<String, Object> output = jdbcCall.execute(inParams);
            Number mailIdVal = (Number) output.get("MailID");
            if (mailIdVal != null && mailIdVal.longValue() > 0) {
                mailId = mailIdVal.longValue();
                mailSendStatus.setMailId(mailId);
                objMail.setMailId(mailId);
                logger.info("saveSentMailDetails() - {} executed successfully with MailID {}", spSaveMailSend, mailId);
            }
        } catch (Exception ex) {
            logger.warn("saveSentMailDetails() - {} not executed (procedure will be created manually): {}", spSaveMailSend, ex.getMessage());
        }

        // 3. Fallback to JPA repository if procedure is not yet created or returned no MailID
        if (mailId == null) {
            try {
                mailSendStatus = mailSendStatusRepository.save(mailSendStatus);
                mailId = mailSendStatus.getMailId();
                objMail.setMailId(mailId);
                logger.info("saveSentMailDetails() - Saved MailSendStatus to table via repository with MailID {}", mailId);
            } catch (Exception ex) {
                logger.error("saveSentMailDetails() - Failed to save MailSendStatus to database: {}", ex.getMessage(), ex);
            }
        }

        // 4. Save attachments via USP_SaveMailAttachmentDetails matching SaveSentmailAttachmentDetails
        if (Boolean.TRUE.equals(mailSendStatus.getIsSent()) && mailId != null && mailId > 0
                && objMail.getAttachmentFiles() != null && !objMail.getAttachmentFiles().isEmpty()) {
            saveMailAttachmentDetails(mailId, objMail.getAttachmentFiles());
        }

        return mailSendStatus;
    }

    /**
     * Persists sent email attachment details using stored procedure USP_SaveMailAttachmentDetails,
     * matching the legacy DemsMailSend SaveSentmailAttachmentDetails implementation.
     */
    public void saveMailAttachmentDetails(Long mailId, List<String> attachmentFiles) {
        if (attachmentFiles == null || attachmentFiles.isEmpty()) {
            return;
        }
        logger.info("saveMailAttachmentDetails() - Processing {} sent attachment(s) for MailID {}", attachmentFiles.size(), mailId);
        for (String pathName : attachmentFiles) {
            if (pathName == null || pathName.trim().isEmpty()) {
                continue;
            }
            try {
                SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                        .withProcedureName(spSaveMailAttachment)
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
                logger.info("saveMailAttachmentDetails() - Successfully executed {} for MailID: {}, Path: {}, AttachmentId: {}",
                        spSaveMailAttachment, mailId, pathName, out.get("AttachmentId"));
            } catch (Exception ex) {
                logger.warn("saveMailAttachmentDetails() - {} call for MailID {} and Path '{}' (procedure will be created manually): {}",
                        spSaveMailAttachment, mailId, pathName, ex.getMessage());
            }
        }
    }
}

