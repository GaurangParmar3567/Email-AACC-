package com.example.mail.controller;

import com.example.mail.dto.response.CustomerMailForReplyDTO;
import com.example.mail.dto.soap.aacc.*;
import com.example.mail.dto.soap.checker.ObjMail;
import com.example.mail.dto.soap.checker.SendMailResponse;
import com.example.mail.dto.soap.checker.SendMailResult;
import com.example.mail.dto.soap.checker.SendToCheckerResult;
import com.example.mail.dto.soap.checker.SoapSendMailRequestEnvelope;
import com.example.mail.dto.soap.checker.SoapSendMailResponseBody;
import com.example.mail.dto.soap.checker.SoapSendMailResponseEnvelope;
import com.example.mail.dto.soap.checker.SoapSendToCheckerRequestEnvelope;
import com.example.mail.dto.soap.checker.SoapSendToCheckerResponseEnvelope;
import com.example.mail.exception.ContactNotFoundException;
import com.example.mail.model.SkillMaster;
import com.example.mail.repository.SkillMasterRepo;
import com.example.mail.service.AaccContactService;
import com.example.mail.service.CustomerMailService;
import com.example.mail.service.DeleteAgentSessionKeyService;
import com.example.mail.service.EmailDisplayService;
import com.example.mail.service.GetAgentSessionKeyService;
import com.example.mail.service.MakerTransferStatusService;
import com.example.mail.service.UpdateAuxDetailsService;
import com.example.mail.service.UnableToReadContactService;
import com.example.mail.service.UpdateAgentContactService;
import com.example.mail.service.UploadAttachmentService;
import com.example.mail.service.DeleteAttachmentFileService;
import com.example.mail.service.MailConfigurationService;
import com.example.mail.service.MailSendStatusService;
import com.example.mail.model.SmtpMailCredential;
import com.example.mail.config.MailProperties;
import com.example.mail.model.MailSendStatus;
import com.example.mail.repository.MailSendStatusRepository;
import com.example.mail.util.XmlUtils;
import com.example.mail.util.xml.CustomerMailNamespacePrefixMapper;
import com.example.mail.util.xml.MyNamespacePrefixMapper;
import com.example.mail.util.xml.SendMailNamespacePrefixMapper;
import com.sun.xml.bind.marshaller.CharacterEscapeHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import java.io.File;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import com.example.mail.repository.AttachmentRepository;
import com.example.mail.service.EmailQueueAssignmentService;

import javax.servlet.http.HttpServletRequest;

@RestController
@RequestMapping(value = "/email-service/sbi", consumes = MediaType.TEXT_XML_VALUE, produces = MediaType.TEXT_XML_VALUE)
@CrossOrigin(origins = "*")
public class ContactSoapController {

    private final Logger logger = LoggerFactory.getLogger("MAIL_SERVICES_AVAAYA_LOGGER");
    private final AaccContactService aaccContactService;
    private final EmailDisplayService emailDisplayService;
    private final MakerTransferStatusService makerTransferStatusService;
    private final CustomerMailService customerMailService;
    private final UpdateAuxDetailsService updateAuxDetailsService;
    private final DeleteAgentSessionKeyService deleteAgentSessionKeyService;
    private final GetAgentSessionKeyService getAgentSessionKeyService;
    private final SkillMasterRepo skillMasterRepo;
    private final JavaMailSender javaMailSender;
    private final MailSendStatusRepository mailSendStatusRepository;
    private final MailSendStatusService mailSendStatusService;
    private final MailProperties mailProperties;
    private final MailConfigurationService mailConfigurationService;
    private final UpdateAgentContactService updateAgentContactService;
    private final UploadAttachmentService uploadAttachmentService;
    private final DeleteAttachmentFileService deleteAttachmentFileService;

    @Autowired(required = false)
    private AttachmentRepository attachmentRepository;

    @Autowired(required = false)
    private UnableToReadContactService unableToReadContactService;

    @Autowired(required = false)
    private EmailQueueAssignmentService emailQueueAssignmentService;

    @Value("${upload.path:C:/Uploads/}")
    private String baseUploadPath;

    public ContactSoapController(AaccContactService aaccContactService,
                                 EmailDisplayService emailDisplayService,
                                 MakerTransferStatusService makerTransferStatusService,
                                 CustomerMailService customerMailService,
                                 UpdateAuxDetailsService updateAuxDetailsService,
                                 DeleteAgentSessionKeyService deleteAgentSessionKeyService,
                                 GetAgentSessionKeyService getAgentSessionKeyService,
                                 SkillMasterRepo skillMasterRepo,
                                 JavaMailSender javaMailSender,
                                 MailSendStatusRepository mailSendStatusRepository,
                                 MailSendStatusService mailSendStatusService,
                                 MailProperties mailProperties,
                                 MailConfigurationService mailConfigurationService,
                                 UpdateAgentContactService updateAgentContactService,
                                 UploadAttachmentService uploadAttachmentService,
                                 DeleteAttachmentFileService deleteAttachmentFileService) {
        this.aaccContactService = aaccContactService;
        this.emailDisplayService = emailDisplayService;
        this.makerTransferStatusService = makerTransferStatusService;
        this.customerMailService = customerMailService;
        this.updateAuxDetailsService = updateAuxDetailsService;
        this.deleteAgentSessionKeyService = deleteAgentSessionKeyService;
        this.getAgentSessionKeyService = getAgentSessionKeyService;
        this.skillMasterRepo = skillMasterRepo;
        this.javaMailSender = javaMailSender;
        this.mailSendStatusRepository = mailSendStatusRepository;
        this.mailSendStatusService = mailSendStatusService;
        this.mailProperties = mailProperties;
        this.mailConfigurationService = mailConfigurationService;
        this.updateAgentContactService = updateAgentContactService;
        this.uploadAttachmentService = uploadAttachmentService;
        this.deleteAttachmentFileService = deleteAttachmentFileService;
    }

