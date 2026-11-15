package com.example.mail.config;

import com.example.mail.repository.AgentSessionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SessionAuthorizationInterceptorTest {

    private final AgentSessionRepository repository = mock(AgentSessionRepository.class);
    private final SessionAuthorizationInterceptor interceptor = new SessionAuthorizationInterceptor(repository);

    @Test
    void rejectsMissingAuthorizationHeader() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(new MockHttpServletRequest(), response, new Object());

        assertFalse(allowed);
        assertEquals(401, response.getStatus());
        verify(repository, never()).touchSessionIfStatusMatches(null, "Online", null);
    }

    @Test
    void rejectsSessionThatIsNotOnline() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "session-key");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = interceptor.preHandle(request, response, new Object());

        assertFalse(allowed);
        assertEquals(401, response.getStatus());
        verify(repository).touchSessionIfStatusMatches(eq("session-key"), eq("Online"), any(LocalDateTime.class));
    }

    @Test
    void allowsOnlineSession() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "session-key");
        when(repository.touchSessionIfStatusMatches(eq("session-key"), eq("Online"), any(LocalDateTime.class)))
            .thenReturn(1);

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        verify(repository).touchSessionIfStatusMatches(eq("session-key"), eq("Online"), any(LocalDateTime.class));
    }

    @Test
    void allowsCorsPreflightWithoutSessionKey() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/email-service/sbi/SendMail");

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        verify(repository, never()).touchSessionIfStatusMatches(null, "Online", null);
    }
}