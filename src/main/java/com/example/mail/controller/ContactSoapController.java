package com.example.mail.controller;

import com.example.mail.dto.MyNamespacePrefixMapper;
import com.example.mail.dto.soapContact.*;
import com.example.mail.dto.soapnew.*;
import com.example.mail.model.Attachment;
import com.example.mail.model.ContactAction;
import com.example.mail.model.Email;
import com.example.mail.model.UserMaster;
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
    private final MakerTransferStatusService makerTransferStatusService;
    private static final JAXBContext RESPONSE_CONTEXT;

    static {
        try {
            RESPONSE_CONTEXT = JAXBContext.newInstance(SoapResponseEnvelope.class, SoapContactResponseEnvelope.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize JAXB context", e);
        }
    }

    public ContactSoapController(EmailRepository emailRepository, UserMasterRepo userMasterRepository,
                                 MakerTransferStatusService makerTransferStatusService) {
        this.emailRepository = emailRepository;
        this.userMasterRepository = userMasterRepository;
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
}