    @PostMapping("/getContactAACC")
    public ResponseEntity<String> readContact(@RequestBody String rawXmlRequestBody,
                                              HttpServletRequest servletRequest) {
        logger.info("readContact() - Received getContactAACC SOAP request");
        try {
            SoapRequestEnvelope request = unmarshal(rawXmlRequestBody, SoapRequestEnvelope.class);
            if (request == null || request.getBody() == null || request.getBody().getReadContact() == null) {
                logger.warn("readContact() - Rejected getContactAACC request: missing ReadContact element");
                return soapFault(HttpStatus.BAD_REQUEST, "Invalid SOAP request: missing ReadContact element");
            }
            long contactId = request.getBody().getReadContact().getId();
            logger.info("readContact() - Processing getContactAACC for ContactID: {}", contactId);
            ReadContactResult contactDetails = aaccContactService.getContactDetails(contactId);
            ReadContactResponse response = new ReadContactResponse();
            response.setReadContactResult(contactDetails);
            SoapResponseBody body = new SoapResponseBody();
            body.setReadContactResponse(response);
            SoapResponseEnvelope envelope = new SoapResponseEnvelope();
            envelope.setBody(body);
            logger.info("readContact() - getContactAACC completed successfully for ContactID: {}", contactId);

            // Record landed contact in email_queue_assignment
            try {
                if (emailQueueAssignmentService != null) {
                    String sessionId = request.getBody().getReadContact().getSessionKey();
                    if (sessionId == null || sessionId.trim().isEmpty()) {
                        if (servletRequest != null) {
                            sessionId = (String) servletRequest.getAttribute("SESSION_ID");
                            if (sessionId == null || sessionId.trim().isEmpty()) {
                                sessionId = servletRequest.getHeader("Authorization");
                                if (sessionId != null && !sessionId.trim().isEmpty()) {
                                    sessionId = sessionId.trim();
                                    if (sessionId.startsWith("Bearer ") || sessionId.startsWith("bearer ")) {
                                        sessionId = sessionId.substring(7).trim();
                                    }
                                }
                            }
                        }
                    }
                    emailQueueAssignmentService.recordQueueAssignment(contactId, sessionId, contactDetails);
                }
            } catch (Exception ex) {
                logger.error("readContact() - Error recording email_queue_assignment for ContactID: {}", contactId, ex);
            }

            return xmlResponse(marshal(envelope));
        } catch (ContactNotFoundException exception) {
            logger.warn("readContact() - getContactAACC contact not found: {}", exception.getMessage());
            return soapFault(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (Exception exception) {
            logger.error("readContact() - Failure while processing ReadContact SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "SOAP Processing Exception");
        }
    }

    @PostMapping("/SendToChecker")
    public ResponseEntity<String> sendToChecker(@RequestBody String rawXmlRequestBody) {
        logger.info("sendToChecker() - Received SendToChecker SOAP request");
        try {
            SoapSendToCheckerRequestEnvelope request = unmarshal(rawXmlRequestBody,
                    SoapSendToCheckerRequestEnvelope.class);
            if (request == null || request.getBody() == null || request.getBody().getSendToChecker() == null
                    || request.getBody().getSendToChecker().getObjMail() == null) {
                logger.warn("sendToChecker() - Rejected SendToChecker request: missing objMail payload");
                return soapFault(HttpStatus.BAD_REQUEST, "Invalid SOAP request: missing SendToChecker objMail payload");
            }
            logger.info("sendToChecker() - Saving maker transfer for ContactID {} and AgentID {}",
                request.getBody().getSendToChecker().getObjMail().getContactId(),
                request.getBody().getSendToChecker().getObjMail().getAgentId());
            makerTransferStatusService.saveMakerTransferDetails(
                    request.getBody().getSendToChecker().getObjMail());
            SendToCheckerResult result = new SendToCheckerResult();
            result.setMailId(request.getBody().getSendToChecker().getObjMail().getMailId());
            result.setMessage("Maker transfer details saved successfully");
            SoapSendToCheckerResponseEnvelope envelope = new SoapSendToCheckerResponseEnvelope();
            envelope.getBody().setSendToCheckerResponse(result);
            logger.info("sendToChecker() - SendToChecker completed successfully with MailID {}",
                request.getBody().getSendToChecker().getObjMail().getMailId());
            return xmlResponse(marshal(envelope));
        } catch (JAXBException exception) {
            logger.warn("sendToChecker() - Rejected invalid SendToChecker SOAP request", exception);
            return soapFault(HttpStatus.BAD_REQUEST, "Invalid SendToChecker SOAP request");
        } catch (IllegalArgumentException exception) {
            logger.warn("sendToChecker() - Rejected SendToChecker request: {}", exception.getMessage());
            return soapFault(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (Exception exception) {
            logger.error("sendToChecker() - Failure while processing SendToChecker SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to save maker transfer details");
        }
    }

    @PostMapping("/SendMail")
    public ResponseEntity<String> sendMail(@RequestBody String rawXmlRequestBody) {
        logger.info("sendMail() - Received SendMail SOAP request");
        try {
            SoapSendMailRequestEnvelope requestEnvelope = unmarshal(rawXmlRequestBody,
                    SoapSendMailRequestEnvelope.class);

            if (requestEnvelope == null || requestEnvelope.getBody() == null
                    || requestEnvelope.getBody().getSendMail() == null
                    || requestEnvelope.getBody().getSendMail().getObjMail() == null) {
                logger.warn("sendMail() - Rejected SendMail request: missing objMail payload");
                return soapFault(HttpStatus.BAD_REQUEST,
                        "Invalid SOAP request: missing SendMail objMail payload");
            }

            ObjMail objMail = requestEnvelope.getBody().getSendMail().getObjMail();

            String toEmail = cleanEmailField(objMail.getToEmail());
            if (toEmail == null || toEmail.isEmpty()) {
                logger.warn("sendMail() - Rejected SendMail request: ToEmail is required");
                return sendMailResponse(1, "ToEmail is required");
            }
            logger.info("sendMail() - Processing SendMail for ContactID: {}, Skillset: {}, ToEmail: {}",
                    objMail.getContactId(), objMail.getSkillset(), toEmail);

            String fromEmail = cleanEmailField(objMail.getFromEmail());
            String ccEmail = cleanEmailField(objMail.getCcEmail());
            String bccEmail = cleanEmailField(objMail.getBccEmail());
            String subject = objMail.getSubject() == null ? "" : objMail.getSubject().trim();
            String bodyContent = objMail.getBodyContent() == null ? "" : objMail.getBodyContent();

            // Prepare MailSendStatus entity
            MailSendStatus mailSendStatus = new MailSendStatus();
            mailSendStatus.setToEmail(toEmail);
            mailSendStatus.setFromEmail(fromEmail != null && !fromEmail.isEmpty() ? fromEmail : "noreply");
            mailSendStatus.setCcEmail(ccEmail);
            mailSendStatus.setBccEmail(bccEmail);
            mailSendStatus.setSubject(subject);
            mailSendStatus.setBodyContent(bodyContent);
            mailSendStatus.setContactId(objMail.getContactId());
            mailSendStatus.setAgentId(objMail.getAgentId());
            mailSendStatus.setClosedReason(objMail.getClosedReason());
            mailSendStatus.setComment(objMail.getComment());
            mailSendStatus.setSkillset(objMail.getSkillset());
            if (objMail.getRemarks() != null && !objMail.getRemarks().trim().isEmpty()) {
                mailSendStatus.setRemarks(objMail.getRemarks());
            }

            Long actionId = objMail.getActionId();
            if (actionId == null) {
                actionId = System.currentTimeMillis() % 10000000L + 1000000L;
            }
            mailSendStatus.setActionId(actionId);
            mailSendStatus.setAnsweredDateTime(parseAnsweredDateTime(objMail.getAnsweredDateTime()));
            mailSendStatus.setCreatedDate(LocalDateTime.now());
            mailSendStatus.setUpdatedDate(LocalDateTime.now());

            try {
                // Extract SMTP mail credentials via stored procedure using ContactID and Skillset
                SmtpMailCredential credential = mailConfigurationService.getMailConfiguration(
                        objMail.getContactId(), objMail.getSkillset());
                JavaMailSender dynamicMailSender = mailConfigurationService.createJavaMailSender(credential);

                MimeMessage mimeMessage = dynamicMailSender.createMimeMessage();
                MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true, "UTF-8");

                // Prioritize the configured from_email from MailConfiguration
                String senderEmail = null;
                if (credential != null && credential.getFromEmail() != null && !credential.getFromEmail().trim().isEmpty()) {
                    senderEmail = credential.getFromEmail().trim();
                } else if (fromEmail != null && !fromEmail.isEmpty()) {
                    senderEmail = fromEmail;
                }

                String defaultUsername = (credential != null && credential.getUsername() != null && !credential.getUsername().trim().isEmpty())
                        ? credential.getUsername().trim() : null;
                if (defaultUsername == null && mailProperties != null && mailProperties.getAccounts() != null && !mailProperties.getAccounts().isEmpty()) {
                    defaultUsername = mailProperties.getAccounts().get(0).getUsername();
                }

                if (senderEmail != null && senderEmail.contains("@")) {
                    helper.setFrom(senderEmail);
                } else if (defaultUsername != null && !defaultUsername.isEmpty()) {
                    if (senderEmail != null && !senderEmail.isEmpty()) {
                        helper.setFrom(defaultUsername, senderEmail);
                    } else {
                        helper.setFrom(defaultUsername);
                    }
                } else {
                    helper.setFrom("noreply@localhost");
                }

                // Update MailSendStatus with the actual resolved from_email
                if (senderEmail != null && !senderEmail.isEmpty()) {
                    mailSendStatus.setFromEmail(senderEmail);
                }

                logger.info("sendMail() - Using From email: {} for ContactID: {}, Skillset: {}",
                        senderEmail, objMail.getContactId(), objMail.getSkillset());

                helper.setTo(splitRecipients(toEmail));
                if (ccEmail != null && !ccEmail.isEmpty()) {
                    helper.setCc(splitRecipients(ccEmail));
                }
                if (bccEmail != null && !bccEmail.isEmpty()) {
                    helper.setBcc(splitRecipients(bccEmail));
                }
                helper.setSubject(subject);
                helper.setText(bodyContent, true);

                if (objMail.getAttachmentFiles() != null) {
                    for (String attachmentFile : objMail.getAttachmentFiles()) {
                        if (attachmentFile == null || attachmentFile.trim().isEmpty()) {
                            continue;
                        }
                        boolean attached = false;
                        File file = resolveAttachmentFile(attachmentFile.trim());
                        if (file != null && file.exists() && file.isFile()) {
                            helper.addAttachment(file.getName(), file);
                            attached = true;
                            logger.info("sendMail() - Attached file from disk: {}", file.getAbsolutePath());
                        }

                        if (!attached) {
                            Long contactIdLong = null;
                            try {
                                if (objMail.getContactId() != null) {
                                    contactIdLong = Long.parseLong(objMail.getContactId().trim());
                                }
                            } catch (Exception ignored) {}

                            byte[] dbData = resolveAttachmentFromDatabase(attachmentFile.trim(), contactIdLong);
                            if (dbData != null && dbData.length > 0) {
                                String fileName = extractFileName(attachmentFile.trim());
                                helper.addAttachment(fileName, new org.springframework.core.io.ByteArrayResource(dbData));
                                attached = true;
                                logger.info("sendMail() - Attached file from database: {} for ContactID: {}", fileName, contactIdLong);
                            }
                        }

                        if (!attached) {
                            logger.warn("sendMail() - Skipping attachment {} because file does not exist on disk or in database", attachmentFile);
                        }
                    }
                }

                dynamicMailSender.send(mimeMessage);

                mailSendStatus.setIsSent(true);
                mailSendStatus.setRemarks("Mail sent successfully");
                mailSendStatusService.saveSentMailDetails(mailSendStatus, objMail);
                try {
                    Long contactId = objMail.getContactId() == null
                        ? null : Long.valueOf(objMail.getContactId().trim());
                    int updatedEmails = emailDisplayService.markContactReplied(contactId);
                    logger.info("sendMail() - Marked {} email(s) as replied for ContactID {}",
                        updatedEmails, contactId);
                } catch (Exception statusEx) {
                    logger.error("sendMail() - Mail was sent, but contact reply status could not be updated for ContactID {}",
                        objMail.getContactId(), statusEx);
                }
                logger.info("sendMail() - Mail sent successfully to {} and processed via MailSendStatusService (MailID: {})",
                        toEmail, mailSendStatus.getMailId());

                return sendMailResponse(0, "Success");
            } catch (Exception sendEx) {
                logger.error("sendMail() - Failure while sending SendMail via SMTP", sendEx);
                mailSendStatus.setIsSent(false);
                String errorMsg = sendEx.getMessage() != null ? sendEx.getMessage() : sendEx.getClass().getSimpleName();
                if (errorMsg.length() > 490) {
                    errorMsg = errorMsg.substring(0, 490);
                }
                mailSendStatus.setRemarks("Failed: " + errorMsg);
                try {
                    mailSendStatusService.saveSentMailDetails(mailSendStatus, objMail);
                } catch (Exception dbEx) {
                    logger.error("sendMail() - Failed to record failed send status in MailSendStatus", dbEx);
                }
                return sendMailResponse(1, "Error : " + errorMsg);
            }
        } catch (JAXBException exception) {
            logger.warn("sendMail() - Rejected invalid SendMail SOAP request", exception);
            return soapFault(HttpStatus.BAD_REQUEST, "Invalid SendMail SOAP request");
        } catch (Exception exception) {
            logger.error("sendMail() - Failure while processing SendMail SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "SOAP Processing Exception");
        }
    }

    @PostMapping("/TransferContactToSkillset")
    public ResponseEntity<String> transferContactToSkillset(@RequestBody String rawXmlRequestBody) {
        logger.info("transferContactToSkillset() - Received TransferContactToSkillset SOAP request");
        try {
            TransferContactToSkillsetRequestEnvelope envelope = unmarshal(rawXmlRequestBody,
                    TransferContactToSkillsetRequestEnvelope.class);
            if (envelope.getBody() == null || envelope.getBody().getTransferContactToSkillset() == null) {
                logger.warn("transferContactToSkillset() - Rejected TransferContactToSkillset request: missing payload");
                return soapFault(HttpStatus.BAD_REQUEST, "Invalid SOAP request: missing TransferContactToSkillset payload");
            }
            TransferContactToSkillsetRequest request = envelope.getBody().getTransferContactToSkillset();
            if (request.getId() == null || request.getId().trim().isEmpty() || request.getSkillsetId() == null) {
                logger.warn("transferContactToSkillset() - Rejected TransferContactToSkillset request: id and skillsetId are required");
                return soapFault(HttpStatus.BAD_REQUEST, "id and skillsetId are required");
            }
            long contactId = Long.parseLong(request.getId().trim());
            logger.info("transferContactToSkillset() - Transferring ContactID: {} to SkillsetID: {}", contactId, request.getSkillsetId());
            TransferContactToSkillsetResponseEnvelope responseEnvelope = new TransferContactToSkillsetResponseEnvelope();
            responseEnvelope.getBody().setTransferContactToSkillsetResponse(
                    new TransferContactToSkillsetResult(
                            aaccContactService.transferToSkillset(contactId, request.getSkillsetId(), request.getNote())));
            logger.info("transferContactToSkillset() - TransferContactToSkillset completed successfully for ContactID: {}, SkillsetID: {}",
                    contactId, request.getSkillsetId());
            return xmlResponse(marshal(responseEnvelope));
        } catch (ContactNotFoundException exception) {
            logger.warn("transferContactToSkillset() - TransferContactToSkillset contact not found: {}", exception.getMessage());
            return soapFault(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (NumberFormatException exception) {
            logger.warn("transferContactToSkillset() - TransferContactToSkillset invalid contact id: {}", exception.getMessage());
            return soapFault(HttpStatus.BAD_REQUEST, "Invalid contact id format");
        } catch (IllegalArgumentException exception) {
            logger.warn("transferContactToSkillset() - TransferContactToSkillset illegal argument: {}", exception.getMessage());
            return soapFault(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (Exception exception) {
            logger.error("transferContactToSkillset() - Failure while processing TransferContactToSkillset SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to transfer contact to skillset");
        }
    }

    @PostMapping("/GetAllSkillsets")
    public ResponseEntity<String> getAllSkillsets(
            @RequestParam(value = "skillsetName", required = false) String paramSkillsetName,
            @RequestParam(value = "skillName", required = false) String paramSkillName,
            @RequestParam(value = "name", required = false) String paramName,
            @RequestBody String rawXmlRequestBody) {
        logger.info("getAllSkillsets() - Received GetAllSkillsets SOAP request");
        try {
            SoapRequestEnvelope request = unmarshal(rawXmlRequestBody, SoapRequestEnvelope.class);
            if (request.getBody() == null || request.getBody().getGetAllSkillsets() == null) {
                logger.warn("getAllSkillsets() - Rejected GetAllSkillsets request: missing GetAllSkillsets element");
                return soapFault(HttpStatus.BAD_REQUEST, "Invalid SOAP request: missing GetAllSkillsets element");
            }

            String skillsetName = null;
            if (paramSkillsetName != null && !paramSkillsetName.trim().isEmpty()) {
                skillsetName = paramSkillsetName.trim();
            } else if (paramSkillName != null && !paramSkillName.trim().isEmpty()) {
                skillsetName = paramSkillName.trim();
            } else if (paramName != null && !paramName.trim().isEmpty()) {
                skillsetName = paramName.trim();
            } else if (request.getBody().getGetAllSkillsets() != null) {
                skillsetName = request.getBody().getGetAllSkillsets().getEffectiveSkillsetName();
            }

            if (skillsetName == null && rawXmlRequestBody != null) {
                skillsetName = extractXmlTag(rawXmlRequestBody, "skillsetName");
                if (skillsetName == null) {
                    skillsetName = extractXmlTag(rawXmlRequestBody, "skillName");
                }
                if (skillsetName == null) {
                    skillsetName = extractXmlTag(rawXmlRequestBody, "name");
                }
            }

            logger.info("getAllSkillsets() - Executing GetAllSkillsets for skillsetName: {}", skillsetName);
            List<AWSkillset> skillsets = aaccContactService.getAllSkillsets(skillsetName);

            GetAllSkillsetsResult result = new GetAllSkillsetsResult();
            result.getAwsSkillsets().addAll(skillsets);

            GetAllSkillsetsResponse response = new GetAllSkillsetsResponse();
            response.setGetAllSkillsetsResult(result);

            SoapResponseBody body = new SoapResponseBody();
            body.setGetAllSkillsetsResponse(response);

            SoapResponseEnvelope envelope = new SoapResponseEnvelope();
            envelope.setBody(body);
            logger.info("getAllSkillsets() - GetAllSkillsets completed successfully returning {} skillsets", skillsets.size());
            return xmlResponse(marshal(envelope));
        } catch (JAXBException exception) {
            logger.warn("getAllSkillsets() - Rejected invalid GetAllSkillsets SOAP request", exception);
            return soapFault(HttpStatus.BAD_REQUEST, "Invalid GetAllSkillsets SOAP request");
        } catch (Exception exception) {
            logger.error("getAllSkillsets() - Failure while processing GetAllSkillsets SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "SOAP Processing Exception");
        }
    }

    @PostMapping("/GetAllClosedReasonCodes")
    public ResponseEntity<String> getAllClosedReasonCodes(@RequestBody String rawXmlRequestBody) {
        logger.info("getAllClosedReasonCodes() - Received GetAllClosedReasonCodes SOAP request");
        try {
            SoapRequestEnvelope request = unmarshal(rawXmlRequestBody, SoapRequestEnvelope.class);
            if (request.getBody() == null || request.getBody().getGetAllClosedReasonCodes() == null) {
                logger.warn("getAllClosedReasonCodes() - Rejected GetAllClosedReasonCodes request: missing GetAllClosedReasonCodes element");
                return soapFault(HttpStatus.BAD_REQUEST, "Invalid SOAP request: missing GetAllClosedReasonCodes element");
            }
            GetAllClosedReasonCodesResult result = new GetAllClosedReasonCodesResult();
            result.getAwClosedReasonCodes().addAll(aaccContactService.getClosedReasonCodes());
            GetAllClosedReasonCodesResponse response = new GetAllClosedReasonCodesResponse();
            response.setGetAllClosedReasonCodesResult(result);
            SoapResponseBody body = new SoapResponseBody();
            body.setGetAllClosedReasonCodesResponse(response);
            SoapResponseEnvelope envelope = new SoapResponseEnvelope();
            envelope.setBody(body);
            logger.info("getAllClosedReasonCodes() - GetAllClosedReasonCodes completed successfully returning {} codes",
                    result.getAwClosedReasonCodes().size());
            return xmlResponse(marshal(envelope));
        } catch (Exception exception) {
            logger.error("getAllClosedReasonCodes() - Failure while processing GetAllClosedReasonCodes SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "SOAP Processing Exception");
        }
    }

    @PostMapping("/GetHistoryFromAACC")
    public ResponseEntity<String> getHistoryFromAACC(@RequestBody String rawXmlRequestBody) {
        logger.info("getHistoryFromAACC() - Received GetHistoryFromAACC SOAP request");
        try {
            SoapHistoryRequestEnvelope request = unmarshal(rawXmlRequestBody, SoapHistoryRequestEnvelope.class);
            if (request.getBody() == null || request.getBody().getGetHistoryFromAACC() == null) {
                logger.warn("getHistoryFromAACC() - Rejected GetHistoryFromAACC request: missing GetHistoryFromAACC element");
                return soapFault(HttpStatus.BAD_REQUEST, "Invalid SOAP request: missing GetHistoryFromAACC element");
            }
            GetHistoryFromAACCRequest input = request.getBody().getGetHistoryFromAACC();
            logger.info("getHistoryFromAACC() - Fetching history from AACC [SearchType: {}, SearchValue: {}]", input.getSearchType(), input.getSearchValue());
            GetHistoryFromAACCResponse response = new GetHistoryFromAACCResponse();
            response.setGetHistoryFromAACCResult(aaccContactService.getHistory(
                    input.getSearchType(), input.getSearchValue()));
            SoapHistoryResponseBody body = new SoapHistoryResponseBody();
            body.setGetHistoryFromAACCResponse(response);
            SoapHistoryResponseEnvelope envelope = new SoapHistoryResponseEnvelope();
            envelope.setBody(body);
            logger.info("getHistoryFromAACC() - GetHistoryFromAACC completed successfully for SearchValue: {}", input.getSearchValue());
            return xmlResponse(marshal(envelope));
        } catch (Exception exception) {
            logger.error("getHistoryFromAACC() - Failure while processing GetHistoryFromAACC SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "SOAP Processing Exception");
        }
    }

    @PostMapping("/GetHistoryForContactID")
    public ResponseEntity<String> getHistoryForContactId(@RequestBody String rawXmlRequestBody) {
        logger.info("getHistoryForContactId() - Received GetHistoryForContactID SOAP request");
        try {
            HistoryForContactIdRequestEnvelope request = unmarshal(
                    rawXmlRequestBody, HistoryForContactIdRequestEnvelope.class);
            if (request.getBody() == null || request.getBody().getRequest() == null
                    || request.getBody().getRequest().getContactId() == null
                    || request.getBody().getRequest().getContactId().trim().isEmpty()) {
                logger.warn("getHistoryForContactId() - Rejected GetHistoryForContactID request: contactID is required");
                return soapFault(HttpStatus.BAD_REQUEST, "contactID is required");
            }

            String contactId = request.getBody().getRequest().getContactId().trim();
            logger.info("getHistoryForContactId() - Fetching history for ContactID: {}", contactId);
            HistoryForContactIdResponseEnvelope response = new HistoryForContactIdResponseEnvelope();
            response.getBody().getResponse().setResult(
                    aaccContactService.getHistoryForContactId(contactId));
            logger.info("getHistoryForContactId() - GetHistoryForContactID completed successfully for ContactID: {}", contactId);
            return xmlResponse(marshal(response));
        } catch (JAXBException exception) {
            logger.warn("getHistoryForContactId() - Rejected invalid GetHistoryForContactID SOAP request", exception);
            return soapFault(HttpStatus.BAD_REQUEST, "Invalid GetHistoryForContactID SOAP request");
        } catch (Exception exception) {
            logger.error("getHistoryForContactId() - Failure while processing GetHistoryForContactID SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to retrieve contact history");
        }
    }

    @PostMapping("/getContact")
    public ResponseEntity<String> getContact(@RequestBody String rawXmlRequestBody) {
        logger.info("getContact() - Received getContact SOAP request");
        try {
            SoapContactRequestEnvelope request = unmarshal(rawXmlRequestBody, SoapContactRequestEnvelope.class);
            Long agentId = request.getBody().getGetContact().getStrAgentID();
            logger.info("getContact() - Assigning next pending email for AgentID: {}", agentId);
            Long contactId = emailDisplayService.assignNextPendingEmail(agentId);
            GetContactResult result = new GetContactResult();
            result.setContactId(contactId == null ? 0L : contactId);
            result.setMessage(contactId == null || contactId == 0L ? "Invalid Input" : "Valid Input");
            GetContactResponse response = new GetContactResponse();
            response.setGetContactResult(result);
            SoapContactResponseBody body = new SoapContactResponseBody();
            body.setGetContactResponse(response);
            SoapContactResponseEnvelope envelope = new SoapContactResponseEnvelope();
            envelope.setBody(body);
            logger.info("getContact() - getContact completed for AgentID: {}, assigned ContactID: {}", agentId, contactId);
            return xmlResponse(marshal(envelope));
        } catch (Exception exception) {
            logger.error("getContact() - Failure while processing GetContact SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "SOAP Processing Exception");
        }
    }

    @PostMapping("/CloseContact")
    public ResponseEntity<String> closeContact(@RequestBody String rawXmlRequestBody) {
        logger.info("closeContact() - Received CloseContact SOAP request");
        try {
            SoapRequestEnvelope requestEnvelope = unmarshal(rawXmlRequestBody, SoapRequestEnvelope.class);
            if (requestEnvelope.getBody() == null || requestEnvelope.getBody().getCloseContact() == null) {
                logger.warn("closeContact() - Rejected CloseContact request: missing CloseContact element");
                return soapFault(HttpStatus.BAD_REQUEST, "Invalid SOAP request: missing CloseContact element");
            }
            CloseContactRequest request = requestEnvelope.getBody().getCloseContact();
            long contactId = Long.parseUnsignedLong(request.getId());
            logger.info("closeContact() - Closing ContactID: {}, ReasonCode: {}", contactId, request.getClosedReasonCodeValue());
            long actionId = aaccContactService.closeContact(contactId, request.getClosureText(),
                    request.getClosedReasonCodeValue(), request.getClosedReasonCodeSpecified());
            CloseContactResponse response = new CloseContactResponse();
            response.setCloseContactResult(new CloseContactResult(actionId));
            SoapResponseBody body = new SoapResponseBody();
            body.setCloseContactResponse(response);
            SoapResponseEnvelope envelope = new SoapResponseEnvelope();
            envelope.setBody(body);
            logger.info("closeContact() - CloseContact completed successfully for ContactID: {}, ActionID: {}", contactId, actionId);
            return xmlResponse(marshal(envelope));
        } catch (ContactNotFoundException exception) {
            logger.warn("closeContact() - CloseContact contact not found: {}", exception.getMessage());
            return soapFault(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (NumberFormatException exception) {
            logger.warn("closeContact() - CloseContact invalid contact id: {}", exception.getMessage());
            return soapFault(HttpStatus.BAD_REQUEST, "Invalid contact id format");
        } catch (Exception exception) {
            logger.error("closeContact() - Failure while processing CloseContact SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "SOAP Processing Exception");
        }
    }

    @PostMapping("/customerMail")
    public ResponseEntity<String> getCustomerMail(@RequestBody String rawXmlRequestBody) {
        logger.info("getCustomerMail() - Received customerMail SOAP request");
        try {
            SoapCustomerMailRequestEnvelope requestEnvelope = unmarshal(
                    rawXmlRequestBody, SoapCustomerMailRequestEnvelope.class);
            if (requestEnvelope == null || requestEnvelope.getBody() == null) {
                logger.warn("getCustomerMail() - Rejected customerMail request: SOAP Body is required");
                return soapFault(HttpStatus.BAD_REQUEST, "SOAP Body is required");
            }
            CustomerMailRequest request;
            boolean makerOperation;
            if (requestEnvelope.getBody().getGetCustomerMailforMaker() != null) {
                request = requestEnvelope.getBody().getGetCustomerMailforMaker();
                makerOperation = true;
            } else if (requestEnvelope.getBody().getGetCustomerMailforChecker() != null) {
                request = requestEnvelope.getBody().getGetCustomerMailforChecker();
                makerOperation = false;
            } else {
                logger.warn("getCustomerMail() - Rejected customerMail request: GetCustomerMailforMaker or GetCustomerMailforChecker is required");
                return soapFault(HttpStatus.BAD_REQUEST,
                        "GetCustomerMailforMaker or GetCustomerMailforChecker is required");
            }
            if (request.getContactId() == null || request.getContactId().trim().isEmpty()) {
                logger.warn("getCustomerMail() - Rejected customerMail request: ContactID is required");
                return soapFault(HttpStatus.BAD_REQUEST, "ContactID is required");
            }

            String operation = makerOperation ? "GetCustomerMailforMaker" : "GetCustomerMailforChecker";
            String contactId = request.getContactId().trim();
            logger.info("getCustomerMail() - Processing {} for ContactID: {}", operation, contactId);

            CustomerMailForReplyDTO mail = makerOperation
                    ? customerMailService.getCustomerMailforMaker(contactId)
                    : customerMailService.getCustomerMailforChecker(contactId);
            CustomerMailResult result = new CustomerMailResult();
            result.setMessage(mail.getMessage());
            result.setReplyText(XmlUtils.cdata(mail.getReplyText()));
            result.setClosedReason(mail.getClosedReason());
            result.setComment(mail.getComment());
            result.setGetError(mail.getGetError());

            SoapCustomerMailResponseEnvelope responseEnvelope = new SoapCustomerMailResponseEnvelope();
            CustomerMailResponseBody responseBody = new CustomerMailResponseBody();
            if (makerOperation) {
                MakerCustomerMailResponse response = new MakerCustomerMailResponse();
                response.setResult(result);
                responseBody.setGetCustomerMailforMakerResponse(response);
            } else {
                CheckerCustomerMailResponse response = new CheckerCustomerMailResponse();
                response.setResult(result);
                responseBody.setGetCustomerMailforCheckerResponse(response);
            }
            responseEnvelope.setBody(responseBody);

            Marshaller marshaller = JAXBContext.newInstance(
                    SoapCustomerMailResponseEnvelope.class,
                    CustomerMailResponseBody.class,
                    MakerCustomerMailResponse.class,
                    CheckerCustomerMailResponse.class,
                    CustomerMailResult.class).createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.FALSE);
            marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper",
                    new CustomerMailNamespacePrefixMapper());
            marshaller.setProperty("com.sun.xml.bind.marshaller.CharacterEscapeHandler",
                    (CharacterEscapeHandler) (characters, start, length, isAttVal, writer) -> {
                        String value = new String(characters, start, length);
                        if (value.startsWith("<![CDATA[") && value.endsWith("]]>") && !isAttVal) {
                            writer.write(value);
                        } else {
                            XmlUtils.escapeXmlCharacters(characters, start, length, isAttVal, writer);
                        }
                    });
            StringWriter writer = new StringWriter();
            marshaller.marshal(responseEnvelope, writer);
            logger.info("getCustomerMail() - {} completed successfully for ContactID: {}", operation, contactId);
            return xmlResponse(writer.toString());
        } catch (JAXBException exception) {
            logger.warn("getCustomerMail() - Rejected invalid GetCustomerMail SOAP request", exception);
            return soapFault(HttpStatus.BAD_REQUEST, "Invalid GetCustomerMail SOAP request");
        } catch (ContactNotFoundException exception) {
            logger.warn("getCustomerMail() - GetCustomerMail contact not found: {}", exception.getMessage());
            return soapFault(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (Exception exception) {
            logger.error("getCustomerMail() - Failure while processing GetCustomerMail SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to retrieve customer mail");
        }
    }

    @PostMapping(value = "/UpdateAuxDetails")
    public ResponseEntity<String> updateAuxDetails(@RequestBody String rawXmlRequestBody) {
        logger.info("updateAuxDetails() - Received UpdateAuxDetails SOAP request");
        try {
            UpdateAuxDetailsRequest request = null;

            // 1. Demarshalling via JAXB (SOAP Envelope)
            try {
                SoapUpdateAuxDetailsRequestEnvelope envelope = unmarshal(rawXmlRequestBody,
                        SoapUpdateAuxDetailsRequestEnvelope.class);
                if (envelope != null && envelope.getBody() != null && envelope.getBody().getUpdateAuxDetails() != null) {
                    request = envelope.getBody().getUpdateAuxDetails();
                }
            } catch (Exception ex) {
                logger.debug("updateAuxDetails() - JAXB demarshalling as SoapUpdateAuxDetailsRequestEnvelope failed: {}", ex.getMessage());
            }

            // Fallback demarshalling for direct UpdateAuxDetails element
            if (request == null || request.getAgentID() == null || request.getAgentID().trim().isEmpty()) {
                try {
                    request = unmarshal(rawXmlRequestBody, UpdateAuxDetailsRequest.class);
                } catch (Exception ignored) {
                }
            }

            // Fallback tag extraction if needed
            if (request == null || request.getAgentID() == null || request.getAgentID().trim().isEmpty()) {
                String agentId = extractXmlTag(rawXmlRequestBody, "AgentID");
                if (agentId == null) {
                    agentId = extractXmlTag(rawXmlRequestBody, "agentID");
                }
                if (agentId != null && !agentId.trim().isEmpty()) {
                    if (request == null) {
                        request = new UpdateAuxDetailsRequest();
                    }
                    request.setAgentID(agentId.trim());

                    String auxCode = extractXmlTag(rawXmlRequestBody, "AuxCode");
                    if (auxCode == null) auxCode = extractXmlTag(rawXmlRequestBody, "auxCode");
                    request.setAuxCode(auxCode);

                    String startTime = extractXmlTag(rawXmlRequestBody, "StartTime");
                    if (startTime == null) startTime = extractXmlTag(rawXmlRequestBody, "startTime");
                    request.setStartTime(startTime);

                    String endTime = extractXmlTag(rawXmlRequestBody, "EndTime");
                    if (endTime == null) endTime = extractXmlTag(rawXmlRequestBody, "endTime");
                    request.setEndTime(endTime);
                }
            }

            if (request == null || request.getAgentID() == null || request.getAgentID().trim().isEmpty()) {
                logger.warn("updateAuxDetails() - Rejected UpdateAuxDetails request: AgentID is required");
                return soapFault(HttpStatus.BAD_REQUEST, "Invalid SOAP request: AgentID is required");
            }

            String agentId = request.getAgentID().trim();
            logger.info("updateAuxDetails() - Processing UpdateAuxDetails for AgentID: {}, AuxCode: {}", agentId, request.getAuxCode());
            UpdateAuxDetailsResponse response = updateAuxDetailsService.updateAuxDetails(request);
            logger.info("updateAuxDetails() - UpdateAuxDetails completed successfully for AgentID: {}", agentId);

            // JAXB Marshalling of response envelope
            SoapUpdateAuxDetailsResponseEnvelope responseEnvelope = new SoapUpdateAuxDetailsResponseEnvelope(
                    new SoapUpdateAuxDetailsResponseBody(response)
            );
            return xmlResponse(marshal(responseEnvelope));
        } catch (JAXBException exception) {
            logger.warn("updateAuxDetails() - Rejected invalid UpdateAuxDetails SOAP request", exception);
            return soapFault(HttpStatus.BAD_REQUEST, "Invalid UpdateAuxDetails SOAP request");
        } catch (Exception exception) {
            logger.error("updateAuxDetails() - Failure while processing UpdateAuxDetails SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "SOAP Processing Exception");
        }
    }

    @PostMapping(value = "/DeleteAgentSessionKey")
    public ResponseEntity<String> deleteAgentSessionKey(@RequestBody String rawXmlRequestBody) {
        logger.info("deleteAgentSessionKey() - Received DeleteAgentSessionKey SOAP request");
        try {
            DeleteAgentSessionKeyRequest request = null;

            // 1. Demarshalling via JAXB (SOAP Envelope)
            try {
                SoapDeleteAgentSessionKeyRequestEnvelope envelope = unmarshal(rawXmlRequestBody,
                        SoapDeleteAgentSessionKeyRequestEnvelope.class);
                if (envelope != null && envelope.getBody() != null && envelope.getBody().getDeleteAgentSessionKey() != null) {
                    request = envelope.getBody().getDeleteAgentSessionKey();
                }
            } catch (Exception ex) {
                logger.debug("deleteAgentSessionKey() - JAXB demarshalling as SoapDeleteAgentSessionKeyRequestEnvelope failed: {}", ex.getMessage());
            }

            // Fallback direct demarshalling
            if (request == null || request.getStrAvayaAgentID() == null || request.getStrAvayaAgentID().trim().isEmpty()) {
                try {
                    request = unmarshal(rawXmlRequestBody, DeleteAgentSessionKeyRequest.class);
                } catch (Exception ignored) {
                }
            }

            // Fallback tag extraction
            if (request == null || request.getStrAvayaAgentID() == null || request.getStrAvayaAgentID().trim().isEmpty()) {
                String agentId = extractXmlTag(rawXmlRequestBody, "strAvayaAgentID");
                if (agentId == null) {
                    agentId = extractXmlTag(rawXmlRequestBody, "agentloginId");
                }
                String sessionKey = extractXmlTag(rawXmlRequestBody, "strSessionKey");
                if (sessionKey == null) {
                    sessionKey = extractXmlTag(rawXmlRequestBody, "agentsessionid");
                }
                if (agentId != null && !agentId.trim().isEmpty()) {
                    if (request == null) {
                        request = new DeleteAgentSessionKeyRequest();
                    }
                    request.setStrAvayaAgentID(agentId.trim());
                    request.setStrSessionKey(sessionKey != null ? sessionKey.trim() : "");
                }
            }

            if (request == null || request.getStrAvayaAgentID() == null
                    || request.getStrAvayaAgentID().trim().isEmpty()
                    || request.getStrSessionKey() == null || request.getStrSessionKey().trim().isEmpty()) {
                logger.warn("deleteAgentSessionKey() - Rejected DeleteAgentSessionKey request: strAvayaAgentID and strSessionKey are required");
                return soapFault(HttpStatus.BAD_REQUEST,
                        "Invalid SOAP request: strAvayaAgentID and strSessionKey are required");
            }

            String agentId = request.getStrAvayaAgentID().trim();
            logger.info("deleteAgentSessionKey() - Processing DeleteAgentSessionKey for AgentID: {}", agentId);
            DeleteAgentSessionKeyResponse response = deleteAgentSessionKeyService.deleteAgentSessionKey(request);
            logger.info("deleteAgentSessionKey() - DeleteAgentSessionKey completed successfully for AgentID: {}", agentId);

            // JAXB Marshalling of response envelope
            SoapDeleteAgentSessionKeyResponseEnvelope responseEnvelope = new SoapDeleteAgentSessionKeyResponseEnvelope(
                    new SoapDeleteAgentSessionKeyResponseBody(response)
            );
            return xmlResponse(marshal(responseEnvelope));
        } catch (JAXBException exception) {
            logger.warn("deleteAgentSessionKey() - Rejected invalid DeleteAgentSessionKey SOAP request", exception);
            return soapFault(HttpStatus.BAD_REQUEST, "Invalid DeleteAgentSessionKey SOAP request");
        } catch (Exception exception) {
            logger.error("deleteAgentSessionKey() - Failure while processing DeleteAgentSessionKey SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "SOAP Processing Exception");
        }
    }

    @PostMapping(value = "/GetAgentSessionKey")
    public ResponseEntity<String> getAgentSessionKey(@RequestBody String rawXmlRequestBody) {
        logger.info("getAgentSessionKey() - Received GetAgentSessionKey SOAP request");
        try {
            GetAgentSessionKeyRequest request = null;

            // 1. Demarshalling via JAXB (SOAP Envelope)
            try {
                SoapGetAgentSessionKeyRequestEnvelope envelope = unmarshal(rawXmlRequestBody,
                        SoapGetAgentSessionKeyRequestEnvelope.class);
                if (envelope != null && envelope.getBody() != null && envelope.getBody().getGetAgentSessionKey() != null) {
                    request = envelope.getBody().getGetAgentSessionKey();
                }
            } catch (Exception ex) {
                logger.debug("getAgentSessionKey() - JAXB demarshalling as SoapGetAgentSessionKeyRequestEnvelope failed: {}", ex.getMessage());
            }

            // Fallback direct demarshalling
            if (request == null || request.getStrAvayaAgentID() == null || request.getStrAvayaAgentID().trim().isEmpty()) {
                try {
                    request = unmarshal(rawXmlRequestBody, GetAgentSessionKeyRequest.class);
                } catch (Exception ignored) {
                }
            }

            // Fallback tag extraction
            if (request == null || request.getStrAvayaAgentID() == null || request.getStrAvayaAgentID().trim().isEmpty()) {
                String agentId = extractXmlTag(rawXmlRequestBody, "strAvayaAgentID");
                if (agentId == null) {
                    agentId = extractXmlTag(rawXmlRequestBody, "AgentID");
                }
                String sessionKey = extractXmlTag(rawXmlRequestBody, "strSessionKey");
                if (sessionKey == null) {
                    sessionKey = extractXmlTag(rawXmlRequestBody, "sessionKey");
                }
                if (agentId != null && !agentId.trim().isEmpty()) {
                    if (request == null) {
                        request = new GetAgentSessionKeyRequest();
                    }
                    request.setStrAvayaAgentID(agentId.trim());
                    request.setStrSessionKey(sessionKey != null ? sessionKey.trim() : "");
                }
            }

            if (request == null || request.getStrAvayaAgentID() == null
                    || request.getStrAvayaAgentID().trim().isEmpty()
                    || request.getStrSessionKey() == null || request.getStrSessionKey().trim().isEmpty()) {
                logger.warn("getAgentSessionKey() - Rejected GetAgentSessionKey request: strAvayaAgentID and strSessionKey are required");
                return soapFault(HttpStatus.BAD_REQUEST,
                        "Invalid SOAP request: strAvayaAgentID and strSessionKey are required");
            }

            String agentId = request.getStrAvayaAgentID().trim();
            logger.info("getAgentSessionKey() - Processing GetAgentSessionKey for AgentID: {}", agentId);
            GetAgentSessionKeyResponse response = getAgentSessionKeyService.getAgentSessionKey(request);
            logger.info("getAgentSessionKey() - GetAgentSessionKey completed successfully for AgentID: {}", agentId);

            // JAXB Marshalling of response envelope
            SoapGetAgentSessionKeyResponseEnvelope responseEnvelope = new SoapGetAgentSessionKeyResponseEnvelope(
                    new SoapGetAgentSessionKeyResponseBody(response)
            );
            return xmlResponse(marshal(responseEnvelope));
        } catch (JAXBException exception) {
            logger.warn("getAgentSessionKey() - Rejected invalid GetAgentSessionKey SOAP request", exception);
            return soapFault(HttpStatus.BAD_REQUEST, "Invalid GetAgentSessionKey SOAP request");
        } catch (Exception exception) {
            logger.error("getAgentSessionKey() - Failure while processing GetAgentSessionKey SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "SOAP Processing Exception");
        }
    }


    @PostMapping(value = "/UpdateAgentContact")
    public ResponseEntity<String> updateAgentContact(@RequestBody String rawXmlRequestBody) {
        logger.info("updateAgentContact() - Received UpdateAgentContact SOAP request");
        try {
            UpdateAgentContactRequest request = null;

            // 1. Demarshalling via JAXB (SOAP Envelope)
            try {
                SoapUpdateAgentContactRequestEnvelope envelope = unmarshal(rawXmlRequestBody,
                        SoapUpdateAgentContactRequestEnvelope.class);
                if (envelope != null && envelope.getBody() != null && envelope.getBody().getUpdateAgentContact() != null) {
                    request = envelope.getBody().getUpdateAgentContact();
                }
            } catch (Exception ex) {
                logger.debug("updateAgentContact() - JAXB demarshalling as SoapUpdateAgentContactRequestEnvelope failed: {}", ex.getMessage());
            }

            // Fallback direct demarshalling
            if (request == null || request.getStrAgentID() == null || request.getStrAgentID().trim().isEmpty()) {
                try {
                    request = unmarshal(rawXmlRequestBody, UpdateAgentContactRequest.class);
                } catch (Exception ignored) {
                }
            }

            // Fallback tag extraction
            if (request == null || request.getStrAgentID() == null || request.getStrAgentID().trim().isEmpty()) {
                String agentId = extractXmlTag(rawXmlRequestBody, "strAgentID");
                if (agentId == null) {
                    agentId = extractXmlTag(rawXmlRequestBody, "AgentID");
                }
                if (agentId != null && !agentId.trim().isEmpty()) {
                    if (request == null) {
                        request = new UpdateAgentContactRequest();
                    }
                    request.setStrAgentID(cleanCdata(agentId.trim()));
                }
            }

            if (request == null || request.getStrAgentID() == null || request.getStrAgentID().trim().isEmpty()) {
                logger.warn("updateAgentContact() - Rejected UpdateAgentContact request: strAgentID is required");
                return soapFault(HttpStatus.BAD_REQUEST, "Invalid SOAP request: strAgentID is required");
            }

            String agentId = cleanCdata(request.getStrAgentID().trim());
            request.setStrAgentID(agentId);
            logger.info("updateAgentContact() - Processing UpdateAgentContact for agentID: {}", agentId);

            UpdateAgentContactResponse objResult = updateAgentContactService.updateAgentContact(request);
            logger.info("updateAgentContact() - UpdateAgentContact completed for agentID: {}", agentId);

            SoapUpdateAgentContactResponseEnvelope responseEnvelope = new SoapUpdateAgentContactResponseEnvelope(
                    new SoapUpdateAgentContactResponseBody(objResult)
            );
            return xmlResponse(marshal(responseEnvelope));
        } catch (JAXBException exception) {
            logger.warn("updateAgentContact() - Rejected invalid UpdateAgentContact SOAP request", exception);
            return soapFault(HttpStatus.BAD_REQUEST, "Invalid UpdateAgentContact SOAP request");
        } catch (Exception exception) {
            logger.error("updateAgentContact() - Failure while processing UpdateAgentContact SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "SOAP Processing Exception");
        }
    }

    @PostMapping(value = "/UnableToReadContact")
    public ResponseEntity<String> unableToReadContact(@RequestBody String rawXmlRequestBody) {
        logger.info("unableToReadContact() - Received UnableToReadContact SOAP request");
        try {
            UnableToReadContactRequest request = null;

            // 1. Demarshalling via JAXB (SOAP Envelope)
            try {
                SoapUnableToReadContactRequestEnvelope envelope = unmarshal(rawXmlRequestBody,
                        SoapUnableToReadContactRequestEnvelope.class);
                if (envelope != null && envelope.getBody() != null && envelope.getBody().getUnableToReadContact() != null) {
                    request = envelope.getBody().getUnableToReadContact();
                }
            } catch (Exception ex) {
                logger.debug("unableToReadContact() - JAXB demarshalling as SoapUnableToReadContactRequestEnvelope failed: {}", ex.getMessage());
            }

            // Fallback direct demarshalling
            if (request == null || request.getStrAgentID() == null || request.getStrAgentID().trim().isEmpty()) {
                try {
                    request = unmarshal(rawXmlRequestBody, UnableToReadContactRequest.class);
                } catch (Exception ignored) {
                }
            }

            // Fallback tag extraction
            if (request == null || request.getStrAgentID() == null || request.getStrAgentID().trim().isEmpty()) {
                String agentId = extractXmlTag(rawXmlRequestBody, "strAgentID");
                if (agentId == null) {
                    agentId = extractXmlTag(rawXmlRequestBody, "AgentID");
                }
                String contactId = extractXmlTag(rawXmlRequestBody, "strContactID");
                if (contactId == null) {
                    contactId = extractXmlTag(rawXmlRequestBody, "ContactID");
                }
                String error = extractXmlTag(rawXmlRequestBody, "strError");
                if (error == null) {
                    error = extractXmlTag(rawXmlRequestBody, "Error");
                }
                if (agentId != null || contactId != null) {
                    if (request == null) {
                        request = new UnableToReadContactRequest();
                    }
                    if (agentId != null) request.setStrAgentID(cleanCdata(agentId.trim()));
                    if (contactId != null) request.setStrContactID(cleanCdata(contactId.trim()));
                    if (error != null) request.setStrError(cleanCdata(error.trim()));
                }
            }

            if (request == null || request.getStrAgentID() == null || request.getStrAgentID().trim().isEmpty()) {
                logger.warn("unableToReadContact() - Rejected UnableToReadContact request: strAgentID is required");
                return soapFault(HttpStatus.BAD_REQUEST, "Invalid SOAP request: strAgentID is required");
            }

            request.setStrAgentID(cleanCdata(request.getStrAgentID().trim()));
            if (request.getStrContactID() != null) {
                request.setStrContactID(cleanCdata(request.getStrContactID().trim()));
            }
            if (request.getStrError() != null) {
                request.setStrError(cleanCdata(request.getStrError().trim()));
            }

            logger.info("unableToReadContact() - Processing UnableToReadContact for agentID: {}, contactID: {}",
                    request.getStrAgentID(), request.getStrContactID());

            UnableToReadContactResponse objResult = unableToReadContactService != null
                    ? unableToReadContactService.unableToReadContact(request)
                    : new UnableToReadContactResponse(0, "Valid Input");

            logger.info("unableToReadContact() - UnableToReadContact completed for agentID: {}, contactID: {}",
                    request.getStrAgentID(), request.getStrContactID());

            SoapUnableToReadContactResponseEnvelope responseEnvelope = new SoapUnableToReadContactResponseEnvelope(
                    new SoapUnableToReadContactResponseBody(objResult)
            );
            return xmlResponse(marshal(responseEnvelope));
        } catch (JAXBException exception) {
            logger.warn("unableToReadContact() - Rejected invalid UnableToReadContact SOAP request", exception);
            return soapFault(HttpStatus.BAD_REQUEST, "Invalid UnableToReadContact SOAP request");
        } catch (Exception exception) {
            logger.error("unableToReadContact() - Failure while processing UnableToReadContact SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "SOAP Processing Exception");
        }
    }

    @PostMapping(value = "/UploadAttachment")
    public ResponseEntity<String> uploadAttachment(@RequestBody String rawXmlRequestBody) {
        logger.info("uploadAttachment() - Received UploadAttachment SOAP request");
        try {
            UploadAttachmentRequest request = null;

            // 1. Demarshalling via JAXB (SOAP Envelope)
            try {
                SoapUploadAttachmentRequestEnvelope envelope = unmarshal(rawXmlRequestBody,
                        SoapUploadAttachmentRequestEnvelope.class);
                if (envelope != null && envelope.getBody() != null && envelope.getBody().getUploadAttachment() != null) {
                    request = envelope.getBody().getUploadAttachment();
                }
            } catch (Exception ex) {
                logger.debug("uploadAttachment() - JAXB demarshalling as SoapUploadAttachmentRequestEnvelope failed: {}", ex.getMessage());
            }

            // Fallback direct demarshalling
            if (request == null || request.getStrFileName() == null || request.getStrFileName().trim().isEmpty()) {
                try {
                    request = unmarshal(rawXmlRequestBody, UploadAttachmentRequest.class);
                } catch (Exception ignored) {
                }
            }

            // Fallback tag extraction
            if (request == null || request.getStrFileName() == null || request.getStrFileName().trim().isEmpty() || request.getStrContent() == null) {
                String fileName = cleanCdata(extractXmlTag(rawXmlRequestBody, "strFileName"));
                if (fileName == null) {
                    fileName = cleanCdata(extractXmlTag(rawXmlRequestBody, "StrFileName"));
                }
                String logicalPath = cleanCdata(extractXmlTag(rawXmlRequestBody, "strLogicalPath"));
                if (logicalPath == null) {
                    logicalPath = cleanCdata(extractXmlTag(rawXmlRequestBody, "StrLogicalPath"));
                }
                String contentStr = cleanCdata(extractXmlTag(rawXmlRequestBody, "strContent"));
                if (contentStr == null) {
                    contentStr = cleanCdata(extractXmlTag(rawXmlRequestBody, "StrContent"));
                }

                if (request == null) {
                    request = new UploadAttachmentRequest();
                }
                if (fileName != null) {
                    request.setStrFileName(fileName);
                }
                if (logicalPath != null) {
                    request.setStrLogicalPath(logicalPath);
                }
                if (contentStr != null && request.getStrContent() == null) {
                    try {
                        request.setStrContent(java.util.Base64.getDecoder().decode(contentStr.trim().replaceAll("\\s+", "")));
                    } catch (Exception e) {
                        request.setStrContent(contentStr.getBytes(java.nio.charset.StandardCharsets.UTF_8));
                    }
                }
            }

            if (request == null || request.getStrFileName() == null || request.getStrFileName().trim().isEmpty()) {
                logger.warn("uploadAttachment() - Rejected UploadAttachment request: strFileName is required");
                return soapFault(HttpStatus.BAD_REQUEST, "Invalid SOAP request: strFileName is required");
            }

            request.setStrFileName(cleanCdata(request.getStrFileName()));
            if (request.getStrLogicalPath() != null) {
                request.setStrLogicalPath(cleanCdata(request.getStrLogicalPath()));
            }

            String fileName = request.getStrFileName();
            logger.info("uploadAttachment() - Processing UploadAttachment with fileName: {}", fileName);

            UploadAttachmentResponse objResult = uploadAttachmentService.uploadAttachment(request);
            logger.info("uploadAttachment() - Completed UploadAttachment for fileName: {}", fileName);

            SoapUploadAttachmentResponseEnvelope responseEnvelope = new SoapUploadAttachmentResponseEnvelope(
                    new SoapUploadAttachmentResponseBody(objResult)
            );
            return xmlResponse(marshal(responseEnvelope));
        } catch (JAXBException exception) {
            logger.warn("uploadAttachment() - Rejected invalid UploadAttachment SOAP request", exception);
            return soapFault(HttpStatus.BAD_REQUEST, "Invalid UploadAttachment SOAP request");
        } catch (Exception exception) {
            logger.error("uploadAttachment() - Failure while processing UploadAttachment SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "SOAP Processing Exception");
        }
    }

    @PostMapping(value = "/DeleteAttachmentFile")
    public ResponseEntity<String> deleteAttachmentFile(@RequestBody String rawXmlRequestBody) {
        logger.info("deleteAttachmentFile() - Received DeleteAttachmentFile SOAP request");
        try {
            DeleteAttachmentFileRequest request = null;

            // 1. Demarshalling via JAXB (SOAP Envelope)
            try {
                SoapDeleteAttachmentFileRequestEnvelope envelope = unmarshal(rawXmlRequestBody,
                        SoapDeleteAttachmentFileRequestEnvelope.class);
                if (envelope != null && envelope.getBody() != null && envelope.getBody().getDeleteAttachmentFile() != null) {
                    request = envelope.getBody().getDeleteAttachmentFile();
                }
            } catch (Exception ex) {
                logger.debug("deleteAttachmentFile() - JAXB demarshalling as SoapDeleteAttachmentFileRequestEnvelope failed: {}", ex.getMessage());
            }

            // Fallback direct demarshalling
            if (request == null || request.getStrFileName() == null || request.getStrFileName().trim().isEmpty()) {
                try {
                    request = unmarshal(rawXmlRequestBody, DeleteAttachmentFileRequest.class);
                } catch (Exception ignored) {
                }
            }

            // Fallback tag extraction
            if (request == null || request.getStrFileName() == null || request.getStrFileName().trim().isEmpty()) {
                String fileName = cleanCdata(extractXmlTag(rawXmlRequestBody, "strFileName"));
                if (fileName == null) {
                    fileName = cleanCdata(extractXmlTag(rawXmlRequestBody, "StrFileName"));
                }
                String logicalPath = cleanCdata(extractXmlTag(rawXmlRequestBody, "strLogicalPath"));
                if (logicalPath == null) {
                    logicalPath = cleanCdata(extractXmlTag(rawXmlRequestBody, "StrLogicalPath"));
                }
                if (fileName != null && !fileName.trim().isEmpty()) {
                    if (request == null) {
                        request = new DeleteAttachmentFileRequest();
                    }
                    request.setStrFileName(fileName.trim());
                    request.setStrLogicalPath(logicalPath != null ? logicalPath.trim() : "");
                }
            }

            if (request == null || request.getStrFileName() == null || request.getStrFileName().trim().isEmpty()) {
                logger.warn("deleteAttachmentFile() - Rejected DeleteAttachmentFile request: strFileName is required");
                return soapFault(HttpStatus.BAD_REQUEST, "Invalid SOAP request: strFileName is required");
            }

            request.setStrFileName(cleanCdata(request.getStrFileName()));
            if (request.getStrLogicalPath() != null) {
                request.setStrLogicalPath(cleanCdata(request.getStrLogicalPath()));
            }

            String fileName = request.getStrFileName();
            logger.info("deleteAttachmentFile() - Processing DeleteAttachmentFile with fileName: {}", fileName);

            DeleteAttachmentFileResponse objResult = deleteAttachmentFileService.deleteAttachmentFile(request);
            logger.info("deleteAttachmentFile() - Completed DeleteAttachmentFile for fileName: {}", fileName);

            SoapDeleteAttachmentFileResponseEnvelope responseEnvelope = new SoapDeleteAttachmentFileResponseEnvelope(
                    new SoapDeleteAttachmentFileResponseBody(objResult)
            );
            return xmlResponse(marshal(responseEnvelope));
        } catch (JAXBException exception) {
            logger.warn("deleteAttachmentFile() - Rejected invalid DeleteAttachmentFile SOAP request", exception);
            return soapFault(HttpStatus.BAD_REQUEST, "Invalid DeleteAttachmentFile SOAP request");
        } catch (Exception exception) {
            logger.error("deleteAttachmentFile() - Failure while processing DeleteAttachmentFile SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "SOAP Processing Exception");
        }
    }

    



    private <T> T unmarshal(String xml, Class<T> type) throws JAXBException {
        if (xml == null || xml.trim().isEmpty()) {
            return null;
        }
        String normalizedXml = normalizeSoapRequestXml(xml);
        Unmarshaller unmarshaller = JAXBContext.newInstance(type).createUnmarshaller();
        return type.cast(unmarshaller.unmarshal(new StringReader(normalizedXml)));
    }

    private String[] splitRecipients(String recipients) {
        if (recipients == null || recipients.trim().isEmpty()) {
            return new String[0];
        }
        return Arrays.stream(recipients.split("[;,]"))
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .toArray(String[]::new);
    }

    private String marshal(Object envelope) throws JAXBException {
        Marshaller marshaller = JAXBContext.newInstance(envelope.getClass()).createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.FALSE);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "utf-8");
        try {
            marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new MyNamespacePrefixMapper());
        } catch (PropertyException exception) {
            marshaller.setProperty("org.glassfish.jaxb.namespacePrefixMapper", new MyNamespacePrefixMapper());
        }
        StringWriter writer = new StringWriter();
        marshaller.marshal(envelope, writer);
        return writer.toString();
    }

    private ResponseEntity<String> xmlResponse(String body) {
        return ResponseEntity.ok().contentType(MediaType.TEXT_XML).body(body);
    }

    private ResponseEntity<String> soapFault(HttpStatus status, String message) {
        return ResponseEntity.status(status).contentType(MediaType.TEXT_XML).body(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                        + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>"
                        + "<soap:Fault><faultcode>soap:Client</faultcode><faultstring>"
                        + XmlUtils.escapeXml(message == null ? "Invalid SOAP request" : message)
                        + "</faultstring></soap:Fault></soap:Body></soap:Envelope>");
    }

    private ResponseEntity<String> sendMailResponse(int resultId, String messageDetails) {
        try {
            SoapSendMailResponseEnvelope envelope = createSendMailResponse(resultId, messageDetails);
            String xml = marshalSendMailResponse(envelope);
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/xml; charset=utf-8"))
                    .body(xml);
        } catch (Exception e) {
            logger.error("sendMailResponse() - Failed to marshal SendMailResponse", e);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to marshal SendMailResponse");
        }
    }

    private String marshalSendMailResponse(SoapSendMailResponseEnvelope envelope) throws JAXBException {
        Marshaller marshaller = JAXBContext.newInstance(
                SoapSendMailResponseEnvelope.class,
                SoapSendMailResponseBody.class,
                SendMailResponse.class,
                SendMailResult.class).createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.FALSE);
        marshaller.setProperty(Marshaller.JAXB_ENCODING, "utf-8");
        try {
            marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new SendMailNamespacePrefixMapper());
        } catch (PropertyException exception) {
            marshaller.setProperty("org.glassfish.jaxb.namespacePrefixMapper", new SendMailNamespacePrefixMapper());
        }
        StringWriter writer = new StringWriter();
        marshaller.marshal(envelope, writer);
        return writer.toString();
    }

    private SoapSendMailResponseEnvelope createSendMailResponse(int resultId, String messageDetails) {
        SendMailResult result = new SendMailResult();
        result.setResultID(resultId);
        result.setMessageDetails(messageDetails);

        SendMailResponse sendMailResponse = new SendMailResponse();
        sendMailResponse.setSendMailResult(result);

        SoapSendMailResponseBody responseBody = new SoapSendMailResponseBody();
        responseBody.setSendMailResponse(sendMailResponse);

        SoapSendMailResponseEnvelope responseEnvelope = new SoapSendMailResponseEnvelope();
        responseEnvelope.setBody(responseBody);

        return responseEnvelope;
    }

    private String cleanEmailField(String field) {
        if (field == null) return null;
        String val = field.trim();
        if (val.startsWith("<![CDATA[") && val.endsWith("]]>")) {
            val = val.substring(9, val.length() - 3).trim();
        }
        while (val.startsWith(";") || val.startsWith(",")) {
            val = val.substring(1).trim();
        }
        return val;
    }

    private String cleanCdata(String val) {
        if (val == null) return null;
        val = val.trim();
        while (val.startsWith("<![CDATA[") && val.endsWith("]]>")) {
            val = val.substring(9, val.length() - 3).trim();
        }
        return val;
    }

    private LocalDateTime parseAnsweredDateTime(String answeredDateTime) {
        if (answeredDateTime == null || answeredDateTime.trim().isEmpty()) {
            return LocalDateTime.now();
        }
        String val = cleanEmailField(answeredDateTime);
        try {
            return LocalDateTime.parse(val, java.time.format.DateTimeFormatter.ISO_DATE_TIME);
        } catch (Exception exception) {
            try {
                java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                return LocalDateTime.parse(val, formatter);
            } catch (Exception ex) {
                try {
                    java.time.format.DateTimeFormatter formatter = java.time.format.DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ss");
                    return LocalDateTime.parse(val, formatter);
                } catch (Exception ex2) {
                    return LocalDateTime.now();
                }
            }
        }
    }

    private String normalizeSoapRequestXml(String rawXml) {
        if (rawXml == null || rawXml.trim().isEmpty()) {
            return "";
        }
        String normalized = rawXml;
        if (normalized.contains("http://www.w3.org/2003/05/soap-envelope")) {
            normalized = normalized.replace("http://www.w3.org/2003/05/soap-envelope", "http://schemas.xmlsoap.org/soap/envelope/");
        }
        if (!normalized.contains("xmlns=\"http://tempuri.org/\"") && !normalized.contains("xmlns='http://tempuri.org/'")) {
            normalized = normalized.replaceAll("<([\\w]+:)?(SendMail|SendToChecker|GetAgentSessionKey|DeleteAgentSessionKey|UpdateAgentContact|UnableToReadContact|UploadAttachment|DeleteAttachmentFile)(?=[\\s>])(?![^>]*xmlns=)", "<$1$2 xmlns=\"http://tempuri.org/\"");
        }
        return normalized;
    }

    private String extractXmlTag(String xml, String tagName) {
        if (xml == null || tagName == null) {
            return null;
        }
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("<(?:" + tagName + "|[^:>]+:" + tagName + ")[^>]*>(.*?)</(?:[\\w]+:)?" + tagName + ">", java.util.regex.Pattern.DOTALL | java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        pattern = java.util.regex.Pattern.compile("<(?:" + tagName + "|[^:>]+:" + tagName + ")[^>]*>([^<]+)</", java.util.regex.Pattern.CASE_INSENSITIVE);
        matcher = pattern.matcher(xml);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return null;
    }

    private File resolveAttachmentFile(String attachmentPath) {
        if (attachmentPath == null || attachmentPath.trim().isEmpty()) {
            return null;
        }
        String trimmed = attachmentPath.trim();
        File directFile = new File(trimmed);
        if (directFile.isAbsolute() && directFile.exists() && directFile.isFile()) {
            return directFile;
        }

        String baseDir = baseUploadPath != null ? baseUploadPath.trim() : "C:/Uploads/";
        if (!baseDir.endsWith("/") && !baseDir.endsWith("\\")) {
            baseDir += File.separator;
        }

        String cleanRelative = trimmed.replace("\\", "/");
        while (cleanRelative.startsWith("/")) {
            cleanRelative = cleanRelative.substring(1);
        }

        File resolvedFile = new File(baseDir + cleanRelative);
        if (resolvedFile.exists() && resolvedFile.isFile()) {
            return resolvedFile;
        }

        return null;
    }

    private byte[] resolveAttachmentFromDatabase(String attachmentPath, Long contactId) {
        if (attachmentRepository == null) {
            return null;
        }
        String fileName = extractFileName(attachmentPath);
        try {
            if (contactId != null) {
                List<com.example.mail.model.Attachment> attachments = attachmentRepository.findByEmailContactId(contactId);
                if (attachments != null) {
                    for (com.example.mail.model.Attachment att : attachments) {
                        if (att != null && att.getFileName() != null && att.getFileName().equalsIgnoreCase(fileName)) {
                            if (att.getFileData() != null && att.getFileData().length > 0) {
                                return att.getFileData();
                            }
                        }
                    }
                }
            }
        } catch (Exception ex) {
            logger.debug("resolveAttachmentFromDatabase() - Exception looking up attachment by contactId: {}", ex.getMessage());
        }
        return null;
    }

    private String extractFileName(String path) {
        if (path == null) return "attachment";
        String clean = path.replace("\\", "/");
        int idx = clean.lastIndexOf("/");
        return (idx >= 0 && idx < clean.length() - 1) ? clean.substring(idx + 1) : clean;
    }

}
