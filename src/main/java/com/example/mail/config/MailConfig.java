package com.example.mail.config;

import com.example.mail.model.SmtpMailCredential;
import com.example.mail.service.SmtpMailCredentialService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;

import java.util.Optional;
import java.util.Properties;

@Configuration
public class MailConfig {

    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(MailConfig.class);

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String defaultHost;

    @Value("${spring.mail.port:587}")
    private int defaultPort;

    @Value("${spring.mail.username:}")
    private String defaultUsername;

    @Value("${spring.mail.password:}")
    private String defaultPassword;

    @Value("${spring.mail.protocol:smtp}")
    private String mailProtocol;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private boolean smtpAuth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
    private boolean smtpStartTlsEnable;

    @Value("${spring.mail.properties.mail.smtp.starttls.required:true}")
    private boolean smtpStartTlsRequired;

    @Value("${spring.mail.properties.mail.smtp.ssl.trust:*}")
    private String smtpSslTrust;

    @Value("${spring.mail.properties.mail.smtp.ssl.protocols:TLSv1.2}")
    private String smtpSslProtocols;

    @Bean
    public JavaMailSender javaMailSender(SmtpMailCredentialService smtpMailCredentialService,
                                         com.example.mail.config.MailProperties mailProperties) {
        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();
        configureMailSender(mailSender, smtpMailCredentialService, mailProperties);
        return mailSender;
    }

    public void configureMailSender(JavaMailSenderImpl mailSender,
                                    SmtpMailCredentialService smtpMailCredentialService,
                                    com.example.mail.config.MailProperties mailProperties) {
        // 1. First check database for active credentials
        Optional<SmtpMailCredential> activeCredential = smtpMailCredentialService != null
                ? smtpMailCredentialService.getActiveSmtpCredential()
                : Optional.empty();

        SmtpMailCredential credential = activeCredential.orElse(null);
        if (credential != null) {
            logger.info("Configuring JavaMailSender using active database credentials (Host: {}, Port: {}, Username: {})",
                    credential.getHost(), credential.getPort(), credential.getUsername());
        } else {
            logger.warn("No active SMTP credential found in database. Falling back to application properties.");
        }

        // Host: database -> properties -> default
        String host = null;
        if (credential != null && credential.getHost() != null && !credential.getHost().trim().isEmpty()) {
            host = credential.getHost().trim();
        } else if (mailProperties != null && mailProperties.getAccounts() != null && !mailProperties.getAccounts().isEmpty()
                && mailProperties.getAccounts().get(0).getHost() != null && !mailProperties.getAccounts().get(0).getHost().trim().isEmpty()) {
            host = mailProperties.getAccounts().get(0).getHost().trim();
        } else {
            host = defaultHost;
        }

        // Port: database -> properties -> default
        Integer port = null;
        if (credential != null && credential.getPort() != null) {
            port = credential.getPort();
        } else if (mailProperties != null && mailProperties.getAccounts() != null && !mailProperties.getAccounts().isEmpty()
                && mailProperties.getAccounts().get(0).getPort() > 0) {
            port = mailProperties.getAccounts().get(0).getPort();
        } else {
            port = defaultPort;
        }

        // Username: database -> properties -> default
        String username = null;
        if (credential != null && credential.getUsername() != null && !credential.getUsername().trim().isEmpty()) {
            username = credential.getUsername().trim();
        } else if (mailProperties != null && mailProperties.getAccounts() != null && !mailProperties.getAccounts().isEmpty()) {
            username = mailProperties.getAccounts().get(0).getUsername();
        } else {
            username = defaultUsername;
        }

        // Password: database -> properties -> default
        String password = null;
        if (credential != null && credential.getPassword() != null && !credential.getPassword().trim().isEmpty()) {
            password = credential.getPassword().trim();
        } else if (mailProperties != null && mailProperties.getAccounts() != null && !mailProperties.getAccounts().isEmpty()) {
            password = mailProperties.getAccounts().get(0).getPassword();
        } else {
            password = defaultPassword;
        }

        // Protocol: database -> properties
        String protocol = (credential != null && credential.getProtocol() != null && !credential.getProtocol().trim().isEmpty())
                ? credential.getProtocol().trim()
                : mailProtocol;

        // Auth: database -> properties
        boolean auth = (credential != null && credential.getSmtpAuth() != null)
                ? credential.getSmtpAuth()
                : smtpAuth;

        // StartTLS Enable: database -> properties
        boolean startTls = (credential != null && credential.getStartTlsEnable() != null)
                ? credential.getStartTlsEnable()
                : smtpStartTlsEnable;

        // SSL Trust: database -> properties -> default to "*"
        String trust = "*";
        if (credential != null && credential.getSmtpSslTrust() != null && !credential.getSmtpSslTrust().trim().isEmpty()) {
            trust = credential.getSmtpSslTrust().trim();
        } else if (smtpSslTrust != null && !smtpSslTrust.trim().isEmpty()) {
            trust = smtpSslTrust.trim();
        }

        // If trust is boolean-like ("true", "false", "1", "0"), contains gmail/google, or host is Gmail -> always "*"
        if (trust.isEmpty()
                || "true".equalsIgnoreCase(trust)
                || "false".equalsIgnoreCase(trust)
                || "1".equals(trust)
                || "0".equals(trust)
                || trust.toLowerCase().contains("gmail")
                || trust.toLowerCase().contains("google")
                || (host != null && host.toLowerCase().contains("gmail"))) {
            trust = "*";
        }

        boolean startTlsRequired = smtpStartTlsRequired;
        String sslProtocols = (smtpSslProtocols != null && !smtpSslProtocols.trim().isEmpty()) ? smtpSslProtocols.trim() : "TLSv1.2";

        logger.info("Configured SMTP -> Host: {}, Port: {}, Auth: {}, StartTLS: {}, SSLTrust: {}, SSLProtocols: {}",
                host, port, auth, startTls, trust, sslProtocols);

        mailSender.setHost(host);
        mailSender.setPort(port != null ? port : 587);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        mailSender.setProtocol(protocol);
        mailSender.setDefaultEncoding("UTF-8");

        Properties javaMailProperties = new Properties();
        javaMailProperties.put("mail.smtp.auth", String.valueOf(auth));
        javaMailProperties.put("mail.smtp.starttls.enable", String.valueOf(startTls));
        javaMailProperties.put("mail.smtp.starttls.required", String.valueOf(startTlsRequired));
        javaMailProperties.put("mail.smtp.ssl.trust", trust);
        javaMailProperties.put("mail.smtps.ssl.trust", trust);
        javaMailProperties.put("mail.smtp.ssl.checkserveridentity", "false");
        javaMailProperties.put("mail.smtps.ssl.checkserveridentity", "false");
        javaMailProperties.put("mail.smtp.ssl.protocols", sslProtocols);
        javaMailProperties.put("mail.smtps.ssl.protocols", sslProtocols);
        mailSender.setJavaMailProperties(javaMailProperties);
    }
}

