package com.example.mail.controller;

import com.example.mail.dto.soap.sessionauth.AgentDesktopLoginResult;
import com.example.mail.dto.soap.sessionauth.AgentLoginRequest;
import com.example.mail.dto.soap.sessionauth.AgentLogoutRequest;
import com.example.mail.model.AgentSession;
import com.example.mail.model.UserMaster;
import com.example.mail.repository.UserMasterRepo;
import com.example.mail.service.AgentSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;

@RestController
@RequestMapping(value = "/email-service/sbi/session-auth", consumes = MediaType.TEXT_XML_VALUE, produces = MediaType.TEXT_XML_VALUE)
@CrossOrigin(origins = "*")
public class SessionAuthController {

    private static final Logger logger = LoggerFactory.getLogger("MAIL_SERVICES_AUTH_LOGGER");

    private final AgentSessionService agentSessionService;
    private final UserMasterRepo userMasterRepo;

    public SessionAuthController(AgentSessionService agentSessionService, UserMasterRepo userMasterRepo) {
        this.agentSessionService = agentSessionService;
        this.userMasterRepo = userMasterRepo;
    }

    @PostMapping("/setAgentLoginDetails")
    public ResponseEntity<String> setAgentLoginDetails(@RequestBody String rawXmlRequestBody) {
        logger.info("Received setAgentLoginDetails SOAP/XML request");
        try {
            AgentLoginRequest request = unmarshal(rawXmlRequestBody, AgentLoginRequest.class);
            if (request == null || request.getAgentLoginId() == null || request.getAgentLoginId().trim().isEmpty()) {
                logger.warn("Rejected setAgentLoginDetails request: missing agentLoginId");
                return xmlResponse(new AgentDesktopLoginResult("AgentLoginId is required"));
            }
            if (request.getAgentId() == null) {
                logger.warn("Rejected setAgentLoginDetails request: missing agentId for agentLoginId={}", request.getAgentLoginId());
                return xmlResponse(new AgentDesktopLoginResult("AgentId is required"));
            }

            UserMaster userMaster = userMasterRepo.findByAgentId(request.getAgentId().longValue());
            if (userMaster == null) {
                logger.warn("Rejected setAgentLoginDetails request: agentId {} not found in user_master for agentLoginId={}",
                        request.getAgentId(), request.getAgentLoginId());
                return xmlResponse(new AgentDesktopLoginResult("AgentId not found in user_master"));
            }

            logger.info("Processing setAgentLoginDetails for agentLoginId={}, agentId={}, deviceName={}",
                    request.getAgentLoginId(), request.getAgentId(), request.getDeviceName());

            AgentSession session = agentSessionService.createSession(
                    request.getAgentLoginId(),
                    request.getAgentId(),
                    request.getIpAddress(),
                    request.getDeviceName(),
                    request.getTokenId()
            );

            logger.info("setAgentLoginDetails completed successfully for agentLoginId={}, generatedToken={}",
                    request.getAgentLoginId(), session.getTokenId());
            return xmlResponse(new AgentDesktopLoginResult(session.getTokenId()));
        } catch (JAXBException exception) {
            logger.warn("Rejected invalid setAgentLoginDetails SOAP/XML request", exception);
            return xmlResponse(new AgentDesktopLoginResult(""));
        } catch (Exception exception) {
            logger.error("Failure while processing setAgentLoginDetails SOAP/XML request", exception);
            return xmlResponse(new AgentDesktopLoginResult(""));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody String rawXmlRequestBody) {
        logger.info("Received logout SOAP/XML request");
        try {
            AgentLogoutRequest request = unmarshal(rawXmlRequestBody, AgentLogoutRequest.class);
            if (request != null && request.getAgentSessionId() != null) {
                try {
                    Long sessionId = Long.parseLong(request.getAgentSessionId());
                    logger.info("Processing logout for agentSessionId={}", sessionId);
                    agentSessionService.updateLogout(sessionId);
                    logger.info("Logout completed successfully for agentSessionId={}", sessionId);
                } catch (NumberFormatException ignored) {
                    logger.warn("Rejected logout request with invalid agentSessionId={}", request.getAgentSessionId());
                }
            } else {
                logger.warn("Rejected logout request: missing agentSessionId");
            }
        } catch (JAXBException exception) {
            logger.warn("Rejected invalid logout SOAP/XML request", exception);
        } catch (Exception exception) {
            logger.error("Failure while processing logout SOAP/XML request", exception);
        }

        return xmlResponse(new AgentDesktopLoginResult(""));
    }

    private <T> T unmarshal(String xml, Class<T> type) throws JAXBException {
        Unmarshaller unmarshaller = JAXBContext.newInstance(type).createUnmarshaller();
        return type.cast(unmarshaller.unmarshal(new StringReader(xml)));
    }

    private String marshal(Object envelope) throws JAXBException {
        Marshaller marshaller = JAXBContext.newInstance(envelope.getClass()).createMarshaller();
        marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
        marshaller.setProperty(Marshaller.JAXB_FRAGMENT, Boolean.FALSE);
        StringWriter writer = new StringWriter();
        marshaller.marshal(envelope, writer);
        return writer.toString();
    }

    private ResponseEntity<String> xmlResponse(Object envelope) {
        try {
            return ResponseEntity.ok().contentType(MediaType.TEXT_XML).body(marshal(envelope));
        } catch (JAXBException exception) {
            return ResponseEntity.ok().contentType(MediaType.TEXT_XML)
                    .body("<AgentDesktopLoginResult></AgentDesktopLoginResult>");
        }
    }

}
