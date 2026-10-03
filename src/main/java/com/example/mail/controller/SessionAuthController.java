package com.example.mail.controller;

import com.example.mail.dto.soap.sessionauth.*;
import com.example.mail.model.AgentSession;
import com.example.mail.model.UserMaster;
import com.example.mail.repository.UserMasterRepo;
import com.example.mail.service.AgentSessionService;
import com.example.mail.util.XmlUtils;
import com.example.mail.util.xml.MyNamespacePrefixMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
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
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    @PostMapping("/AgentDesktopLogin")
    public ResponseEntity<String> setAgentLoginDetails(@RequestBody String rawXmlRequestBody) {
        logger.info("setAgentLoginDetails() - Received setAgentLoginDetails SOAP/XML request");
        try {
            String username = null;
            String password = null;
            Integer agentId = null;
            String ipAddress = null;
            String deviceName = null;
            String tokenId = null;

            // 1. Demarshalling via JAXB (SOAP Envelope)
            try {
                SoapAgentLoginRequestEnvelope envelope = unmarshal(rawXmlRequestBody, SoapAgentLoginRequestEnvelope.class);
                if (envelope != null && envelope.getBody() != null && envelope.getBody().getAgentDesktopLogin() != null) {
                    AgentDesktopLogin login = envelope.getBody().getAgentDesktopLogin();
                    username = login.getUsername();
                    password = login.getPassword();
                }
            } catch (Exception ex) {
                logger.debug("setAgentLoginDetails() - JAXB demarshalling as SoapAgentLoginRequestEnvelope failed, trying direct unmarshal: {}", ex.getMessage());
            }

            // Fallback demarshalling for direct AgentDesktopLogin element
            if (username == null || username.trim().isEmpty()) {
                try {
                    AgentDesktopLogin directLogin = unmarshal(rawXmlRequestBody, AgentDesktopLogin.class);
                    if (directLogin != null) {
                        username = directLogin.getUsername();
                        password = directLogin.getPassword();
                    }
                } catch (Exception ignored) {
                }
            }

            // Fallback demarshalling for AgentLoginRequest
            if (username == null || username.trim().isEmpty()) {
                try {
                    AgentLoginRequest request = unmarshal(rawXmlRequestBody, AgentLoginRequest.class);
                    if (request != null) {
                        username = request.getAgentLoginId();
                        password = request.getAgentLoginPwd();
                        agentId = request.getAgentId();
                        ipAddress = request.getIpAddress();
                        deviceName = request.getDeviceName();
                        tokenId = request.getTokenId();
                    }
                } catch (Exception ignored) {
                }
            }

            // Fallback tag extraction if needed
            if (username == null || username.trim().isEmpty()) {
                username = extractXmlTag(rawXmlRequestBody, "username");
                if (username == null || username.trim().isEmpty()) {
                    username = extractXmlTag(rawXmlRequestBody, "agentLoginId");
                }
            }
            if (password == null || password.trim().isEmpty()) {
                password = extractXmlTag(rawXmlRequestBody, "password");
                if (password == null || password.trim().isEmpty()) {
                    password = extractXmlTag(rawXmlRequestBody, "agentLoginPwd");
                }
            }
            if (agentId == null) {
                String aIdStr = extractXmlTag(rawXmlRequestBody, "agentId");
                if (aIdStr != null && !aIdStr.isEmpty()) {
                    try {
                        agentId = Integer.parseInt(aIdStr);
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
            if (ipAddress == null) {
                ipAddress = extractXmlTag(rawXmlRequestBody, "ipAddress");
            }
            if (deviceName == null) {
                deviceName = extractXmlTag(rawXmlRequestBody, "deviceName");
            }
            if (tokenId == null) {
                tokenId = extractXmlTag(rawXmlRequestBody, "tokenId");
            }

            if (username == null || username.trim().isEmpty()) {
                logger.warn("setAgentLoginDetails() - Rejected setAgentLoginDetails request: missing username/agentLoginId");
                return buildSoapFault(HttpStatus.INTERNAL_SERVER_ERROR, "Username/AgentLoginId is required");
            }

            String loginId = username.trim();

            if (agentId == null) {
                try {
                    long numericId = Long.parseLong(loginId);
                    UserMaster userMaster = userMasterRepo.findByAgentId(numericId);
                    if (userMaster == null) {
                        userMaster = userMasterRepo.findById(numericId).orElse(null);
                    }
                    if (userMaster != null && userMaster.getAgentId() != null) {
                        agentId = userMaster.getAgentId().intValue();
                    } else {
                        agentId = (int) numericId;
                    }
                } catch (NumberFormatException ex) {
                    agentId = 0;
                }
            }

            if (agentId != null) {
                UserMaster userMaster = userMasterRepo.findByAgentId(agentId.longValue());
                if (userMaster == null) {
                    userMaster = userMasterRepo.findById(agentId.longValue()).orElse(null);
                }
                if (userMaster != null) {
                    logger.info("setAgentLoginDetails() - Found UserMaster for agentId={}: {} {}",
                            agentId, userMaster.getFirstName(), userMaster.getLastName());
                } else {
                    logger.warn("setAgentLoginDetails() - UserMaster record not found for agentId={}, proceeding with session creation", agentId);
                }
            }

            logger.info("setAgentLoginDetails() - Processing setAgentLoginDetails for agentLoginId={}, agentId={}, deviceName={}",
                    loginId, agentId, deviceName);

            AgentSession session = agentSessionService.createSession(
                    loginId,
                    agentId,
                    ipAddress,
                    deviceName,
                    tokenId
            );

            String sessionKey = session.getTokenId() != null && !session.getTokenId().isEmpty()
                    ? session.getTokenId()
                    : String.valueOf(session.getSessionId());

            logger.info("setAgentLoginDetails() - setAgentLoginDetails completed successfully for agentLoginId={}, sessionKey={}",
                    loginId, sessionKey);

            // JAXB Marshalling of response envelope
            SoapAgentLoginResponseEnvelope responseEnvelope = new SoapAgentLoginResponseEnvelope(
                    new SoapAgentLoginResponseBody(
                            new AgentDesktopLoginResponse(sessionKey)
                    )
            );
            String responseXml = marshalSoap(responseEnvelope);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/xml; charset=utf-8"))
                    .body(responseXml);

        } catch (Exception exception) {
            logger.error("setAgentLoginDetails() - Failure while processing setAgentLoginDetails SOAP/XML request", exception);
            return buildSoapFault(HttpStatus.INTERNAL_SERVER_ERROR, "Login processing exception: " + exception.getMessage());
        }
    }

    @PostMapping({"/logout", "/AgentDesktopLogoff"})
    public ResponseEntity<String> logout(@RequestBody String rawXmlRequestBody) {
        logger.info("logout() - Received logout SOAP/XML request");
        try {
            String identifier = null;

            // 1. Demarshalling via JAXB (SOAP Envelope)
            try {
                SoapAgentLogoffRequestEnvelope envelope = unmarshal(rawXmlRequestBody, SoapAgentLogoffRequestEnvelope.class);
                if (envelope != null && envelope.getBody() != null && envelope.getBody().getAgentDesktopLogoff() != null) {
                    identifier = envelope.getBody().getAgentDesktopLogoff().getUsername();
                }
            } catch (Exception ex) {
                logger.debug("logout() - JAXB demarshalling as SoapAgentLogoffRequestEnvelope failed, trying direct unmarshal: {}", ex.getMessage());
            }

            // Fallback demarshalling for direct AgentDesktopLogoff element
            if (identifier == null || identifier.trim().isEmpty()) {
                try {
                    AgentDesktopLogoff directLogoff = unmarshal(rawXmlRequestBody, AgentDesktopLogoff.class);
                    if (directLogoff != null) {
                        identifier = directLogoff.getUsername();
                    }
                } catch (Exception ignored) {
                }
            }

            // Fallback demarshalling for AgentLogoutRequest
            if (identifier == null || identifier.trim().isEmpty()) {
                try {
                    AgentLogoutRequest request = unmarshal(rawXmlRequestBody, AgentLogoutRequest.class);
                    if (request != null) {
                        identifier = request.getUsername() != null && !request.getUsername().isEmpty()
                                ? request.getUsername()
                                : request.getAgentSessionId();
                    }
                } catch (Exception ignored) {
                }
            }

            // Fallback tag extraction
            if (identifier == null || identifier.isEmpty()) {
                identifier = extractXmlTag(rawXmlRequestBody, "username");
            }
            if (identifier == null || identifier.isEmpty()) {
                identifier = extractXmlTag(rawXmlRequestBody, "agentLoginId");
            }
            if (identifier == null || identifier.isEmpty()) {
                identifier = extractXmlTag(rawXmlRequestBody, "agentSessionId");
            }
            if (identifier == null || identifier.isEmpty()) {
                identifier = extractXmlTag(rawXmlRequestBody, "sessionID");
            }
            if (identifier == null || identifier.isEmpty()) {
                identifier = extractXmlTag(rawXmlRequestBody, "sessionId");
            }

            if (identifier != null && !identifier.isEmpty()) {
                logger.info("logout() - Processing logout for identifier={}", identifier);
                agentSessionService.logoutAgentByUsernameOrId(identifier);
                logger.info("logout() - Logout completed successfully for identifier={}", identifier);
            } else {
                logger.warn("logout() - Rejected logout request: missing username/agentSessionId");
            }

            // JAXB Marshalling of response envelope
            SoapAgentLogoffResponseEnvelope responseEnvelope = new SoapAgentLogoffResponseEnvelope(
                    new SoapAgentLogoffResponseBody(
                            new AgentDesktopLogoffResponse("true")
                    )
            );
            String responseXml = marshalSoap(responseEnvelope);

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType("text/xml; charset=utf-8"))
                    .body(responseXml);

        } catch (Exception exception) {
            logger.error("logout() - Failure while processing logout SOAP/XML request", exception);
            return buildSoapFault(HttpStatus.INTERNAL_SERVER_ERROR, "Logout processing exception: " + exception.getMessage());
        }
    }

    private String extractXmlTag(String xml, String tagName) {
        if (xml == null || tagName == null) {
            return null;
        }
        Pattern pattern = Pattern.compile(
                "<(?:[\\w\\-]+:)?" + tagName + "(?:\\s+[^>]*)?>([\\s\\S]*?)</(?:[\\w\\-]+:)?" + tagName + ">",
                Pattern.CASE_INSENSITIVE
        );
        Matcher matcher = pattern.matcher(xml);
        if (matcher.find()) {
            String value = matcher.group(1).trim();
            if (value.startsWith("<![CDATA[") && value.endsWith("]]>")) {
                value = value.substring(9, value.length() - 3).trim();
            }
            return value;
        }
        return null;
    }

    private ResponseEntity<String> buildSoapFault(HttpStatus status, String message) {
        String faultXml = "<?xml version=\"1.0\" encoding=\"utf-8\"?>\n"
                + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\">\n"
                + "  <soap:Body>\n"
                + "    <soap:Fault>\n"
                + "      <faultcode>soap:Server</faultcode>\n"
                + "      <faultstring>" + XmlUtils.escapeXml(message == null ? "Processing error" : message) + "</faultstring>\n"
                + "    </soap:Fault>\n"
                + "  </soap:Body>\n"
                + "</soap:Envelope>";

        return ResponseEntity.status(status)
                .contentType(MediaType.parseMediaType("text/xml; charset=utf-8"))
                .body(faultXml);
    }

    private <T> T unmarshal(String xml, Class<T> type) throws JAXBException {
        if (xml == null || xml.trim().isEmpty()) {
            return null;
        }
        Unmarshaller unmarshaller = JAXBContext.newInstance(type).createUnmarshaller();
        return type.cast(unmarshaller.unmarshal(new StringReader(xml)));
    }

    private String marshalSoap(Object envelope) throws JAXBException {
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
}
