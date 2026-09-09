package com.example.mail.controller;

import com.example.mail.dto.soap.sessionauth.AgentDesktopLoginResult;
import com.example.mail.dto.soap.sessionauth.AgentLoginRequest;
import com.example.mail.dto.soap.sessionauth.AgentLogoutRequest;
import com.example.mail.model.AgentSession;
import com.example.mail.service.AgentSessionService;
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
@RequestMapping(value = "/api/session-auth", consumes = MediaType.TEXT_XML_VALUE, produces = MediaType.TEXT_XML_VALUE)
@CrossOrigin(origins = "*")
public class SessionAuthController {

    private final AgentSessionService agentSessionService;

    public SessionAuthController(AgentSessionService agentSessionService) {
        this.agentSessionService = agentSessionService;
    }

    @PostMapping("/setAgentLoginDetails")
    public ResponseEntity<String> setAgentLoginDetails(@RequestBody String rawXmlRequestBody) {
        try {
            AgentLoginRequest request = unmarshal(rawXmlRequestBody, AgentLoginRequest.class);
            if (request == null || request.getAgentLoginId() == null || request.getAgentLoginId().trim().isEmpty()) {
                return xmlResponse(new AgentDesktopLoginResult(""));
            }

            AgentSession session = agentSessionService.createSession(
                    request.getAgentLoginId(),
                    request.getAgentId(),
                    request.getIpAddress(),
                    request.getDeviceName(),
                    request.getTokenId()
            );

            return xmlResponse(new AgentDesktopLoginResult(session.getTokenId()));
        } catch (JAXBException exception) {
            return xmlResponse(new AgentDesktopLoginResult(""));
        } catch (Exception exception) {
            return xmlResponse(new AgentDesktopLoginResult(""));
        }
    }

    @PostMapping("/logout")
    public ResponseEntity<String> logout(@RequestBody String rawXmlRequestBody) {
        try {
            AgentLogoutRequest request = unmarshal(rawXmlRequestBody, AgentLogoutRequest.class);
            if (request != null && request.getAgentSessionId() != null) {
                try {
                    Long sessionId = Long.parseLong(request.getAgentSessionId());
                    agentSessionService.updateLogout(sessionId);
                } catch (NumberFormatException ignored) {
                    // ignore invalid id
                }
            }
        } catch (JAXBException ignored) {
            // ignore invalid xml payload
        } catch (Exception ignored) {
            // ignore invalid payload
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
