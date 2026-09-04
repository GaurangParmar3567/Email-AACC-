package com.example.mail.controller;

import com.example.mail.dto.MyNamespacePrefixMapper;
import com.example.mail.dto.sendToChecker.SendToCheckerResult;
import com.example.mail.dto.sendToChecker.SoapSendToCheckerRequestEnvelope;
import com.example.mail.dto.sendToChecker.SoapSendToCheckerResponseEnvelope;
import com.example.mail.dto.soapContact.*;
import com.example.mail.dto.soapnew.*;
import com.example.mail.model.Attachment;
import com.example.mail.model.ContactAction;
import com.example.mail.model.ClosedReason;
import com.example.mail.model.Email;
import com.example.mail.model.UserMaster;
import com.example.mail.repository.ClosedReasonRepository;
import com.example.mail.repository.ContactActionRepository;
import com.example.mail.repository.EmailRepository;
import com.example.mail.repository.UserMasterRepo;
import com.example.mail.service.MakerTransferStatusService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.xml.bind.*;
import java.io.StringReader;
import java.io.StringWriter;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@RestController
@RequestMapping(value = "/email-service/sbi",
        consumes = MediaType.TEXT_XML_VALUE,
        produces = MediaType.TEXT_XML_VALUE)
@CrossOrigin(origins = "https://ccdemsuat.sbi:6001")
public class ContactSoapController {

    private final Logger logger = LoggerFactory.getLogger("MAIL_SERVICES_AVAAYA_LOGGER");
    private final EmailRepository emailRepository;
    private final UserMasterRepo userMasterRepository;
    private final ContactActionRepository contactActionRepository;
    private final ClosedReasonRepository closedReasonRepository;
    private final MakerTransferStatusService makerTransferStatusService;
    private static final JAXBContext RESPONSE_CONTEXT;

    static {
        try {
            RESPONSE_CONTEXT = JAXBContext.newInstance(
                    SoapResponseEnvelope.class,
                    SoapContactResponseEnvelope.class,
                    GetAllClosedReasonCodesResponse.class,
                    GetAllClosedReasonCodesResult.class,
                    AWClosedReasonCode.class,
                    SoapHistoryResponseEnvelope.class,
                    SoapHistoryResponseBody.class,
                    GetHistoryFromAACCResponse.class,
                    GetHistoryFromAACCResult.class,
                    MailHistory.class
                        ,
                        CloseContactResponse.class,
                        CloseContactResult.class,
                        CloseContactRequest.class,
                        SoapSendToCheckerResponseEnvelope.class,
                        SendToCheckerResult.class
            );
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize JAXB context", e);
        }
    }

    public ContactSoapController(EmailRepository emailRepository, UserMasterRepo userMasterRepository,
                                 ContactActionRepository contactActionRepository,
                                 ClosedReasonRepository closedReasonRepository,
                                 MakerTransferStatusService makerTransferStatusService) {
        this.emailRepository = emailRepository;
        this.userMasterRepository = userMasterRepository;
        this.contactActionRepository = contactActionRepository;
        this.closedReasonRepository = closedReasonRepository;
        this.makerTransferStatusService = makerTransferStatusService;
    }

