package com.example.mail.config;

import com.example.mail.repository.AgentSessionRepository;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@Component
public class SessionAuthorizationInterceptor implements HandlerInterceptor {

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

        String sessionKey = request.getHeader("Authorization");
        if (sessionKey != null && agentSessionRepository
                .findByTokenIdAndStatus(sessionKey.trim(), "Online").isPresent()) {
            return true;
        }

        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.TEXT_XML_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(UNAUTHORIZED_SOAP_FAULT);
        return false;
    }
}