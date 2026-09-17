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
import com.example.mail.service.MailConfigurationService;
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

@RestController
@RequestMapping(value = "/email-service/sbi", consumes = MediaType.TEXT_XML_VALUE, produces = MediaType.TEXT_XML_VALUE)
@CrossOrigin(origins = "https://ccdemsuat.sbi:6001")
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
    private final MailProperties mailProperties;
    private final MailConfigurationService mailConfigurationService;

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
                                 MailProperties mailProperties,
                                 MailConfigurationService mailConfigurationService) {
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
        this.mailProperties = mailProperties;
        this.mailConfigurationService = mailConfigurationService;
    }

    @PostMapping("/getContactAACC")
    public ResponseEntity<String> readContact(@RequestBody String rawXmlRequestBody) {
        logger.info("Received getContactAACC SOAP request");
        try {
            SoapRequestEnvelope request = unmarshal(rawXmlRequestBody, SoapRequestEnvelope.class);
            if (request == null || request.getBody() == null || request.getBody().getReadContact() == null) {
                logger.warn("Rejected getContactAACC request: missing ReadContact element");
                return soapFault(HttpStatus.BAD_REQUEST, "Invalid SOAP request: missing ReadContact element");
            }
            long contactId = request.getBody().getReadContact().getId();
            logger.info("Processing getContactAACC for ContactID: {}", contactId);
            ReadContactResponse response = new ReadContactResponse();
            response.setReadContactResult(aaccContactService.getContactDetails(contactId));
            SoapResponseBody body = new SoapResponseBody();
            body.setReadContactResponse(response);
            SoapResponseEnvelope envelope = new SoapResponseEnvelope();
            envelope.setBody(body);
            logger.info("getContactAACC completed successfully for ContactID: {}", contactId);
            return xmlResponse(marshal(envelope));
        } catch (ContactNotFoundException exception) {
            logger.warn("getContactAACC contact not found: {}", exception.getMessage());
            return soapFault(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (Exception exception) {
            logger.error("Failure while processing ReadContact SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "SOAP Processing Exception");
        }
    }

    @PostMapping("/SendToChecker")
    public ResponseEntity<String> sendToChecker(@RequestBody String rawXmlRequestBody) {
        logger.info("Received SendToChecker SOAP request");
        try {
            SoapSendToCheckerRequestEnvelope request = unmarshal(rawXmlRequestBody,
                    SoapSendToCheckerRequestEnvelope.class);
            if (request == null || request.getBody() == null || request.getBody().getSendToChecker() == null
                    || request.getBody().getSendToChecker().getObjMail() == null) {
            logger.warn("Rejected SendToChecker request: missing objMail payload");
                return soapFault(HttpStatus.BAD_REQUEST, "Invalid SOAP request: missing SendToChecker objMail payload");
            }
            logger.info("Saving maker transfer for ContactID {} and AgentID {}",
                request.getBody().getSendToChecker().getObjMail().getContactId(),
                request.getBody().getSendToChecker().getObjMail().getAgentId());
            makerTransferStatusService.saveMakerTransferDetails(
                    request.getBody().getSendToChecker().getObjMail());
            SendToCheckerResult result = new SendToCheckerResult();
            result.setMailId(request.getBody().getSendToChecker().getObjMail().getMailId());
            result.setMessage("Maker transfer details saved successfully");
            SoapSendToCheckerResponseEnvelope envelope = new SoapSendToCheckerResponseEnvelope();
            envelope.getBody().setSendToCheckerResponse(result);
                logger.info("SendToChecker completed successfully with MailID {}",
                    request.getBody().getSendToChecker().getObjMail().getMailId());
            return xmlResponse(marshal(envelope));
        } catch (JAXBException exception) {
            logger.warn("Rejected invalid SendToChecker SOAP request", exception);
            return soapFault(HttpStatus.BAD_REQUEST, "Invalid SendToChecker SOAP request");
        } catch (IllegalArgumentException exception) {
            logger.warn("Rejected SendToChecker request: {}", exception.getMessage());
            return soapFault(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (Exception exception) {
            logger.error("Failure while processing SendToChecker SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to save maker transfer details");
        }
    }

    @PostMapping("/SendMail")
    public ResponseEntity<String> sendMail(@RequestBody String rawXmlRequestBody) {
        logger.info("Received SendMail SOAP request");
        try {
            SoapSendMailRequestEnvelope requestEnvelope = unmarshal(rawXmlRequestBody,
                    SoapSendMailRequestEnvelope.class);

            if (requestEnvelope == null || requestEnvelope.getBody() == null
                    || requestEnvelope.getBody().getSendMail() == null
                    || requestEnvelope.getBody().getSendMail().getObjMail() == null) {
                logger.warn("Rejected SendMail request: missing objMail payload");
                return soapFault(HttpStatus.BAD_REQUEST,
                        "Invalid SOAP request: missing SendMail objMail payload");
            }

            ObjMail objMail = requestEnvelope.getBody().getSendMail().getObjMail();

            String toEmail = cleanEmailField(objMail.getToEmail());
            if (toEmail == null || toEmail.isEmpty()) {
                logger.warn("Rejected SendMail request: ToEmail is required");
                return sendMailResponse(0, "ToEmail is required");
            }
            logger.info("Processing SendMail for ContactID: {}, Skillset: {}, ToEmail: {}",
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

                String senderEmail = fromEmail;
                if ((senderEmail == null || senderEmail.isEmpty()) && credential != null && credential.getFromEmail() != null && !credential.getFromEmail().trim().isEmpty()) {
                    senderEmail = credential.getFromEmail().trim();
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
                        File file = new File(attachmentFile.trim());
                        if (file.exists() && file.isFile()) {
                            helper.addAttachment(file.getName(), file);
                        } else {
                            logger.warn("Skipping attachment {} because file does not exist", attachmentFile);
                        }
                    }
                }

                dynamicMailSender.send(mimeMessage);

                mailSendStatus.setIsSent(true);
                mailSendStatus.setRemarks("Mail sent successfully");
                mailSendStatusRepository.save(mailSendStatus);
                logger.info("Mail sent successfully to {} and recorded in MailSendStatus (MailID: {})",
                        toEmail, mailSendStatus.getMailId());

                return sendMailResponse(1, "Mail sent successfully");
            } catch (Exception sendEx) {
                logger.error("Failure while sending SendMail via SMTP", sendEx);
                mailSendStatus.setIsSent(false);
                String errorMsg = sendEx.getMessage() != null ? sendEx.getMessage() : sendEx.getClass().getSimpleName();
                if (errorMsg.length() > 490) {
                    errorMsg = errorMsg.substring(0, 490);
                }
                mailSendStatus.setRemarks("Failed: " + errorMsg);
                try {
                    mailSendStatusRepository.save(mailSendStatus);
                } catch (Exception dbEx) {
                    logger.error("Failed to record failed send status in MailSendStatus", dbEx);
                }
                return sendMailResponse(0, "Unable to send mail: " + errorMsg);
            }
        } catch (JAXBException exception) {
            logger.warn("Rejected invalid SendMail SOAP request", exception);
            return soapFault(HttpStatus.BAD_REQUEST, "Invalid SendMail SOAP request");
        } catch (Exception exception) {
            logger.error("Failure while processing SendMail SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "SOAP Processing Exception");
        }
    }

    @PostMapping("/TransferContactToSkillset")
    public ResponseEntity<String> transferContactToSkillset(@RequestBody String rawXmlRequestBody) {
        logger.info("Received TransferContactToSkillset SOAP request");
        try {
            TransferContactToSkillsetRequestEnvelope envelope = unmarshal(rawXmlRequestBody,
                    TransferContactToSkillsetRequestEnvelope.class);
            if (envelope.getBody() == null || envelope.getBody().getTransferContactToSkillset() == null) {
                logger.warn("Rejected TransferContactToSkillset request: missing payload");
                return soapFault(HttpStatus.BAD_REQUEST, "Invalid SOAP request: missing TransferContactToSkillset payload");
            }
            TransferContactToSkillsetRequest request = envelope.getBody().getTransferContactToSkillset();
            if (request.getId() == null || request.getId().trim().isEmpty() || request.getSkillsetId() == null) {
                logger.warn("Rejected TransferContactToSkillset request: id and skillsetId are required");
                return soapFault(HttpStatus.BAD_REQUEST, "id and skillsetId are required");
            }
            long contactId = Long.parseLong(request.getId().trim());
            logger.info("Transferring ContactID: {} to SkillsetID: {}", contactId, request.getSkillsetId());
            TransferContactToSkillsetResponseEnvelope responseEnvelope = new TransferContactToSkillsetResponseEnvelope();
            responseEnvelope.getBody().setTransferContactToSkillsetResponse(
                    new TransferContactToSkillsetResult(
                            aaccContactService.transferToSkillset(contactId, request.getSkillsetId())));
            logger.info("TransferContactToSkillset completed successfully for ContactID: {}, SkillsetID: {}",
                    contactId, request.getSkillsetId());
            return xmlResponse(marshal(responseEnvelope));
        } catch (ContactNotFoundException exception) {
            logger.warn("TransferContactToSkillset contact not found: {}", exception.getMessage());
            return soapFault(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (NumberFormatException exception) {
            logger.warn("TransferContactToSkillset invalid contact id: {}", exception.getMessage());
            return soapFault(HttpStatus.BAD_REQUEST, "Invalid contact id format");
        } catch (IllegalArgumentException exception) {
            logger.warn("TransferContactToSkillset illegal argument: {}", exception.getMessage());
            return soapFault(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (Exception exception) {
            logger.error("Failure while processing TransferContactToSkillset SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to transfer contact to skillset");
        }
    }

    @PostMapping("/GetAllSkillsets")
    public ResponseEntity<String> getAllSkillsets(@RequestBody String rawXmlRequestBody) {
        logger.info("Received GetAllSkillsets SOAP request");
        try {
            SoapRequestEnvelope request = unmarshal(rawXmlRequestBody, SoapRequestEnvelope.class);
            if (request.getBody() == null || request.getBody().getGetAllSkillsets() == null) {
                logger.warn("Rejected GetAllSkillsets request: missing GetAllSkillsets element");
                return soapFault(HttpStatus.BAD_REQUEST, "Invalid SOAP request: missing GetAllSkillsets element");
            }

            List<SkillMaster> skillMasters = skillMasterRepo.findAll();
            GetAllSkillsetsResult result = new GetAllSkillsetsResult();
            for (SkillMaster skillMaster : skillMasters) {
                AWSkillset skillset = new AWSkillset();
                skillset.setId(skillMaster.getId());
                skillset.setName(skillMaster.getName());
                result.getAwsSkillsets().add(skillset);
            }

            GetAllSkillsetsResponse response = new GetAllSkillsetsResponse();
            response.setGetAllSkillsetsResult(result);

            SoapResponseBody body = new SoapResponseBody();
            body.setGetAllSkillsetsResponse(response);

            SoapResponseEnvelope envelope = new SoapResponseEnvelope();
            envelope.setBody(body);
            logger.info("GetAllSkillsets completed successfully returning {} skillsets", skillMasters.size());
            return xmlResponse(marshal(envelope));
        } catch (JAXBException exception) {
            logger.warn("Rejected invalid GetAllSkillsets SOAP request", exception);
            return soapFault(HttpStatus.BAD_REQUEST, "Invalid GetAllSkillsets SOAP request");
        } catch (Exception exception) {
            logger.error("Failure while processing GetAllSkillsets SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "SOAP Processing Exception");
        }
    }

    @PostMapping("/GetAllClosedReasonCodes")
    public ResponseEntity<String> getAllClosedReasonCodes(@RequestBody String rawXmlRequestBody) {
        logger.info("Received GetAllClosedReasonCodes SOAP request");
        try {
            SoapRequestEnvelope request = unmarshal(rawXmlRequestBody, SoapRequestEnvelope.class);
            if (request.getBody() == null || request.getBody().getGetAllClosedReasonCodes() == null) {
                logger.warn("Rejected GetAllClosedReasonCodes request: missing GetAllClosedReasonCodes element");
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
            logger.info("GetAllClosedReasonCodes completed successfully returning {} codes",
                    result.getAwClosedReasonCodes().size());
            return xmlResponse(marshal(envelope));
        } catch (Exception exception) {
            logger.error("Failure while processing GetAllClosedReasonCodes SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "SOAP Processing Exception");
        }
    }

    @PostMapping("/GetHistoryFromAACC")
    public ResponseEntity<String> getHistoryFromAACC(@RequestBody String rawXmlRequestBody) {
        logger.info("Received GetHistoryFromAACC SOAP request");
        try {
            SoapHistoryRequestEnvelope request = unmarshal(rawXmlRequestBody, SoapHistoryRequestEnvelope.class);
            if (request.getBody() == null || request.getBody().getGetHistoryFromAACC() == null) {
                logger.warn("Rejected GetHistoryFromAACC request: missing GetHistoryFromAACC element");
                return soapFault(HttpStatus.BAD_REQUEST, "Invalid SOAP request: missing GetHistoryFromAACC element");
            }
            GetHistoryFromAACCRequest input = request.getBody().getGetHistoryFromAACC();
            logger.info("Fetching history from AACC [SearchType: {}, SearchValue: {}]", input.getSearchType(), input.getSearchValue());
            GetHistoryFromAACCResponse response = new GetHistoryFromAACCResponse();
            response.setGetHistoryFromAACCResult(aaccContactService.getHistory(
                    input.getSearchType(), input.getSearchValue()));
            SoapHistoryResponseBody body = new SoapHistoryResponseBody();
            body.setGetHistoryFromAACCResponse(response);
            SoapHistoryResponseEnvelope envelope = new SoapHistoryResponseEnvelope();
            envelope.setBody(body);
            logger.info("GetHistoryFromAACC completed successfully for SearchValue: {}", input.getSearchValue());
            return xmlResponse(marshal(envelope));
        } catch (Exception exception) {
            logger.error("Failure while processing GetHistoryFromAACC SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "SOAP Processing Exception");
        }
    }

    @PostMapping("/GetHistoryForContactID")
    public ResponseEntity<String> getHistoryForContactId(@RequestBody String rawXmlRequestBody) {
        logger.info("Received GetHistoryForContactID SOAP request");
        try {
            HistoryForContactIdRequestEnvelope request = unmarshal(
                    rawXmlRequestBody, HistoryForContactIdRequestEnvelope.class);
            if (request.getBody() == null || request.getBody().getRequest() == null
                    || request.getBody().getRequest().getContactId() == null
                    || request.getBody().getRequest().getContactId().trim().isEmpty()) {
                logger.warn("Rejected GetHistoryForContactID request: contactID is required");
                return soapFault(HttpStatus.BAD_REQUEST, "contactID is required");
            }

            String contactId = request.getBody().getRequest().getContactId().trim();
            logger.info("Fetching history for ContactID: {}", contactId);
            HistoryForContactIdResponseEnvelope response = new HistoryForContactIdResponseEnvelope();
            response.getBody().getResponse().setResult(
                    aaccContactService.getHistoryForContactId(contactId));
            logger.info("GetHistoryForContactID completed successfully for ContactID: {}", contactId);
            return xmlResponse(marshal(response));
        } catch (JAXBException exception) {
            logger.warn("Rejected invalid GetHistoryForContactID SOAP request", exception);
            return soapFault(HttpStatus.BAD_REQUEST, "Invalid GetHistoryForContactID SOAP request");
        } catch (Exception exception) {
            logger.error("Failure while processing GetHistoryForContactID SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to retrieve contact history");
        }
    }

    @PostMapping("/getContact")
    public ResponseEntity<String> getContact(@RequestBody String rawXmlRequestBody) {
        logger.info("Received getContact SOAP request");
        try {
            SoapContactRequestEnvelope request = unmarshal(rawXmlRequestBody, SoapContactRequestEnvelope.class);
            Long agentId = request.getBody().getGetContact().getStrAgentID();
            logger.info("Assigning next pending email for AgentID: {}", agentId);
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
            logger.info("getContact completed for AgentID: {}, assigned ContactID: {}", agentId, contactId);
            return xmlResponse(marshal(envelope));
        } catch (Exception exception) {
            logger.error("Failure while processing GetContact SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "SOAP Processing Exception");
        }
    }

    @PostMapping("/CloseContact")
    public ResponseEntity<String> closeContact(@RequestBody String rawXmlRequestBody) {
        logger.info("Received CloseContact SOAP request");
        try {
            SoapRequestEnvelope requestEnvelope = unmarshal(rawXmlRequestBody, SoapRequestEnvelope.class);
            if (requestEnvelope.getBody() == null || requestEnvelope.getBody().getCloseContact() == null) {
                logger.warn("Rejected CloseContact request: missing CloseContact element");
                return soapFault(HttpStatus.BAD_REQUEST, "Invalid SOAP request: missing CloseContact element");
            }
            CloseContactRequest request = requestEnvelope.getBody().getCloseContact();
            long contactId = Long.parseUnsignedLong(request.getId());
            logger.info("Closing ContactID: {}, ReasonCode: {}", contactId, request.getClosedReasonCodeValue());
            long actionId = aaccContactService.closeContact(contactId, request.getClosureText(),
                    request.getClosedReasonCodeValue(), request.getClosedReasonCodeSpecified());
            CloseContactResponse response = new CloseContactResponse();
            response.setCloseContactResult(new CloseContactResult(actionId));
            SoapResponseBody body = new SoapResponseBody();
            body.setCloseContactResponse(response);
            SoapResponseEnvelope envelope = new SoapResponseEnvelope();
            envelope.setBody(body);
            logger.info("CloseContact completed successfully for ContactID: {}, ActionID: {}", contactId, actionId);
            return xmlResponse(marshal(envelope));
        } catch (ContactNotFoundException exception) {
            logger.warn("CloseContact contact not found: {}", exception.getMessage());
            return soapFault(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (NumberFormatException exception) {
            logger.warn("CloseContact invalid contact id: {}", exception.getMessage());
            return soapFault(HttpStatus.BAD_REQUEST, "Invalid contact id format");
        } catch (Exception exception) {
            logger.error("Failure while processing CloseContact SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "SOAP Processing Exception");
        }
    }

    @PostMapping("/customerMail")
    public ResponseEntity<String> getCustomerMail(@RequestBody String rawXmlRequestBody) {
        logger.info("Received customerMail SOAP request");
        try {
            SoapCustomerMailRequestEnvelope requestEnvelope = unmarshal(
                    rawXmlRequestBody, SoapCustomerMailRequestEnvelope.class);
            if (requestEnvelope == null || requestEnvelope.getBody() == null) {
                logger.warn("Rejected customerMail request: SOAP Body is required");
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
                logger.warn("Rejected customerMail request: GetCustomerMailforMaker or GetCustomerMailforChecker is required");
                return soapFault(HttpStatus.BAD_REQUEST,
                        "GetCustomerMailforMaker or GetCustomerMailforChecker is required");
            }
            if (request.getContactId() == null || request.getContactId().trim().isEmpty()) {
                logger.warn("Rejected customerMail request: ContactID is required");
                return soapFault(HttpStatus.BAD_REQUEST, "ContactID is required");
            }

            String operation = makerOperation ? "GetCustomerMailforMaker" : "GetCustomerMailforChecker";
            String contactId = request.getContactId().trim();
            logger.info("Processing {} for ContactID: {}", operation, contactId);

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
            logger.info("{} completed successfully for ContactID: {}", operation, contactId);
            return xmlResponse(writer.toString());
        } catch (JAXBException exception) {
            logger.warn("Rejected invalid GetCustomerMail SOAP request", exception);
            return soapFault(HttpStatus.BAD_REQUEST, "Invalid GetCustomerMail SOAP request");
        } catch (ContactNotFoundException exception) {
            logger.warn("GetCustomerMail contact not found: {}", exception.getMessage());
            return soapFault(HttpStatus.NOT_FOUND, exception.getMessage());
        } catch (Exception exception) {
            logger.error("Failure while processing GetCustomerMail SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to retrieve customer mail");
        }
    }

    @PostMapping(value = "/UpdateAuxDetails")
    public ResponseEntity<String> updateAuxDetails(@RequestBody String rawXmlRequestBody) {
        logger.info("Received UpdateAuxDetails SOAP request");
        try {
            UpdateAuxDetailsRequest request = unmarshal(rawXmlRequestBody, UpdateAuxDetailsRequest.class);
            if (request == null || request.getAgentID() == null || request.getAgentID().trim().isEmpty()) {
                logger.warn("Rejected UpdateAuxDetails request: AgentID is required");
                return soapFault(HttpStatus.BAD_REQUEST, "Invalid SOAP request: AgentID is required");
            }

            String agentId = request.getAgentID().trim();
            logger.info("Processing UpdateAuxDetails for AgentID: {}, AuxCode: {}", agentId, request.getAuxCode());
            UpdateAuxDetailsResponse response = updateAuxDetailsService.updateAuxDetails(request);
            logger.info("UpdateAuxDetails completed successfully for AgentID: {}", agentId);
            return xmlResponse(marshal(response));
        } catch (JAXBException exception) {
            logger.warn("Rejected invalid UpdateAuxDetails SOAP request", exception);
            return soapFault(HttpStatus.BAD_REQUEST, "Invalid UpdateAuxDetails SOAP request");
        } catch (Exception exception) {
            logger.error("Failure while processing UpdateAuxDetails SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "SOAP Processing Exception");
        }
    }

    @PostMapping(value = "/DeleteAgentSessionKey")
    public ResponseEntity<String> deleteAgentSessionKey(@RequestBody String rawXmlRequestBody) {
        logger.info("Received DeleteAgentSessionKey SOAP request");
        try {
            DeleteAgentSessionKeyRequest request = unmarshal(rawXmlRequestBody, DeleteAgentSessionKeyRequest.class);
            if (request == null || request.getStrAvayaAgentID() == null
                    || request.getStrAvayaAgentID().trim().isEmpty()
                    || request.getStrSessionKey() == null || request.getStrSessionKey().trim().isEmpty()) {
                logger.warn("Rejected DeleteAgentSessionKey request: strAvayaAgentID and strSessionKey are required");
                return soapFault(HttpStatus.BAD_REQUEST,
                        "Invalid SOAP request: strAvayaAgentID and strSessionKey are required");
            }

            String agentId = request.getStrAvayaAgentID().trim();
            logger.info("Processing DeleteAgentSessionKey for AgentID: {}", agentId);
            DeleteAgentSessionKeyResponse response = deleteAgentSessionKeyService.deleteAgentSessionKey(request);
            logger.info("DeleteAgentSessionKey completed successfully for AgentID: {}", agentId);
            return xmlResponse(marshal(response));
        } catch (JAXBException exception) {
            logger.warn("Rejected invalid DeleteAgentSessionKey SOAP request", exception);
            return soapFault(HttpStatus.BAD_REQUEST, "Invalid DeleteAgentSessionKey SOAP request");
        } catch (Exception exception) {
            logger.error("Failure while processing DeleteAgentSessionKey SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "SOAP Processing Exception");
        }
    }

    @PostMapping(value = "/GetAgentSessionKey")
    public ResponseEntity<String> getAgentSessionKey(@RequestBody String rawXmlRequestBody) {
        logger.info("Received GetAgentSessionKey SOAP request");
        try {
            GetAgentSessionKeyRequest request = unmarshal(rawXmlRequestBody, GetAgentSessionKeyRequest.class);
            if (request == null || request.getStrAvayaAgentID() == null
                    || request.getStrAvayaAgentID().trim().isEmpty()
                    || request.getStrSessionKey() == null || request.getStrSessionKey().trim().isEmpty()) {
                logger.warn("Rejected GetAgentSessionKey request: strAvayaAgentID and strSessionKey are required");
                return soapFault(HttpStatus.BAD_REQUEST,
                        "Invalid SOAP request: strAvayaAgentID and strSessionKey are required");
            }

            String agentId = request.getStrAvayaAgentID().trim();
            logger.info("Processing GetAgentSessionKey for AgentID: {}", agentId);
            GetAgentSessionKeyResponse response = getAgentSessionKeyService.getAgentSessionKey(request);
            logger.info("GetAgentSessionKey completed successfully for AgentID: {}", agentId);
            return xmlResponse(marshal(response));
        } catch (JAXBException exception) {
            logger.warn("Rejected invalid GetAgentSessionKey SOAP request", exception);
            return soapFault(HttpStatus.BAD_REQUEST, "Invalid GetAgentSessionKey SOAP request");
        } catch (Exception exception) {
            logger.error("Failure while processing GetAgentSessionKey SOAP request", exception);
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
            logger.error("Failed to marshal SendMailResponse", e);
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
            normalized = normalized.replaceAll("<([\\w]+:)?(SendMail|SendToChecker)(?=[\\s>])(?![^>]*xmlns=)", "<$1$2 xmlns=\"http://tempuri.org/\"");
        }
        return normalized;
    }

}
