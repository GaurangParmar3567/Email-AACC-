package com.example.mail.service;

import com.example.mail.config.MailProperties;
import com.example.mail.dto.sendMail.ObjMail;
import com.example.mail.model.*;
import com.example.mail.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import javax.mail.internet.MimeMessage;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
public class EmailProcessingService {

    private static final Logger logger = LoggerFactory.getLogger(EmailProcessingService.class);

    private final EmailRepository emailRepository;
    private final MakerTransferStatusRepository makerTransferStatusRepository;
    private final MailSendStatusRepository mailSendStatusRepository;
    private final JavaMailSender mailSender;
    private final MailProperties mailProperties;
    private final SkillMasterRepo skillMasterRepository;
    private final ContactActionRepository contactActionRepository;
    private final UserMasterRepo userMasterRepository;

    public EmailProcessingService(EmailRepository emailRepository,
                                  MakerTransferStatusRepository makerTransferStatusRepository,
                                  MailSendStatusRepository mailSendStatusRepository,
                                  JavaMailSender mailSender, MailProperties mailProperties, SkillMasterRepo skillMasterRepository, ContactActionRepository contactActionRepository, UserMasterRepo userMasterRepository) {
        this.emailRepository = emailRepository;
        this.makerTransferStatusRepository = makerTransferStatusRepository;
        this.mailSendStatusRepository = mailSendStatusRepository;
        this.mailSender = mailSender;
        this.mailProperties = mailProperties;
        this.skillMasterRepository = skillMasterRepository;
        this.contactActionRepository = contactActionRepository;
        this.userMasterRepository = userMasterRepository;
    }

    public boolean transferMakerToChecker(ObjMail dto) {
        try {
            Long contactIdLong = Long.parseLong(dto.getContactId());
            SkillMaster checkerSkillId = skillMasterRepository.findByName(mailProperties.getCheckerSkillName());
            List<Email> emails = emailRepository.findByContactId(contactIdLong);
            Email rootEmail = emails.isEmpty() ? null
                    : emails.get(emails.size() - 1);
            createContactAction(rootEmail, dto);
            for(Email email : emails){
                email.setSkillId(checkerSkillId.getId());
                email.setSkillsetId(checkerSkillId.getId());
                email.setSkillsetName(checkerSkillId.getName());
                email.setAssigned(false);
                email.setAssignedTime(null);
                emailRepository.save(email);
            }

            // Save transfer entry in MakerTransferStatus
            MakerTransferStatus transferStatus = new MakerTransferStatus();
            transferStatus.setFromEmail(String.valueOf(dto.getSkillset()));
            transferStatus.setToEmail(checkerSkillId.getName());
            transferStatus.setSubject(dto.getSubject());
            transferStatus.setBodyContent(rootEmail.getBody());
            transferStatus.setCreatedDate(LocalDateTime.now());
            transferStatus.setContactId(dto.getContactId());
            transferStatus.setAgentId(dto.getAgentId());
            transferStatus.setClosedReason(dto.getClosedReason());
            transferStatus.setComment(dto.getComment());
            transferStatus.setActionId(dto.getActionId());
            transferStatus.setSkillset(dto.getSkillset());

            if (dto.getAnsweredDateTime() != null && !dto.getAnsweredDateTime().isEmpty()) {
                transferStatus.setAnsweredDateTime(parseDateTime(dto.getAnsweredDateTime()));
            }

            makerTransferStatusRepository.save(transferStatus);
            return true;
        } catch (Exception e) {
            logger.error("Error during Maker transfer for ContactID: {}", dto.getContactId(), e);
            return false;
        }
    }

