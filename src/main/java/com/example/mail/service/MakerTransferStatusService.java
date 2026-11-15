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

import com.example.mail.model.Attachment;
import com.example.mail.model.ContactAction;
import com.example.mail.model.Email;
import com.example.mail.repository.AttachmentRepository;
import com.example.mail.repository.EmailRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

@Service
public class MakerTransferStatusService {

    private static final Logger logger = LoggerFactory.getLogger(MakerTransferStatusService.class);
    private final MakerTransferStatusRepository makerTransferStatusRepository;
    private final ContactActionRepository contactActionRepository;
    private final JdbcTemplate jdbcTemplate;

    @Autowired(required = false)
    private EmailRepository emailRepository;

    @Autowired(required = false)
    private AttachmentRepository attachmentRepository;

    @Autowired(required = false)
    private com.example.mail.repository.UserMasterRepo userMasterRepo;

    @Value("${upload.path:C:/Uploads/}")
    private String baseUploadPath;

    @Value("${db.sp.save-maker-transfer:USP_SaveMakerTransferDetails}")
    private String spSaveMakerTransfer;

    @Value("${db.sp.save-maker-attachment:USP_SaveMakerAttachmentDetails}")
    private String spSaveMakerAttachment;

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

        logger.debug("saveMakerTransferDetails() - Calling procedure {} for ContactID {}", spSaveMakerTransfer, transferStatus.getContactId());
        Map<String, Object> output = null;
        try {
            SimpleJdbcCall jdbcCall = new SimpleJdbcCall(jdbcTemplate)
                    .withProcedureName(spSaveMakerTransfer)
                    .declareParameters(
                            new SqlParameter("FromEmail", Types.VARCHAR),
                            new SqlParameter("ToEmail", Types.VARCHAR),
                            new SqlParameter("Subject", Types.VARCHAR),
                            new SqlParameter("BodyContent", Types.VARCHAR),
                            new SqlParameter("ContactID", Types.VARCHAR),
                            new SqlParameter("AgentID", Types.VARCHAR),
                            new SqlParameter("ClosedReason", Types.VARCHAR),
                            new SqlParameter("Comment", Types.VARCHAR),
                            new SqlParameter("ActionID", Types.BIGINT),
                            new SqlOutParameter("ERRORNO", Types.INTEGER),
                            new SqlOutParameter("ERRORMSG", Types.VARCHAR),
                            new SqlOutParameter("MailID", Types.BIGINT),
                            new SqlParameter("AnswerDT", Types.TIMESTAMP)
                    );
            MapSqlParameterSource inParams = new MapSqlParameterSource()
                    .addValue("FromEmail", transferStatus.getFromEmail())
                    .addValue("ToEmail", transferStatus.getToEmail())
                    .addValue("Subject", transferStatus.getSubject())
                    .addValue("BodyContent", transferStatus.getBodyContent())
                    .addValue("ContactID", transferStatus.getContactId())
                    .addValue("AgentID", agentId)
                    .addValue("ClosedReason", transferStatus.getClosedReason())
                    .addValue("Comment", transferStatus.getComment())
                    .addValue("ActionID", transferStatus.getActionId())
                    .addValue("AnswerDT", Timestamp.valueOf(transferStatus.getAnsweredDateTime()));
            output = jdbcCall.execute(inParams);
        } catch (Exception ex) {
            logger.warn("saveMakerTransferDetails() - SimpleJdbcCall for {} failed (falling back to repository): {}", spSaveMakerTransfer, ex.getMessage());
            output = makerTransferStatusRepository.executeSaveMakerTransferDetails(
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
        }

        Number errorNumber = numberValue(output, "ERRORNO");
        String errorMessage = stringValue(output, "ERRORMSG");
        if (errorNumber != null && errorNumber.intValue() != 0) {
            logger.error("saveMakerTransferDetails() - {} failed for ContactID {}. ERRORNO={}, ERRORMSG={}",
                spSaveMakerTransfer, transferStatus.getContactId(), errorNumber, errorMessage);
            throw new IllegalStateException(spSaveMakerTransfer + " failed ("
                    + errorNumber + "): " + errorMessage);
        }

        Number mailIdValue = numberValue(output, "MailID");
        if (mailIdValue == null || mailIdValue.longValue() <= 0) {
            logger.error("saveMakerTransferDetails() - {} returned no valid MailID for ContactID {}. Output keys={}",
                spSaveMakerTransfer, transferStatus.getContactId(), output.keySet());
            throw new IllegalStateException(spSaveMakerTransfer + " did not return a MailID");
        }
        Long mailId = mailIdValue.longValue();
        transferStatus.setMailId(mailId);
        mail.setMailId(mailId);
        logger.info("saveMakerTransferDetails() - Saved maker transfer MailID {} for ContactID {}", mailId, transferStatus.getContactId());

        if (mailId > 0 && mail.getAttachmentFiles() != null && !mail.getAttachmentFiles().isEmpty()) {
            saveMakerAttachmentDetails(mailId, mail.getAttachmentFiles());
        }

        recordMakerTransferAction(mail, actionId);

        return transferStatus;
    }

