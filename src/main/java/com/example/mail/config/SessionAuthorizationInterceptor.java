package com.example.mail.config;

import com.example.mail.repository.AgentSessionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class SessionAuthorizationInterceptor implements HandlerInterceptor {

    private static final Logger log = LoggerFactory.getLogger("MAIL_SERVICES_AUTH_LOGGER");

    private static final String UNAUTHORIZED_SOAP_FAULT = "<?xml version=\"1.0\" encoding=\"utf-8\"?>"
            + "<soap:Envelope xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body>"
            + "<soap:Fault><faultcode>soap:Client</faultcode>"
            + "<faultstring>Unauthorized: valid session key required</faultstring>"
            + "</soap:Fault></soap:Body></soap:Envelope>";

    private final AgentSessionRepository agentSessionRepository;

    public SessionAuthorizationInterceptor(AgentSessionRepository agentSessionRepository) {
        this.agentSessionRepository = agentSessionRepository;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String sessionKey = null;

        // 1. First, check if sessionId/sessionKey is passed in the API's request payload (body)
        sessionKey = extractSessionKeyFromBody(request);
        if (sessionKey != null && !sessionKey.trim().isEmpty()) {
            log.debug("preHandle() - Found sessionId in API request payload for URI: {}", request.getRequestURI());
        }

        // 2. If not passed in the request body, check if the header has Authorization (which also contains sessionId)
        if (sessionKey == null || sessionKey.trim().isEmpty()) {
            sessionKey = request.getHeader("Authorization");
            if (sessionKey != null && !sessionKey.trim().isEmpty()) {
                sessionKey = sessionKey.trim();
                if (sessionKey.startsWith("Bearer ") || sessionKey.startsWith("bearer ")) {
                    sessionKey = sessionKey.substring(7).trim();
                }
                log.debug("preHandle() - Found sessionId in Authorization header for URI: {}", request.getRequestURI());
            }
        }

        // 3. Optional fallback to other common header names or query parameters
        if (sessionKey == null || sessionKey.trim().isEmpty()) {
            sessionKey = request.getHeader("SessionKey");
            if (sessionKey == null || sessionKey.trim().isEmpty()) {
                sessionKey = request.getHeader("SessionId");
            }
            if (sessionKey == null || sessionKey.trim().isEmpty()) {
                sessionKey = request.getParameter("sessionId");
            }
            if (sessionKey == null || sessionKey.trim().isEmpty()) {
                sessionKey = request.getParameter("sessionKey");
            }
        }

        // 4. Validate sessionId against the single AgentSession table
        if (sessionKey != null && !sessionKey.trim().isEmpty()) {
            int updatedSessions = agentSessionRepository.touchSessionIfStatusMatches(
                    sessionKey.trim(), "Online", LocalDateTime.now());
            if (updatedSessions > 0) {
                request.setAttribute("SESSION_ID", sessionKey.trim());
                return true;
            }
            log.warn("preHandle() - Session '{}' rejected (not found or not Online) for URI: {}",
                    sessionKey, request.getRequestURI());
        } else {
            log.warn("preHandle() - No sessionId found in API request body or Authorization header for URI: {}",
                    request.getRequestURI());
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.TEXT_XML_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(UNAUTHORIZED_SOAP_FAULT);
        return false;
    }

    private String extractSessionKeyFromBody(HttpServletRequest request) {
        try {
            if (request instanceof CachedBodyHttpServletFilter.CachedBodyHttpServletRequestWrapper) {
                String body = ((CachedBodyHttpServletFilter.CachedBodyHttpServletRequestWrapper) request).getBodyAsString();
                if (body != null && !body.trim().isEmpty()) {
                    return extractTagValue(body, "sessionId", "sessionKey", "strSessionKey", "agentSessionId");
                }
            }
        } catch (Exception ex) {
            log.debug("extractSessionKeyFromBody() - Could not inspect body: {}", ex.getMessage());
        }
        return null;
    }

    private String extractTagValue(String xml, String... tagNames) {
        if (xml == null || xml.isEmpty()) {
            return null;
        }
        for (String tagName : tagNames) {
            Pattern pattern = Pattern.compile(
                    "<(?:[a-zA-Z0-9_]+:)?" + Pattern.quote(tagName) + "[^>]*>([^<]+)</(?:[a-zA-Z0-9_]+:)?" + Pattern.quote(tagName) + ">",
                    Pattern.CASE_INSENSITIVE
            );
            Matcher matcher = pattern.matcher(xml);
            if (matcher.find()) {
                String val = matcher.group(1);
                if (val != null && !val.trim().isEmpty()) {
                    return val.trim();
                }
            }
        }
        return null;
    }
}
