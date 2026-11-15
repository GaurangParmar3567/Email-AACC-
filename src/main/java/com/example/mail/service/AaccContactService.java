package com.example.mail.service;

import com.example.mail.dto.soap.aacc.*;
import com.example.mail.dto.soap.common.AgentDTO;
import com.example.mail.dto.soap.common.MillisecondsDTO;
import com.example.mail.dto.soap.common.SkillsetDTO;
import com.example.mail.exception.ContactNotFoundException;
import com.example.mail.model.Attachment;
import com.example.mail.model.ContactAction;
import com.example.mail.model.Email;
import com.example.mail.model.SkillMaster;
import com.example.mail.repository.ContactActionRepository;
import com.example.mail.repository.AttachmentRepository;
import com.example.mail.repository.ClosedReasonRepository;
import com.example.mail.repository.EmailRepository;
import com.example.mail.repository.SkillMasterRepo;
import com.example.mail.util.IdGenerator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import javax.servlet.http.HttpServletRequest;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.CallableStatement;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AaccContactService {

    private static final Logger logger = LoggerFactory.getLogger("MAIL_SERVICES_AVAAYA_LOGGER");

    @Value("${upload.path:C:/Uploads/}")
    private String baseUploadPath;

    @Value("${app.domain:}")
    private String appDomain;

    @Value("${server.port:8081}")
    private String serverPort;

    @Value("${server.address:}")
    private String serverAddress;

    @Value("${db.sp.get-all-skillsets:USP_GetAllSkillsets}")
    private String spGetAllSkillsets;

    private final EmailRepository emailRepository;
    private final ContactActionRepository contactActionRepository;
    private final ClosedReasonRepository closedReasonRepository;
    private final SkillMasterRepo skillMasterRepository;
    private final JdbcTemplate jdbcTemplate;
    private final AttachmentRepository attachmentRepository;
    @Autowired(required = false)
    private com.example.mail.repository.UserMasterRepo userMasterRepo;

    public AaccContactService(EmailRepository emailRepository,
                              ContactActionRepository contactActionRepository,
                              ClosedReasonRepository closedReasonRepository,
                              SkillMasterRepo skillMasterRepository,
                              JdbcTemplate jdbcTemplate,
                              AttachmentRepository attachmentRepository) {
        this.emailRepository = emailRepository;
        this.contactActionRepository = contactActionRepository;
        this.closedReasonRepository = closedReasonRepository;
        this.skillMasterRepository = skillMasterRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.attachmentRepository = attachmentRepository;
    }

    @Transactional(readOnly = true)
    public ReadContactResult getContactDetails(Long contactId) {
        logger.info("getContactDetails() - Fetching contact details for ContactID: {}", contactId);
        List<Email> emailThread = emailRepository.findByContactId(contactId);
        if (emailThread == null || emailThread.isEmpty()) {
            logger.warn("getContactDetails() - Contact not found for ContactID: {}", contactId);
            throw new ContactNotFoundException(String.valueOf(contactId));
        }

        Email rootEmail = emailThread.stream()
                .filter(email -> email.getParentEmail() == null)
                .findFirst()
                .orElse(emailThread.get(0));
        Email agentEmail = emailThread.stream()
                .filter(e -> e.getAgentId() != null)
                .findFirst()
                .orElse(rootEmail);

        ReadContactResult result = new ReadContactResult();
        result.setId(contactId);
        result.setCustomerID(rootEmail.getCustomerId());
        result.setOriginalSubject(rootEmail.getOriginalSubject());
        result.setSource(rootEmail.getSource());
        result.setStatus(rootEmail.getStatus());
        result.setPriority(rootEmail.getPriority());
        result.setTimezone(rootEmail.getTimezone());
        result.setOpenDuration(rootEmail.getOpenDuration());
        result.setMailTo(rootEmail.getMailTo());
        result.setMailFrom(rootEmail.getMailFrom());
        result.setMailCc(rootEmail.getMailCc());
        result.setContactType(rootEmail.getContactType());

        if (rootEmail.getSkillId() != null) {
            SkillsetDTO skillset = new SkillsetDTO();
            skillset.setId(rootEmail.getSkillId());
            skillset.setName(rootEmail.getSkillsetName());
            result.setSkillset(skillset);
        }
        if (rootEmail.getArrivalTime() != null) {
            MillisecondsDTO arrivalTime = new MillisecondsDTO();
            arrivalTime.setMilliseconds(rootEmail.getArrivalTime());
            result.setArrivalTime(arrivalTime);
        }
        if (rootEmail.getOpenTime() != null) {
            MillisecondsDTO openTime = new MillisecondsDTO();
            openTime.setMilliseconds(rootEmail.getOpenTime());
            result.setOpenTime(openTime);
        }
        if (agentEmail.getAgentId() != null) {
            AgentDTO agent = new AgentDTO();
            agent.setId(agentEmail.getAgentId());
            agent.setFirstName(agentEmail.getAgentFirstName());
            agent.setLastName(agentEmail.getAgentLastName());
            result.setAgent(agent);
        }

        // Retrieve contact-level attachments from repository (covers email attachments even if action linkage is missing)
        List<Attachment> contactAttachments = new ArrayList<>();
        if (attachmentRepository != null) {
            List<Attachment> repoAttachments = attachmentRepository.findByEmailContactId(contactId);
            if (repoAttachments != null && !repoAttachments.isEmpty()) {
                contactAttachments.addAll(repoAttachments);
            }
        }
        for (Email email : emailThread) {
            if (email != null && email.getAttachments() != null) {
                for (Attachment att : email.getAttachments()) {
                    if (att != null && att.getId() != null) {
                        boolean exists = contactAttachments.stream().anyMatch(a -> att.getId().equals(a.getId()));
                        if (!exists) {
                            contactAttachments.add(att);
                        }
                    }
                }
            }
        }

        // Also scan physical disk folder (C:/Uploads/Inbound/<contactId>/) to include any files placed directly on disk
        syncAttachmentsFromDisk(contactAttachments, rootEmail, contactId);

        // Ensure all contact attachments exist on disk for instant download/preview
        for (Attachment att : contactAttachments) {
            ensureAttachmentOnDisk(att, contactId);
        }

        ActionListDTO actionList = new ActionListDTO();
        List<ContactAction> actions = contactActionRepository.findByContactId(contactId);
        if (actions != null && !actions.isEmpty()) {
            actions.sort(Comparator.comparing((ContactAction a) -> a.getCreationTime() != null ? a.getCreationTime() : 0L)
                    .thenComparing(a -> a.getId() != null ? a.getId() : 0L));
            for (int i = 0; i < actions.size(); i++) {
                ContactAction action = actions.get(i);
                if (action == null) {
                    continue;
                }
                AWActionDTO actionDto = new AWActionDTO();
                actionDto.setId(action.getActionId());
                actionDto.setContactID(contactId);
                actionDto.setSubject(action.getSubject());
                actionDto.setText(action.getTextContent());
                actionDto.setTextHTML(action.getTextHtml());
                actionDto.setCallbackStatus(action.getCallbackStatus());
                actionDto.setSource(action.getSource());
                actionDto.setComment(action.getComment());
                actionDto.setMailFrom(action.getMailFrom());
                actionDto.setMailTo(action.getMailTo());
                actionDto.setMailCC(action.getMailCc());
                actionDto.setTimeAllocated(action.getTimeAllocated());
                actionDto.setOutboundTalkTime(0);
                actionDto.setOutboundDispositionCode(action.getOutboundDispositionCode());
                actionDto.setActionType(action.getActionType());

                if (action.getCreationTime() != null) {
                    MillisecondsDTO creationTime = new MillisecondsDTO();
                    creationTime.setMilliseconds(action.getCreationTime());
                    actionDto.setCreationTime(creationTime);
                }
                if (action.getAgentId() != null) {
                    AgentDTO agent = new AgentDTO();
                    agent.setId(action.getAgentId());
                    String fName = action.getAgentFirstName();
                    String lName = action.getAgentLastName();
                    if ((fName == null || fName.trim().isEmpty()) && userMasterRepo != null) {
                        try {
                            com.example.mail.model.UserMaster u = userMasterRepo.findByAgentId(action.getAgentId());
                            if (u != null) {
                                fName = u.getFirstName();
                                lName = u.getLastName();
                            }
                        } catch (Exception ignored) {}
                    }
                    agent.setFirstName(fName);
                    agent.setLastName(lName);
                    actionDto.setAgent(agent);
                }
                if (action.getClosedReasonName() != null || action.getClosedReasonNumericValue() != null) {
                    ClosedReasonDTO reason = new ClosedReasonDTO();
                    reason.setName(action.getClosedReasonName());
                    reason.setNumericValue(action.getClosedReasonNumericValue());
                    actionDto.setClosedReason(reason);
                }

                // Gather attachments for this action
                List<Attachment> candidateAttachments = new ArrayList<>();
                try {
                    if (action.getAttachments() != null && !action.getAttachments().isEmpty()) {
                        candidateAttachments.addAll(action.getAttachments());
                    }
                } catch (Exception ex) {
                    logger.warn("getContactDetails() - Could not lazy load attachments for action ID: {}", action.getId(), ex);
                }

                if (candidateAttachments.isEmpty() && action.getId() != null) {
                    List<Attachment> fromActionRepo = attachmentRepository.findByContactActionId(action.getId());
                    if (fromActionRepo != null && !fromActionRepo.isEmpty()) {
                        candidateAttachments.addAll(fromActionRepo);
                    }
                }

                // Ensure customer incoming action (or first action) includes all contact-level attachments (including files on disk)
                if (i == 0 || "EMail_from_Customer".equalsIgnoreCase(action.getSource())) {
                    for (Attachment ca : contactAttachments) {
                        boolean exists = candidateAttachments.stream()
                                .anyMatch(a -> (a.getId() != null && a.getId().equals(ca.getId()))
                                        || (a.getFileName() != null && a.getFileName().equalsIgnoreCase(ca.getFileName())));
                        if (!exists) {
                            candidateAttachments.add(ca);
                        }
                    }
                }

                if (!candidateAttachments.isEmpty()) {
                    Map<Long, Attachment> uniqueMap = new LinkedHashMap<>();
                    for (Attachment att : candidateAttachments) {
                        if (att != null && att.getId() != null) {
                            uniqueMap.put(att.getId(), att);
                        }
                    }
                    if (!uniqueMap.isEmpty()) {
                        AttachmentListDTO attachments = new AttachmentListDTO();
                        for (Attachment attachment : uniqueMap.values()) {
                            attachments.getAwAttachments().add(buildAttachmentDto(attachment, contactId));
                        }
                        actionDto.setAttachmentList(attachments);
                    }
                }
                actionList.getAwActions().add(actionDto);
            }
        } else {
            // Synthesize default AWActionDTO from rootEmail so actionList is never empty
            AWActionDTO defaultAction = new AWActionDTO();
            defaultAction.setId(IdGenerator.generateContactId());
            defaultAction.setContactID(contactId);
            defaultAction.setSubject(rootEmail.getOriginalSubject() != null ? rootEmail.getOriginalSubject() : rootEmail.getSubject());
            defaultAction.setText(rootEmail.getText());
            defaultAction.setTextHTML(rootEmail.getBodyHtml());
            defaultAction.setCallbackStatus("Unspecified");
            defaultAction.setSource("EMail_from_Customer");
            defaultAction.setMailFrom(rootEmail.getMailFrom() != null ? rootEmail.getMailFrom() : rootEmail.getSender());
            defaultAction.setMailTo(rootEmail.getMailTo() != null ? rootEmail.getMailTo() : rootEmail.getRecipient());
            defaultAction.setMailCC(rootEmail.getMailCc() != null ? rootEmail.getMailCc() : rootEmail.getCc());
            defaultAction.setTimeAllocated(20);
            defaultAction.setOutboundTalkTime(0);
            defaultAction.setActionType("Email");
            if (rootEmail.getArrivalTime() != null) {
                MillisecondsDTO creationTime = new MillisecondsDTO();
                creationTime.setMilliseconds(rootEmail.getArrivalTime());
                defaultAction.setCreationTime(creationTime);
            }
            if (!contactAttachments.isEmpty()) {
                AttachmentListDTO attachments = new AttachmentListDTO();
                for (Attachment attachment : contactAttachments) {
                    attachments.getAwAttachments().add(buildAttachmentDto(attachment, contactId));
                }
                defaultAction.setAttachmentList(attachments);
            }
            actionList.getAwActions().add(defaultAction);
        }
        result.setActionList(actionList);
        logger.info("getContactDetails() - Successfully retrieved contact details for ContactID: {}", contactId);
        return result;
    }

    public String getApplicationDomain() {
        if (appDomain != null && !appDomain.trim().isEmpty()) {
            String domain = appDomain.trim();
            if (domain.endsWith("/")) {
                domain = domain.substring(0, domain.length() - 1);
            }
            return domain;
        }

        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null && attrs.getRequest() != null) {
                HttpServletRequest req = attrs.getRequest();
                String scheme = req.getScheme();
                String serverName = req.getServerName();
                int port = req.getServerPort();
                boolean isDefaultPort = ("http".equalsIgnoreCase(scheme) && port == 80)
                        || ("https".equalsIgnoreCase(scheme) && port == 443);
                return isDefaultPort ? (scheme + "://" + serverName) : (scheme + "://" + serverName + ":" + port);
            }
        } catch (Exception ignored) {
        }

        String host = (serverAddress != null && !serverAddress.trim().isEmpty()) ? serverAddress.trim() : "localhost";
        String port = (serverPort != null && !serverPort.trim().isEmpty()) ? serverPort.trim() : "8081";
        return "http://" + host + ":" + port;
    }

    public String resolveInternalAttachmentUrl(Attachment attachment, Long contactId) {
        if (attachment == null) {
            return "";
        }
        if (contactId == null && attachment.getEmail() != null) {
            contactId = attachment.getEmail().getContactId();
        }
        String internalPath = attachment.getInternalPath();
        String fileName = (attachment.getFileName() != null && !attachment.getFileName().trim().isEmpty())
                ? attachment.getFileName().trim()
                : "attachment";

        if (internalPath != null && (internalPath.startsWith("http://") || internalPath.startsWith("https://"))) {
            return internalPath.trim();
        }

        String relativePath;
        if (internalPath == null || internalPath.trim().isEmpty()) {
            relativePath = "/Inbound/" + (contactId != null ? contactId : 0) + "/" + fileName;
        } else {
            relativePath = internalPath.trim().replace("\\", "/");
            if (relativePath.contains("Inbound/")) {
                relativePath = "/" + relativePath.substring(relativePath.indexOf("Inbound/"));
            } else if (relativePath.contains("Outbound/")) {
                relativePath = "/" + relativePath.substring(relativePath.indexOf("Outbound/"));
            } else if (!relativePath.startsWith("/")) {
                relativePath = "/Inbound/" + (contactId != null ? contactId : 0) + "/" + relativePath;
            }
        }

        String domain = getApplicationDomain();
        if (domain.endsWith("/")) {
            domain = domain.substring(0, domain.length() - 1);
        }
        if (!relativePath.startsWith("/")) {
            relativePath = "/" + relativePath;
        }
        return domain + relativePath;
    }

    private AWAttachmentDTO buildAttachmentDto(Attachment attachment, Long contactId) {
        AWAttachmentDTO attachmentDto = new AWAttachmentDTO();
        attachmentDto.setId(attachment.getId());
        attachmentDto.setIdSpecified(true);
        String displayName = attachment.getDisplayName();
        if (displayName == null || displayName.trim().isEmpty()) {
            displayName = attachment.getFileName();
        }
        String internalPath = resolveInternalAttachmentUrl(attachment, contactId);
        attachmentDto.setDisplayFileName(displayName != null ? displayName : "");
        attachmentDto.setInternalFileName(internalPath);
        attachmentDto.setFileContents("");
        return attachmentDto;
    }

    private void ensureAttachmentOnDisk(Attachment attachment, Long contactId) {
        if (attachment == null || attachment.getFileData() == null || attachment.getFileData().length == 0) {
            return;
        }
        try {
            String fileName = attachment.getFileName();
            if (fileName == null || fileName.trim().isEmpty()) {
                fileName = "attachment_" + attachment.getId();
            }
            String baseDir = baseUploadPath != null ? baseUploadPath.trim() : "C:/Uploads/";
            if (!baseDir.endsWith("/") && !baseDir.endsWith("\\")) {
                baseDir += File.separator;
            }
            Path targetFile = Paths.get(baseDir, "Inbound", String.valueOf(contactId), fileName);
            if (targetFile.getParent() != null && !Files.exists(targetFile.getParent())) {
                Files.createDirectories(targetFile.getParent());
            }
            if (!Files.exists(targetFile)) {
                Files.write(targetFile, attachment.getFileData());
                logger.info("ensureAttachmentOnDisk() - Saved attachment file to disk: {}", targetFile.toAbsolutePath());
            }
        } catch (Exception ex) {
            logger.warn("ensureAttachmentOnDisk() - Could not write attachment file to disk: {}", ex.getMessage());
        }
    }

    private void syncAttachmentsFromDisk(List<Attachment> contactAttachments, Email rootEmail, Long contactId) {
        if (contactId == null) {
            return;
        }
        try {
            String baseDir = baseUploadPath != null ? baseUploadPath.trim() : "C:/Uploads/";
            if (!baseDir.endsWith("/") && !baseDir.endsWith("\\")) {
                baseDir += File.separator;
            }
            Path inboundFolder = Paths.get(baseDir, "Inbound", String.valueOf(contactId));
            if (!Files.exists(inboundFolder) || !Files.isDirectory(inboundFolder)) {
                return;
            }

            File folder = inboundFolder.toFile();
            File[] files = folder.listFiles();
            if (files == null || files.length == 0) {
                return;
            }

            for (File file : files) {
                if (file == null || !file.isFile() || file.length() == 0) {
                    continue;
                }
                String fileName = file.getName();
                boolean alreadyListed = contactAttachments.stream()
                        .anyMatch(a -> (a.getFileName() != null && fileName.equalsIgnoreCase(a.getFileName().trim()))
                                || (a.getDisplayName() != null && fileName.equalsIgnoreCase(a.getDisplayName().trim())));

                if (!alreadyListed) {
                    Attachment newAtt = new Attachment();
                    newAtt.setFileName(fileName);
                    newAtt.setDisplayName(fileName);
                    newAtt.setInternalPath("/Inbound/" + contactId + "/" + fileName);
                    newAtt.setEmail(rootEmail);

                    try {
                        newAtt.setFileData(Files.readAllBytes(file.toPath()));
                    } catch (Exception ignored) {}

                    if (attachmentRepository != null && rootEmail != null && rootEmail.getId() != null) {
                        try {
                            newAtt = attachmentRepository.save(newAtt);
                        } catch (Exception ex) {
                            logger.warn("syncAttachmentsFromDisk() - Could not save disk attachment {} to DB: {}", fileName, ex.getMessage());
                            newAtt.setId(Math.abs((long) fileName.hashCode()));
                        }
                    } else {
                        newAtt.setId(Math.abs((long) fileName.hashCode()));
                    }
                    contactAttachments.add(newAtt);
                    logger.info("syncAttachmentsFromDisk() - Discovered and added disk attachment: {} (ID: {}) for ContactID: {}",
                            fileName, newAtt.getId(), contactId);
                }
            }
        } catch (Exception ex) {
            logger.warn("syncAttachmentsFromDisk() - Error scanning disk folder for ContactID {}: {}", contactId, ex.getMessage());
        }
    }

    @Transactional
    public long transferToSkillset(long contactId, long skillsetId, String note) {
        logger.info("transferToSkillset() - Transferring ContactID: {} to SkillsetID: {} with note: {}", contactId, skillsetId, note);
        List<Email> emails = requireEmails(contactId);
        String skillsetName = skillMasterRepository.findById(skillsetId)
                .orElseThrow(() -> new IllegalArgumentException("Skillset not found for ID " + skillsetId))
                .getName();
        for (Email email : emails) {
            email.setSkillId(skillsetId);
            email.setSkillsetName(skillsetName);
            email.setAssigned(false);
            email.setAgentId(null);
            email.setAgentFirstName(null);
            email.setAgentLastName(null);
            email.setStatus("Open");
        }
        emailRepository.saveAll(emails);

        // Record a ContactAction for the transfer event
        Email rootEmail = !emails.isEmpty() ? emails.get(0) : null;
        String bodyText = (rootEmail != null && rootEmail.getText() != null && !rootEmail.getText().trim().isEmpty())
                ? rootEmail.getText()
                : (rootEmail != null ? rootEmail.getBody() : (note != null ? note : ("Transferred to " + skillsetName)));
        String bodyHtml = (rootEmail != null && rootEmail.getBodyHtml() != null && !rootEmail.getBodyHtml().trim().isEmpty())
                ? rootEmail.getBodyHtml()
                : (rootEmail != null && rootEmail.getBody() != null ? rootEmail.getBody() : bodyText);

        if ((bodyHtml == null || bodyHtml.trim().isEmpty()) && contactActionRepository != null) {
            List<ContactAction> existingActions = contactActionRepository.findByContactId(contactId);
            if (existingActions != null) {
                for (ContactAction prev : existingActions) {
                    if (prev.getTextHtml() != null && !prev.getTextHtml().trim().isEmpty()) {
                        bodyHtml = prev.getTextHtml();
                        bodyText = prev.getTextContent();
                        break;
                    }
                }
            }
        }

        ContactAction transferAction = new ContactAction();
        transferAction.setActionId(IdGenerator.generateContactId());
        transferAction.setContactId(contactId);
        transferAction.setSubject("Transferred to " + skillsetName);
        transferAction.setTextContent(bodyText != null ? bodyText : "");
        transferAction.setTextHtml(bodyHtml != null ? bodyHtml : "");
        transferAction.setSource("TransferContactToSkillset");
        transferAction.setComment(note);
        transferAction.setCreationTime(System.currentTimeMillis());
        contactActionRepository.save(transferAction);

        logger.info("transferToSkillset() - ContactID: {} successfully transferred to Skillset: {} ({})",
                contactId, skillsetId, skillsetName);
        return contactId;
    }

    @Transactional
    public long transferToSkillset(long contactId, long skillsetId) {
        return transferToSkillset(contactId, skillsetId, null);
    }

    @Transactional
    public long closeContact(long contactId, String closureText, Long reasonCode, Boolean reasonSpecified) {
        logger.info("closeContact() - Closing ContactID: {}, reasonCode: {}", contactId, reasonCode);
        ContactAction action = new ContactAction();
        action.setActionId(IdGenerator.generateContactId());
        action.setContactId(contactId);
        action.setSubject("CloseContact");
        action.setTextContent(closureText);
        action.setTextHtml(closureText);
        action.setSource("CloseContact");
        action.setComment(closureText);
        action.setCreationTime(System.currentTimeMillis());
        if (Boolean.TRUE.equals(reasonSpecified) && reasonCode != null) {
            action.setClosedReasonNumericValue(reasonCode.intValue());
            action.setClosedReasonName(closureText);
        }
        contactActionRepository.save(action);

        List<Email> emails = requireEmails(contactId);
        for (Email email : emails) {
            email.setStatus("Closed");
        }
        emailRepository.saveAll(emails);
        logger.info("closeContact() - ContactID: {} successfully closed with ActionID: {}", contactId, action.getActionId());
        return action.getActionId();
    }

    public List<AWClosedReasonCode> getClosedReasonCodes() {
        List<AWClosedReasonCode> result = new java.util.ArrayList<>();
        closedReasonRepository.findAll().forEach(closedReason -> {
            if (closedReason == null) {
                return;
            }
            AWClosedReasonCode code = new AWClosedReasonCode();
            code.setName(closedReason.getName());
            code.setNumericValue(closedReason.getOldCodeMappingID());
            result.add(code);
        });
        return result;
    }

    public List<AWSkillset> getAllSkillsets(String skillsetName) {
        logger.info("getAllSkillsets() - Fetching skillsets for skillsetName: {}", skillsetName);
        List<AWSkillset> skillsets = new ArrayList<>();

        // 1. Execute Stored Procedure spGetAllSkillsets
        try {
            skillsets = jdbcTemplate.query(
                con -> {
                    CallableStatement cs = con.prepareCall("{call " + spGetAllSkillsets + "(?)}");
                    if (skillsetName != null && !skillsetName.trim().isEmpty()) {
                        cs.setString(1, skillsetName.trim());
                    } else {
                        cs.setNull(1, Types.VARCHAR);
                    }
                    return cs;
                },
                (rs, rowNum) -> {
                    AWSkillset skillset = new AWSkillset();
                    skillset.setId(rs.getLong("id"));
                    skillset.setName(rs.getString("name"));
                    return skillset;
                }
            );
            logger.info("getAllSkillsets() - {} executed successfully returning {} skillsets", spGetAllSkillsets, skillsets.size());
            return skillsets;
        } catch (Exception ex) {
            logger.warn("getAllSkillsets() - Failed to execute stored procedure {} (Reason: {}). Falling back to JPA repository.", spGetAllSkillsets, ex.getMessage());
        }

        // 2. Fallback to JPA repository if procedure is not present or fails
        try {
            List<SkillMaster> masters;
            if (skillsetName != null && !skillsetName.trim().isEmpty() && !"ALL".equalsIgnoreCase(skillsetName.trim())) {
                String term = skillsetName.trim().toLowerCase();
                masters = skillMasterRepository.findAll().stream()
                        .filter(sm -> (sm.getName() != null && sm.getName().toLowerCase().contains(term))
                                || (sm.getKeywords() != null && sm.getKeywords().toLowerCase().contains(term)))
                        .collect(Collectors.toList());
            } else {
                masters = skillMasterRepository.findAll();
            }

            for (SkillMaster sm : masters) {
                AWSkillset skillset = new AWSkillset();
                skillset.setId(sm.getId());
                skillset.setName(sm.getName());
                skillsets.add(skillset);
            }
            logger.info("getAllSkillsets() - JPA fallback returned {} skillsets", skillsets.size());
        } catch (Exception fallbackEx) {
            logger.error("getAllSkillsets() - JPA fallback also failed", fallbackEx);
        }

        return skillsets;
    }

    @Transactional(readOnly = true)
    public String getHistoryForContactId(String contactIdValue) {
        StringBuilder html = new StringBuilder("<html><p align=\"center\"><b>Contact ID<br>")
                .append(escapeHtml(contactIdValue)).append("</b></p>");
        Long contactId;
        try {
            contactId = Long.parseLong(contactIdValue == null ? "" : contactIdValue.trim());
        } catch (NumberFormatException exception) {
            return html.append("<p><b>Error: Invalid contact ID</b></p></html>").toString();
        }

        List<Email> emails = emailRepository.findByContactId(contactId);
        if (emails == null || emails.isEmpty()) {
            return html.append("<p><b>No history found</b></p></html>").toString();
        }
        List<ContactAction> actions = contactActionRepository.findByContactId(contactId);
        actions.sort(Comparator.comparing((ContactAction a) -> a.getCreationTime() != null ? a.getCreationTime() : 0L)
                .thenComparing(a -> a.getId() != null ? a.getId() : 0L));
        if (actions.isEmpty()) {
            for (Email email : emails) {
                appendHistoryBlock(html, email.getSource(), email.getReceivedDate(), email.getMailFrom(),
                        email.getMailTo(), email.getMailCc(), email.getSubject(), email.getBodyHtml(),
                        email.getText(), email.getStatus(), null, null, email.getAttachments());
            }
        } else {
            for (ContactAction action : actions) {
                appendHistoryBlock(html, action.getSource(), action.getCreationTime(), action.getMailFrom(),
                        action.getMailTo(), action.getMailCc(), action.getSubject(), action.getTextHtml(),
                        action.getTextContent(), action.getClosedReasonName(), action.getComment(),
                        action.getAgentFirstName(), action.getAttachments());
            }
        }
        return html.append("</html>").toString();
    }

    private void appendHistoryBlock(StringBuilder html, String source, Object creationTime, String mailFrom,
                                    String mailTo, String mailCc, String subject, String textHtml, String text,
                                    String closedReason, String comment, String agentName,
                                    List<Attachment> attachments) {
        html.append("<hr><table border=1 cellspacing=0 cellpadding=4 width=90% align=center><tr><td><b>")
                .append(escapeHtml(source == null ? "" : source.replace('_', ' '))).append("</b>");
        appendHistoryField(html, "Date", formatHistoryDate(creationTime));
        appendHistoryField(html, "From", mailFrom);
        appendHistoryField(html, "To", mailTo);
        appendHistoryField(html, "CC", mailCc);
        appendHistoryField(html, "Subject", subject);
        if (textHtml != null && !textHtml.trim().isEmpty()) {
            html.append("<br><b>Content:</b><br>").append(textHtml);
        } else {
            appendHistoryField(html, "Content", text);
        }
        if (attachments != null) {
            for (Attachment attachment : attachments) {
                String fullPath = resolveInternalAttachmentUrl(attachment, null);
                html.append("<br><a target=\"_blank\" href=\"")
                        .append(escapeHtml(fullPath)).append("\">")
                        .append(escapeHtml(attachment.getDisplayName())).append("</a>");
            }
        }
        appendHistoryField(html, "Closed Reason", closedReason);
        appendHistoryField(html, "Agent Note", comment);
        appendHistoryField(html, "Created by Agent", agentName);
        html.append("</td></tr></table>");
    }

    private void appendHistoryField(StringBuilder html, String label, String value) {
        if (value != null && !value.trim().isEmpty()) {
            html.append("<br><b>").append(label).append(":</b> ").append(escapeHtml(value));
        }
    }

    private String formatHistoryDate(Object value) {
        if (value instanceof Long) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date((Long) value));
        }
        if (value instanceof Date) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date) value);
        }
        return value == null ? null : value.toString();
    }

    private String escapeHtml(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    public GetHistoryFromAACCResult getHistory(String searchType, String searchValue) {
        List<Email> emails = emailRepository.findAll();
        if (searchType != null && searchValue != null) {
            String type = searchType.toUpperCase().replaceAll("[_\\-\\s]", "");
            String value = searchValue.trim();
            if (value.startsWith("<") && value.endsWith(">")) {
                value = value.substring(1, value.length() - 1).trim();
            }

            if ("STATUS".equals(type)) {
                emails = "NEW".equalsIgnoreCase(value)
                        ? emailRepository.findByStatusIgnoreCase("NEW")
                        : emailRepository.findAllExceptNewStatus();
            } else if ("TOMAIL".equals(type) || "TO".equals(type) || "MAILTO".equals(type) || "RECIPIENT".equals(type)) {
                emails = emailRepository.findByMailToContainingIgnoreCase(value);
            } else if ("FROMMAIL".equals(type) || "FROM".equals(type) || "MAILFROM".equals(type) || "SENDER".equals(type)) {
                emails = emailRepository.findByMailFromContainingIgnoreCase(value);
            } else if ("SUBJECT".equals(type) || "ORIGINALSUBJECT".equals(type)) {
                emails = emailRepository.findByOriginalSubjectContainingIgnoreCase(value);
            } else if ("AGENTID".equals(type) || "AGENT".equals(type)) {
                emails = findByLong(value, emailRepository::findByAgentId);
            } else if ("ID".equals(type) || "CONTACTID".equals(type)) {
                emails = findByLong(value, emailRepository::findByContactId);
            } else if ("GTID".equals(type) || "LTID".equals(type)) {
                emails = findByLong(value, "GTID".equals(type)
                        ? emailRepository::findByContactIdGreaterThan
                        : emailRepository::findByContactIdLessThan);
            } else {
                emails = emailRepository.findByMailToContainingIgnoreCase(value);
                if (emails == null || emails.isEmpty()) {
                    emails = emailRepository.findByMailFromContainingIgnoreCase(value);
                }
            }
        }

        GetHistoryFromAACCResult result = new GetHistoryFromAACCResult();
        for (Email email : emails) {
            MailHistory history = new MailHistory();
            history.setContactId(email.getContactId() == null ? "" : email.getContactId().toString());
            history.setCreatedTime(email.getReceivedDate() == null ? "" : email.getReceivedDate().toString());
            history.setSubject(email.getSubject() != null && !email.getSubject().trim().isEmpty() ? email.getSubject() : email.getOriginalSubject());
            history.setMailFrom(email.getMailFrom() != null && !email.getMailFrom().trim().isEmpty() ? email.getMailFrom() : email.getSender());
            history.setMailTo(email.getMailTo() != null && !email.getMailTo().trim().isEmpty() ? email.getMailTo() : email.getRecipient());
            history.setMailCC(email.getMailCc() != null ? email.getMailCc() : email.getCc());
            history.setSkillSet(email.getSkillsetName());
            history.setClosedReason(email.getStatus());
            history.setStatus(email.getStatus());
            history.setMessage("Valid Input");
            result.getMailHistory().add(history);
        }
        return result;
    }

    private List<Email> requireEmails(long contactId) {
        List<Email> emails = emailRepository.findByContactId(contactId);
        if (emails == null || emails.isEmpty()) {
            throw new ContactNotFoundException(String.valueOf(contactId));
        }
        return emails;
    }

    private List<Email> findByLong(String value, java.util.function.Function<Long, List<Email>> finder) {
        try {
            Long id = Long.parseLong(value);
            return finder.apply(id);
        } catch (NumberFormatException exception) {
            return Collections.emptyList();
        }
    }
}