    private void recordMakerTransferAction(ObjMail mail, Long actionId) {
        if (contactActionRepository == null || mail == null) {
            return;
        }
        try {
            Long contactIdLong = mail.getContactId() != null ? Long.parseLong(mail.getContactId().trim()) : null;
            if (contactIdLong == null) return;

            ContactAction makerAction = new ContactAction();
            makerAction.setActionId(actionId != null && actionId > 0 ? actionId : System.currentTimeMillis() % 10000000L + 1000000L);
            makerAction.setContactId(contactIdLong);
            makerAction.setSubject(mail.getSubject() != null ? mail.getSubject() : "Maker Transfer");
            makerAction.setTextContent(mail.getBodyContent());
            makerAction.setTextHtml(mail.getBodyContent());
            makerAction.setSource("Maker_Transfer");
            makerAction.setComment(mail.getComment());
            makerAction.setClosedReasonName(mail.getClosedReason());
            makerAction.setCreationTime(System.currentTimeMillis());
            makerAction.setMailFrom(mail.getFromEmail());
            makerAction.setMailTo(mail.getToEmail());
            makerAction.setMailCc(mail.getCcEmail());
            makerAction.setActionType("Email");
            makerAction.setCallbackStatus("Unspecified");
            makerAction.setTimeAllocated(20);
            makerAction.setOutboundTalkTime(0);
            if (mail.getAgentId() != null) {
                try {
                    Long agentIdVal = Long.parseLong(mail.getAgentId().trim());
                    makerAction.setAgentId(agentIdVal);
                    if (userMasterRepo != null) {
                        com.example.mail.model.UserMaster u = userMasterRepo.findByAgentId(agentIdVal);
                        if (u != null) {
                            makerAction.setAgentFirstName(u.getFirstName());
                            makerAction.setAgentLastName(u.getLastName());
                        }
                    }
                } catch (Exception ignored) {}
            }
            ContactAction savedAction = contactActionRepository.save(makerAction);

            if (attachmentRepository != null && emailRepository != null && mail.getAttachmentFiles() != null && !mail.getAttachmentFiles().isEmpty()) {
                List<Email> emails = emailRepository.findByContactId(contactIdLong);
                Email targetEmail = (!emails.isEmpty()) ? emails.get(0) : null;
                if (targetEmail != null) {
                    for (String pathName : mail.getAttachmentFiles()) {
                        if (pathName == null || pathName.trim().isEmpty()) continue;
                        String clean = pathName.trim().replace("\\", "/");
                        int idx = clean.lastIndexOf("/");
                        String fileName = (idx >= 0 && idx < clean.length() - 1) ? clean.substring(idx + 1) : clean;

                        Attachment att = new Attachment();
                        att.setEmail(targetEmail);
                        att.setContactAction(savedAction);
                        att.setFileName(fileName);
                        att.setDisplayName(fileName);
                        att.setInternalPath(pathName.trim());

                        try {
                            java.io.File diskFile = new java.io.File(pathName.trim());
                            if (!diskFile.exists() || !diskFile.isFile()) {
                                String baseDir = baseUploadPath != null ? baseUploadPath.trim() : "C:/Uploads/";
                                String rel = clean;
                                while (rel.startsWith("/")) rel = rel.substring(1);
                                diskFile = new java.io.File(baseDir + (baseDir.endsWith("/") || baseDir.endsWith("\\") ? "" : "/") + rel);
                            }
                            if (diskFile.exists() && diskFile.isFile()) {
                                att.setFileData(java.nio.file.Files.readAllBytes(diskFile.toPath()));
                            }
                        } catch (Exception ignored) {}

                        attachmentRepository.save(att);
                    }
                }
            }
            logger.info("recordMakerTransferAction() - Recorded Maker_Transfer ContactAction {} for ContactID {}", savedAction.getActionId(), contactIdLong);
        } catch (Exception ex) {
            logger.warn("recordMakerTransferAction() - Could not record Maker_Transfer ContactAction: {}", ex.getMessage());
        }
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
                        .withProcedureName(spSaveMakerAttachment)
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
                logger.info("saveMakerAttachmentDetails() - Successfully executed {} for MailID: {}, Path: {}, AttachmentId: {}",
                        spSaveMakerAttachment, mailId, pathName, out.get("AttachmentId"));
            } catch (Exception ex) {
                logger.warn("saveMakerAttachmentDetails() - {} call for MailID {} and Path '{}' (procedure may not be created yet): {}",
                        spSaveMakerAttachment, mailId, pathName, ex.getMessage());
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
