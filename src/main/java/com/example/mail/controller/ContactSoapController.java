package com.example.mail.controller;

import com.example.mail.dto.MyNamespacePrefixMapper;
import com.example.mail.dto.soapnew.*;
import com.example.mail.model.Attachment;
import com.example.mail.model.ContactAction;
import com.example.mail.model.Email;
import com.example.mail.repository.EmailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

@RestController
@RequestMapping(value = "/webservices",
        consumes = MediaType.APPLICATION_XML_VALUE,
        produces = MediaType.APPLICATION_XML_VALUE)
public class ContactSoapController {

    private final Logger logger = LoggerFactory.getLogger(ContactSoapController.class);
    private final EmailRepository emailRepository;
    private static final JAXBContext RESPONSE_CONTEXT;

    static {
        try {
            RESPONSE_CONTEXT = JAXBContext.newInstance(SoapResponseEnvelope.class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize JAXB context", e);
        }
    }

    public ContactSoapController(EmailRepository emailRepository) {
        this.emailRepository = emailRepository;
    }

    @PostMapping(value = "/CCMMAgentWebservices")
    public ResponseEntity<String> readContact(@RequestBody String rawXmlRequestBody) {
        try {
            JAXBContext requestContext = JAXBContext.newInstance(SoapRequestEnvelope.class);
            Unmarshaller unmarshaller = requestContext.createUnmarshaller();
            SoapRequestEnvelope requestEnvelope = (SoapRequestEnvelope) unmarshaller.unmarshal(new StringReader(rawXmlRequestBody));

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

                for (ContactAction action : threadEmail.getContactActions()) {
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
}