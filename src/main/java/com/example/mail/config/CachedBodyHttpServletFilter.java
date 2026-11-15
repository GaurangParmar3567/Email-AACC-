package com.example.mail.config;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ReadListener;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

/**
 * Filter that caches incoming request bodies for endpoints under /email-service/sbi.
 * Allows the body stream to be read multiple times: once by SessionAuthorizationInterceptor
 * to extract and validate session IDs from XML payloads, and once by downstream controllers.
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class CachedBodyHttpServletFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String uri = request.getRequestURI();
        String method = request.getMethod();

        if (uri != null && uri.contains("/email-service/sbi")
                && !"OPTIONS".equalsIgnoreCase(method)) {
            CachedBodyHttpServletRequestWrapper wrappedRequest = new CachedBodyHttpServletRequestWrapper(request);
            filterChain.doFilter(wrappedRequest, response);
        } else {
            filterChain.doFilter(request, response);
        }
    }

    public static class CachedBodyHttpServletRequestWrapper extends HttpServletRequestWrapper {

        private final byte[] cachedBody;
        private final Charset characterEncoding;

        public CachedBodyHttpServletRequestWrapper(HttpServletRequest request) throws IOException {
            super(request);
            this.cachedBody = StreamUtils.copyToByteArray(request.getInputStream());
            String enc = request.getCharacterEncoding();
            this.characterEncoding = (enc != null && !enc.trim().isEmpty())
                    ? Charset.forName(enc)
                    : StandardCharsets.UTF_8;
        }

        public byte[] getCachedBody() {
            return cachedBody;
        }

        public String getBodyAsString() {
            return new String(cachedBody, characterEncoding);
        }

        @Override
        public ServletInputStream getInputStream() {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(cachedBody);
            return new ServletInputStream() {
                @Override
                public boolean isFinished() {
                    return byteArrayInputStream.available() == 0;
                }

                @Override
                public boolean isReady() {
                    return true;
                }

                @Override
                public void setReadListener(ReadListener readListener) {
                }

                @Override
                public int read() {
                    return byteArrayInputStream.read();
                }
            };
        }

        @Override
        public BufferedReader getReader() {
            return new BufferedReader(new InputStreamReader(getInputStream(), characterEncoding));
        }
    }
}