    @PostMapping(value = "/getContactAACC")
    public ResponseEntity<String> readContact(@RequestBody String rawXmlRequestBody) {
        try {
            JAXBContext requestContext = JAXBContext.newInstance(SoapRequestEnvelope.class);
            Unmarshaller unmarshaller = requestContext.createUnmarshaller();
            SoapRequestEnvelope requestEnvelope = (SoapRequestEnvelope) unmarshaller.unmarshal(new StringReader(rawXmlRequestBody));

            if (requestEnvelope.getBody() == null || requestEnvelope.getBody().getReadContact() == null) {
                logger.warn("Incoming SOAP request is missing the ReadContact payload.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("<error>Invalid SOAP request: missing ReadContact element</error>");
            }

            Long contactId = requestEnvelope.getBody().getReadContact().getId();
            logger.info("Received ReadContact SOAP request for Contact ID: {}", contactId);

            List<Email> emailThread = emailRepository.findByContactId(contactId);
            if (emailThread == null || emailThread.isEmpty()) {
                logger.warn("Contact ID {} not found in system database.", contactId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("<error>Contact Not Found</error>");
            }
            Email rootEmail = emailThread.stream()
                    .filter(e -> e.getParentEmail() == null)
                    .findFirst()
                    .orElse(emailThread.get(0));

            SoapResponseEnvelope responseEnvelope = new SoapResponseEnvelope();
            SoapResponseBody body = new SoapResponseBody();
            ReadContactResponse readContactResponse = new ReadContactResponse();
            ReadContactResult result = new ReadContactResult();

            result.setId(rootEmail.getContactId());
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

            if (rootEmail.getSkillsetId() != null) {
                SkillsetDTO skillset = new SkillsetDTO();
                skillset.setId(rootEmail.getSkillsetId());
                skillset.setName(rootEmail.getSkillsetName());
                result.setSkillset(skillset);
            }

            if (rootEmail.getArrivalTime() != null) {
                MillisecondsDTO arrTime = new MillisecondsDTO();
                arrTime.setMilliseconds(rootEmail.getArrivalTime());
                result.setArrivalTime(arrTime);
            }

            if (rootEmail.getOpenTime() != null) {
                MillisecondsDTO opTime = new MillisecondsDTO();
                opTime.setMilliseconds(rootEmail.getOpenTime());
                result.setOpenTime(opTime);
            }

            if (rootEmail.getAgentId() != null) {
                AgentDTO agent = new AgentDTO();
                agent.setId(rootEmail.getAgentId());
                agent.setFirstName(rootEmail.getAgentFirstName());
                agent.setLastName(rootEmail.getAgentLastName());
                result.setAgent(agent);
            }

            ActionListDTO actionListDto = new ActionListDTO();

            for (Email threadEmail : emailThread) {
                if (threadEmail.getContactActions() == null) continue;

                List<ContactAction> actions = threadEmail.getContactActions();
                if (actions == null) {
                    continue;
                }

                for (ContactAction action : actions) {
                    if (action == null) {
                        continue;
                    }
                    AWActionDTO actionDto = new AWActionDTO();
                    actionDto.setId(action.getActionId());
                    actionDto.setContactID(action.getContactId());
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
                        MillisecondsDTO cTime = new MillisecondsDTO();
                        cTime.setMilliseconds(action.getCreationTime());
                        actionDto.setCreationTime(cTime);
                    }

                    if (action.getAgentId() != null) {
                        AgentDTO actAgent = new AgentDTO();
                        actAgent.setId(action.getAgentId());
                        actAgent.setFirstName(action.getAgentFirstName());
                        actAgent.setLastName(action.getAgentLastName());
                        actionDto.setAgent(actAgent);
                    }

                    if (action.getClosedReasonName() != null || action.getClosedReasonNumericValue() != null) {
                        ClosedReasonDTO reason = new ClosedReasonDTO();
                        reason.setName(action.getClosedReasonName());
                        reason.setNumericValue(action.getClosedReasonNumericValue());
                        actionDto.setClosedReason(reason);
                    }
                    if (action.getAttachments() != null && !action.getAttachments().isEmpty()) {
                        AttachmentListDTO attachmentListDto = new AttachmentListDTO();

                        for (Attachment attachment : action.getAttachments()) {
                            AWAttachmentDTO attDto = new AWAttachmentDTO();
                            attDto.setId(attachment.getId());
                            attDto.setDisplayFileName(attachment.getDisplayName());
                            attDto.setInternalFileName(attachment.getInternalPath());
                            attachmentListDto.getAwAttachments().add(attDto);
                        }
                        actionDto.setAttachmentList(attachmentListDto);
                    }
                    actionListDto.getAwActions().add(actionDto);
                }
            }
            result.setActionList(actionListDto);

            readContactResponse.setReadContactResult(result);
            body.setReadContactResponse(readContactResponse);
            responseEnvelope.setBody(body);

            Marshaller marshaller = RESPONSE_CONTEXT.createMarshaller();
            marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new MyNamespacePrefixMapper());
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.FALSE);
            try {
                marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new MyNamespacePrefixMapper());
            } catch (PropertyException e) {
                marshaller.setProperty("org.glassfish.jaxb.namespacePrefixMapper", new MyNamespacePrefixMapper());
            }