    public boolean sendCheckerEmailToCustomer(ObjMail dto) {
        boolean isSent = false;
        String remarks = "SUCCESS";
        List<Email> emails = emailRepository.findByContactId(Long.valueOf(dto.getContactId()));
        Email rootEmail = emails.isEmpty() ? null
                : emails.get(emails.size() - 1);

        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(rootEmail.getRecipient());
            helper.setTo(dto.getToEmail().split(","));

            if (dto.getCcEmail() != null && !dto.getCcEmail().trim().isEmpty()) {
                helper.setCc(dto.getCcEmail().split(","));
            }
            if (dto.getBccEmail() != null && !dto.getBccEmail().trim().isEmpty()) {
                helper.setBcc(dto.getBccEmail().split(","));
            }

            helper.setSubject(dto.getSubject());
            helper.setText(dto.getBodyContent(), true); // HTML format

            if (dto.getAttachmentFiles() != null) {
                for (String filePath : dto.getAttachmentFiles()) {
                    File file = new File(filePath);
                    if (file.exists()) {
                        helper.addAttachment(file.getName(), file);
                    }
                }
            }

            mailSender.send(message);
            isSent = true;
            logger.info("Email dispatched successfully via SMTP to {}", dto.getToEmail());
            if (dto.getContactId() != null) {
                List<Email> email = emailRepository.findByContactId(Long.parseLong(dto.getContactId()));
                for (Email e : email) {
                    e.setResponded(true);
                    e.setStatus("Closed");
                    emailRepository.save(e);
                }
            }
            createContactAction(rootEmail, dto);
        } catch (Exception e) {
            isSent = false;
            remarks = "FAILED: " + e.getMessage();
            logger.error("Failed sending SMTP mail for ActionID: {}", dto.getActionId(), e);
        }
        saveMailSendStatus(dto, isSent, remarks, rootEmail);
        return isSent;
    }

    private void saveMailSendStatus(ObjMail dto, boolean isSent, String remarks, Email email) {
        MailSendStatus sendStatus = new MailSendStatus();
        sendStatus.setFromEmail(email.getSkillsetName());
        sendStatus.setToEmail(email.getSender());
        sendStatus.setCcEmail(dto.getCcEmail());
        sendStatus.setBccEmail(dto.getBccEmail());
        sendStatus.setSubject(dto.getSubject());
        sendStatus.setBodyContent(dto.getBodyContent());
        sendStatus.setIsSent(isSent);
        sendStatus.setCreatedDate(LocalDateTime.now());
//        sendStatus.setUpdatedDate(LocalDateTime.now());
        sendStatus.setRemarks(remarks);
        sendStatus.setContactId(dto.getContactId());
        sendStatus.setAgentId(dto.getAgentId());
        sendStatus.setClosedReason(dto.getClosedReason());
        sendStatus.setComment(dto.getComment());
        sendStatus.setActionId(dto.getActionId() != null ? dto.getActionId() : 0L);
        sendStatus.setSkillset(dto.getSkillset());

        if (dto.getAnsweredDateTime() != null && !dto.getAnsweredDateTime().isEmpty()) {
            sendStatus.setAnsweredDateTime(parseDateTime(dto.getAnsweredDateTime()));
        }

        mailSendStatusRepository.save(sendStatus);
    }

    private LocalDateTime parseDateTime(String dateStr) {
        try {
            return LocalDateTime.parse(dateStr, DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception e) {
            return LocalDateTime.now();
        }
    }
    private void createContactAction(Email email, ObjMail objMail) {
        ContactAction action = new ContactAction();
        action.setContact(email);
        action.setContactId(Long.valueOf(objMail.getContactId()));
        action.setComment(objMail.getComment());
        action.setClosedReasonName(objMail.getClosedReason());
        action.setClosedReasonNumericValue(0);
        action.setSubject(objMail.getSubject());
        action.setMailFrom(objMail.getFromEmail());
        action.setMailTo(objMail.getToEmail());
        action.setMailCc(objMail.getCcEmail());
        action.setAttachmentFiles(objMail.getAttachmentFiles());

        action.setAgentId(Long.valueOf(objMail.getAgentId()));
        UserMaster userMaster = userMasterRepository.findByAgentId(Long.valueOf(objMail.getAgentId()));
        action.setAgentFirstName(userMaster.getFirstName());
        action.setAgentLastName(userMaster.getLastName());

        action.setCreatedAt(LocalDateTime.now());
        action.setCreationTime(System.currentTimeMillis());
        action.setActionId(objMail.getActionId());

        action.setTextContent(email.getText());
        action.setTextHtml(objMail.getBodyContent());
        action.setRemarks(objMail.getRemarks());
        action.setAnsweredDateTime(objMail.getAnsweredDateTime());
        action.setSkillSet(Long.valueOf(objMail.getSkillset()));

        //default values
        action.setSource("EMail_from_Customer");
        action.setActionType("Email");
        action.setTimeAllocated(20);
        action.setOutboundTalkTime(0);
        action.setOutboundDispositionCode("Initial");
        action.setCallbackStatus("Unspecified");

        ContactAction savedAction = contactActionRepository.save(action);
        email.getContactActions().add(savedAction);
        emailRepository.save(email);
    }
}