            StringWriter writer = new StringWriter();
            marshaller.marshal(responseEnvelope, writer);

            System.out.println("SOAP RESPONSE:");
            System.out.println(writer.toString());

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_XML)
                    .body(writer.toString());

        } catch (Exception e) {
            logger.error("Critical Failure while processing legacy SOAP action mapping: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("<error>SOAP Processing Exception: " + e.getMessage() + "</error>");
        }
    }

    @PostMapping(value = "/SendToChecker")
    public ResponseEntity<String> sendToChecker(@RequestBody String rawXmlRequestBody) {
        try {
            JAXBContext requestContext = JAXBContext.newInstance(SoapSendToCheckerRequestEnvelope.class);
            SoapSendToCheckerRequestEnvelope requestEnvelope = (SoapSendToCheckerRequestEnvelope)
                    requestContext.createUnmarshaller().unmarshal(new StringReader(rawXmlRequestBody));

            if (requestEnvelope.getBody() == null || requestEnvelope.getBody().getSendToChecker() == null
                    || requestEnvelope.getBody().getSendToChecker().getObjMail() == null) {
                return soapFault(HttpStatus.BAD_REQUEST, "Invalid SOAP request: missing SendToChecker objMail payload");
            }

            System.out.println(requestEnvelope.getBody());
            makerTransferStatusService.saveMakerTransferDetails(
                    requestEnvelope.getBody().getSendToChecker().getObjMail());
            SendToCheckerResult result = new SendToCheckerResult();
            result.setMailId(requestEnvelope.getBody().getSendToChecker().getObjMail().getMailId());
            result.setMessage("Maker transfer details saved successfully");

            SoapSendToCheckerResponseEnvelope responseEnvelope = new SoapSendToCheckerResponseEnvelope();
            responseEnvelope.getBody().setSendToCheckerResponse(result);
            Marshaller marshaller = RESPONSE_CONTEXT.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.FALSE);
            StringWriter writer = new StringWriter();
            marshaller.marshal(responseEnvelope, writer);
            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_XML)
                    .body(writer.toString());
        } catch (JAXBException exception) {
            logger.error("Invalid SendToChecker SOAP request", exception);
            return soapFault(HttpStatus.BAD_REQUEST, "Invalid SendToChecker SOAP request");
        } catch (IllegalArgumentException exception) {
            logger.warn("Invalid SendToChecker input: {}", exception.getMessage());
            return soapFault(HttpStatus.BAD_REQUEST, exception.getMessage());
        } catch (Exception exception) {
            logger.error("Failure while processing SendToChecker SOAP request", exception);
            return soapFault(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to save maker transfer details");
        }
    }

    private ResponseEntity<String> soapFault(HttpStatus status, String message) {
        String safeMessage = message == null ? "Invalid SOAP request" : message;
        return ResponseEntity.status(status).contentType(MediaType.TEXT_XML).body(
                "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
                        + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>"
                        + "<soap:Fault><faultcode>soap:Client</faultcode><faultstring>"
                        + escapeXml(safeMessage)
                        + "</faultstring></soap:Fault></soap:Body></soap:Envelope>");
    }

    private String escapeXml(String value) {
        return value.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&apos;");
    }

    @PostMapping(value = "/GetAllClosedReasonCodes")
    public ResponseEntity<String> getAllClosedReasonCodes(@RequestBody String rawXmlRequestBody) {
        try {
            JAXBContext requestContext = JAXBContext.newInstance(SoapRequestEnvelope.class);
            Unmarshaller unmarshaller = requestContext.createUnmarshaller();
            SoapRequestEnvelope requestEnvelope = (SoapRequestEnvelope) unmarshaller.unmarshal(new StringReader(rawXmlRequestBody));

            if (requestEnvelope.getBody() == null || requestEnvelope.getBody().getGetAllClosedReasonCodes() == null) {
                logger.warn("Incoming SOAP request is missing the GetAllClosedReasonCodes payload.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("<error>Invalid SOAP request: missing GetAllClosedReasonCodes element</error>");
            }

            String sessionKey = requestEnvelope.getBody().getGetAllClosedReasonCodes().getSessionKey();
            logger.info("Received GetAllClosedReasonCodes SOAP request. sessionKey={}", sessionKey);

            List<ClosedReason> closedReasons = closedReasonRepository.findAll();
            GetAllClosedReasonCodesResponse response = new GetAllClosedReasonCodesResponse();
            GetAllClosedReasonCodesResult result = new GetAllClosedReasonCodesResult();

            for (ClosedReason closedReason : closedReasons) {
                if (closedReason == null) {
                    continue;
                }
                AWClosedReasonCode code = new AWClosedReasonCode();
                code.setName(closedReason.getName());
                code.setNumericValue(closedReason.getOldCodeMappingID());
                result.getAwClosedReasonCodes().add(code);
            }

            response.setGetAllClosedReasonCodesResult(result);
            SoapResponseEnvelope responseEnvelope = new SoapResponseEnvelope();
            SoapResponseBody body = new SoapResponseBody();
            body.setGetAllClosedReasonCodesResponse(response);
            responseEnvelope.setBody(body);

            Marshaller marshaller = RESPONSE_CONTEXT.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.FALSE);
            try {
                marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new MyNamespacePrefixMapper());
            } catch (PropertyException e) {
                marshaller.setProperty("org.glassfish.jaxb.namespacePrefixMapper", new MyNamespacePrefixMapper());
            }

            StringWriter writer = new StringWriter();
            marshaller.marshal(responseEnvelope, writer);

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_XML)
                    .body(writer.toString());
        } catch (Exception e) {
            logger.error("Critical Failure while processing GetAllClosedReasonCodes SOAP action: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("<error>SOAP Processing Exception: " + e.getMessage() + "</error>");
        }
    }

    @PostMapping(value = "/GetHistoryFromAACC")
    public ResponseEntity<String> getHistoryFromAACC(@RequestBody String rawXmlRequestBody) {
        try {
            JAXBContext requestContext = JAXBContext.newInstance(SoapHistoryRequestEnvelope.class);
            Unmarshaller unmarshaller = requestContext.createUnmarshaller();
            SoapHistoryRequestEnvelope requestEnvelope = (SoapHistoryRequestEnvelope) unmarshaller.unmarshal(new StringReader(rawXmlRequestBody));

            if (requestEnvelope.getBody() == null || requestEnvelope.getBody().getGetHistoryFromAACC() == null) {
                logger.warn("Incoming SOAP request is missing the GetHistoryFromAACC payload.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("<error>Invalid SOAP request: missing GetHistoryFromAACC element</error>");
            }

            GetHistoryFromAACCRequest request = requestEnvelope.getBody().getGetHistoryFromAACC();
            String searchType = request.getSearchType();
            String searchValue = request.getSearchValue();
            logger.info("Received GetHistoryFromAACC SOAP request. searchType={}, searchValue={}", searchType, searchValue);

            List<Email> emails = emailRepository.findAll();
            if (searchType != null && searchValue != null) {
                String upperType = searchType.toUpperCase();
                String normalizedValue = searchValue.trim();

                if ("STATUS".equals(upperType)) {
                    if ("NEW".equalsIgnoreCase(normalizedValue)) {
                        emails = filterEmails(emails, e -> "NEW".equalsIgnoreCase(e.getStatus()));
                    } else {
                        emails = filterEmails(emails, e -> e.getStatus() == null || !"NEW".equalsIgnoreCase(e.getStatus()));
                    }
                } else if ("TOEMAIL".equals(upperType)) {
                    emails = filterEmails(emails, e -> containsIgnoreCase(e.getMailTo(), normalizedValue) || containsIgnoreCase(e.getMailFrom(), normalizedValue));
                } else if ("SUBJECT".equals(upperType)) {
                    emails = filterEmails(emails, e -> containsIgnoreCase(e.getOriginalSubject(), normalizedValue));
                } else if ("AGENTID".equals(upperType)) {
                    String[] parts = normalizedValue.split("\\|");
                    if (parts.length > 0) {
                        try {
                            Long agentId = Long.parseLong(parts[0]);
                            emails = filterEmails(emails, e -> e.getAgentId() != null && e.getAgentId().equals(agentId));
                        } catch (NumberFormatException ignored) {
                            emails = Collections.emptyList();
                        }
                    }
                } else if ("ID".equals(upperType)) {
                    try {
                        Long idValue = Long.parseLong(normalizedValue);
                        emails = filterEmails(emails, e -> e.getId() != null && e.getId().equals(idValue));
                    } catch (NumberFormatException ignored) {
                        emails = Collections.emptyList();
                    }
                } else if ("GTID".equals(upperType)) {
                    try {
                        Long idValue = Long.parseLong(normalizedValue);
                        Email baseEmail = emailRepository.findById(idValue).orElse(null);
                        if (baseEmail != null && baseEmail.getCustomerId() != null) {
                            Long customerId = baseEmail.getCustomerId();
                            emails = filterEmails(emails, e -> e.getId() != null && e.getCustomerId() != null && e.getId() >= idValue && e.getCustomerId().equals(customerId));
                        } else {
                            emails = Collections.emptyList();
                        }
                    } catch (NumberFormatException ignored) {
                        emails = Collections.emptyList();
                    }
                } else if ("LTID".equals(upperType)) {
                    try {
                        Long idValue = Long.parseLong(normalizedValue);
                        Email baseEmail = emailRepository.findById(idValue).orElse(null);
                        if (baseEmail != null && baseEmail.getCustomerId() != null) {
                            Long customerId = baseEmail.getCustomerId();
                            emails = filterEmails(emails, e -> e.getId() != null && e.getCustomerId() != null && e.getId() <= idValue && e.getCustomerId().equals(customerId));
                        } else {
                            emails = Collections.emptyList();
                        }
                    } catch (NumberFormatException ignored) {
                        emails = Collections.emptyList();
                    }
                } else {
                    emails = filterEmails(emails, e -> containsIgnoreCase(e.getMailFrom(), normalizedValue));
                }
            }

            GetHistoryFromAACCResponse response = new GetHistoryFromAACCResponse();
            GetHistoryFromAACCResult result = new GetHistoryFromAACCResult();

            for (Email email : emails) {
                MailHistory history = new MailHistory();
                history.setContactId(email.getContactId() == null ? "" : email.getContactId().toString());
                history.setCreatedTime(email.getReceivedDate() == null ? "" : email.getReceivedDate().toString());
                history.setSubject(email.getSubject());
                history.setMailFrom(email.getMailFrom());
                history.setMailTo(email.getMailTo());
                history.setMailCC(email.getMailCc());
                history.setSkillSet(email.getSkillsetName());
                history.setClosedReason(email.getStatus());
                history.setStatus(email.getStatus());
                history.setMessage("Valid Input");
                result.getMailHistory().add(history);
            }

            response.setGetHistoryFromAACCResult(result);
            SoapHistoryResponseEnvelope responseEnvelope = new SoapHistoryResponseEnvelope();
            SoapHistoryResponseBody body = new SoapHistoryResponseBody();
            body.setGetHistoryFromAACCResponse(response);
            responseEnvelope.setBody(body);

            Marshaller marshaller = RESPONSE_CONTEXT.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.FALSE);
            try {
                marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new MyNamespacePrefixMapper());
            } catch (PropertyException e) {
                marshaller.setProperty("org.glassfish.jaxb.namespacePrefixMapper", new MyNamespacePrefixMapper());
            }

            StringWriter writer = new StringWriter();
            marshaller.marshal(responseEnvelope, writer);

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_XML)
                    .body(writer.toString());
        } catch (Exception e) {
            logger.error("Critical Failure while processing GetHistoryFromAACC SOAP action: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("<error>SOAP Processing Exception: " + e.getMessage() + "</error>");
        }
    }

    private boolean containsIgnoreCase(String source, String search) {
        return source != null && search != null && source.toLowerCase().contains(search.toLowerCase());
    }

    private List<Email> filterEmails(List<Email> emails, java.util.function.Predicate<Email> predicate) {
        if (emails == null || emails.isEmpty()) {
            return Collections.emptyList();
        }
        List<Email> result = new java.util.ArrayList<>();
        for (Email email : emails) {
            if (predicate.test(email)) {
                result.add(email);
            }
        }
        return result;
    }

    @PostMapping(value = "/getContact")
    public ResponseEntity<String> getContact(@RequestBody String rawXmlRequestBody) throws JAXBException {
        try {
            JAXBContext requestContext = JAXBContext.newInstance(SoapContactRequestEnvelope.class);
            Unmarshaller unmarshaller = requestContext.createUnmarshaller();
            SoapContactRequestEnvelope requestEnvelope = (SoapContactRequestEnvelope) unmarshaller.unmarshal(new StringReader(rawXmlRequestBody));
            Long agentId = requestEnvelope.getBody().getGetContact().getStrAgentID();
            logger.info("Received SOAP request to get Contact ID for agentID: {}", agentId);

            UserMaster user = userMasterRepository.findByAgentId(agentId);
            if (user == null) {
                logger.error("User not found for agentID: {}", agentId);
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body("User not found for agentID: " + agentId);
            }
            List<Email> emails = emailRepository.findTopPendingEmailsBySkill(user.getSkillId());
            logger.info("Found {} pending emails for skill ID: {}", emails.size(), user.getSkillId());
            long contactId = 0;
            if (emails != null && !emails.isEmpty()) {
                Email email = emails.get(0);
                contactId = email.getContactId();
                email.setAssigned(true);
                email.setAssignedTime(LocalDateTime.now());
                email.setAgentId(user.getAgentId());
                email.setAgentFirstName(user.getFirstName());
                email.setAgentLastName(user.getLastName());
                emailRepository.save(email);
            }


            SoapContactResponseEnvelope responseEnvelope = new SoapContactResponseEnvelope();
            SoapContactResponseBody body = new SoapContactResponseBody();
            GetContactResponse getContactResponse = new GetContactResponse();
            GetContactResult result = new GetContactResult();

            result.setContactId(contactId);
            if(contactId!=0){
                result.setMessage("Valid Input");
            }else{
                result.setMessage("Invalid Input");
            }
            getContactResponse.setGetContactResult(result);
            body.setGetContactResponse(getContactResponse);
            responseEnvelope.setBody(body);

            Marshaller marshaller = RESPONSE_CONTEXT.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.FALSE);

            try {
                marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new MyNamespacePrefixMapper());
            } catch (PropertyException e) {
                marshaller.setProperty("org.glassfish.jaxb.namespacePrefixMapper", new MyNamespacePrefixMapper());
            }

            StringWriter writer = new StringWriter();
            marshaller.marshal(responseEnvelope, writer);

            logger.debug("Generated SOAP response for Agent ID {}: {}", agentId, writer.toString());

            return ResponseEntity.ok()
                    .contentType(MediaType.TEXT_XML)
                    .body(writer.toString());
        } catch (NumberFormatException e) {
            logger.error("Failed to parse Agent ID string format: ", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body("<error>Invalid Agent ID format</error>");
        } catch (Exception e) {
            logger.error("Critical Failure while processing legacy GetContact SOAP action mapping: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("<error>SOAP Processing Exception: " + e.getMessage() + "</error>");
        }
    }

    @PostMapping(value = "/CloseContact")
    public ResponseEntity<String> closeContact(@RequestBody String rawXmlRequestBody) {
        try {
            JAXBContext requestContext = JAXBContext.newInstance(SoapRequestEnvelope.class);
            Unmarshaller unmarshaller = requestContext.createUnmarshaller();
            SoapRequestEnvelope requestEnvelope = (SoapRequestEnvelope) unmarshaller.unmarshal(new StringReader(rawXmlRequestBody));

            if (requestEnvelope.getBody() == null || requestEnvelope.getBody().getCloseContact() == null) {
                logger.warn("Incoming SOAP request is missing the CloseContact payload.");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                        .body("<error>Invalid SOAP request: missing CloseContact element</error>");
            }

            CloseContactRequest request = requestEnvelope.getBody().getCloseContact();
            String idStr = request.getId();
            String closureText = request.getClosureText();
            Long closedReasonCodeValue = request.getClosedReasonCodeValue();
            Boolean closedReasonCodeSpecified = request.getClosedReasonCodeSpecified();
            String sessionKey = request.getSessionKey();

            long contactId = 0L;
            try {
                contactId = Long.parseUnsignedLong(idStr);
            } catch (Exception ex) {
                logger.error("Invalid contact id format: {}", idStr);
                CloseContactResponse response = new CloseContactResponse();
                CloseContactResult result = new CloseContactResult(0L);
                response.setCloseContactResult(result);
                SoapResponseEnvelope responseEnvelope = new SoapResponseEnvelope();
                SoapResponseBody body = new SoapResponseBody();
                body.setCloseContactResponse(response);
                responseEnvelope.setBody(body);

                Marshaller marshaller = RESPONSE_CONTEXT.createMarshaller();
                marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
                marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.FALSE);
                try {
                    marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new MyNamespacePrefixMapper());
                } catch (PropertyException e) {
                    marshaller.setProperty("org.glassfish.jaxb.namespacePrefixMapper", new MyNamespacePrefixMapper());
                }

                StringWriter writer = new StringWriter();
                marshaller.marshal(responseEnvelope, writer);

                return ResponseEntity.badRequest().contentType(MediaType.TEXT_XML).body(writer.toString());
            }

            // create action
            ContactAction action = new ContactAction();
            action.setActionId(System.currentTimeMillis() % 10000000L + 1000000L);
            action.setContactId(contactId);
            action.setSubject("CloseContact");
            action.setTextContent(closureText);
            action.setTextHtml(closureText);
            action.setSource("CloseContact");
            action.setComment(closureText);
            action.setCreationTime(System.currentTimeMillis());
            if (closedReasonCodeSpecified != null && closedReasonCodeSpecified && closedReasonCodeValue != null) {
                action.setClosedReasonNumericValue(closedReasonCodeValue.intValue());
            }
            contactActionRepository.save(action);

            // update emails with this contactId -> set status Closed
            List<Email> emails = emailRepository.findByContactId(contactId);
            for (Email email : emails) {
                email.setStatus("Closed");
                emailRepository.save(email);
            }

            long returned = action.getActionId();

            CloseContactResponse response = new CloseContactResponse();
            CloseContactResult result = new CloseContactResult(returned);
            response.setCloseContactResult(result);
            SoapResponseEnvelope responseEnvelope = new SoapResponseEnvelope();
            SoapResponseBody body = new SoapResponseBody();
            body.setCloseContactResponse(response);
            responseEnvelope.setBody(body);

            Marshaller marshaller = RESPONSE_CONTEXT.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
            marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.FALSE);
            try {
                marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper", new MyNamespacePrefixMapper());
            } catch (PropertyException e) {
                marshaller.setProperty("org.glassfish.jaxb.namespacePrefixMapper", new MyNamespacePrefixMapper());
            }

            StringWriter writer = new StringWriter();
            marshaller.marshal(responseEnvelope, writer);

            return ResponseEntity.ok().contentType(MediaType.TEXT_XML).body(writer.toString());
        } catch (Exception e) {
            logger.error("Critical Failure while processing CloseContact SOAP action: ", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("<error>SOAP Processing Exception: " + e.getMessage() + "</error>");
        }
    }

}