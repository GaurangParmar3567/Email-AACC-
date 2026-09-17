package com.example.mail.service;

import com.example.mail.config.MailProperties;
import com.example.mail.model.SmtpMailCredential;
import com.example.mail.repository.SmtpMailCredentialRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

@Service
public class MailConfigurationService {

    private static final Logger logger = LoggerFactory.getLogger(MailConfigurationService.class);

    private final JdbcTemplate jdbcTemplate;
    private final SmtpMailCredentialRepository smtpMailCredentialRepository;
    private final MailProperties mailProperties;

    @Value("${mail.smtp.procedure.name:USP_GetMailConfiguration}")
    private String procedureName;

    @Value("${spring.mail.host:smtp.gmail.com}")
    private String defaultHost;

    @Value("${spring.mail.port:587}")
    private int defaultPort;

    @Value("${spring.mail.username:}")
    private String defaultUsername;

    @Value("${spring.mail.password:}")
    private String defaultPassword;

    @Value("${spring.mail.protocol:smtp}")
    private String defaultProtocol;

    @Value("${spring.mail.properties.mail.smtp.auth:true}")
    private boolean defaultSmtpAuth;

    @Value("${spring.mail.properties.mail.smtp.starttls.enable:true}")
    private boolean defaultStartTlsEnable;

    @Value("${spring.mail.properties.mail.smtp.starttls.required:true}")
    private boolean defaultStartTlsRequired;

    @Value("${spring.mail.properties.mail.smtp.ssl.trust:*}")
    private String defaultSmtpSslTrust;

    @Value("${spring.mail.properties.mail.smtp.ssl.protocols:TLSv1.2}")
    private String defaultSslProtocols;

    public MailConfigurationService(JdbcTemplate jdbcTemplate,
                                   SmtpMailCredentialRepository smtpMailCredentialRepository,
                                   MailProperties mailProperties) {
        this.jdbcTemplate = jdbcTemplate;
        this.smtpMailCredentialRepository = smtpMailCredentialRepository;
        this.mailProperties = mailProperties;
    }

    /**
     * Extracts SMTP mail configuration by executing the configured stored procedure with ContactID and Skillset.
     * If procedure fails or returns no record, falls back to active database credential or application properties.
     *
     * @param contactId the contact ID
     * @param skillset  the skillset number or ID
     * @return a populated SmtpMailCredential with all required SMTP fields
     */
    public SmtpMailCredential getMailConfiguration(String contactId, Integer skillset) {
        logger.info("Extracting mail configuration for ContactID: {}, Skillset: {} using procedure: {}",
                contactId, skillset, procedureName);

        // 1. Attempt to fetch credentials from stored procedure
        try {
            String sql = "{call " + procedureName + "(?, ?)}";
            List<SmtpMailCredential> results = jdbcTemplate.query(sql, (rs, rowNum) -> mapRowToCredential(rs), contactId, skillset);

            if (results != null && !results.isEmpty()) {
                SmtpMailCredential procedureCredential = results.get(0);
                if (hasUsableDetails(procedureCredential)) {
                    logger.info("Successfully extracted SMTP configuration from procedure {} (Host: {}, Port: {}, Username: {}, FromEmail: {})",
                            procedureName, procedureCredential.getHost(), procedureCredential.getPort(),
                            procedureCredential.getUsername(), procedureCredential.getFromEmail());
                    return procedureCredential;
                }
            }
            logger.warn("Stored procedure {} returned no usable credentials for ContactID: {}, Skillset: {}. Trying fallback.",
                    procedureName, contactId, skillset);
        } catch (Exception ex) {
            logger.warn("Failed to execute stored procedure {} for ContactID: {}, Skillset: {} (Reason: {}). Falling back to active DB credentials.",
                    procedureName, contactId, skillset, ex.getMessage());
        }

        // 2. Fallback to active SmtpMailCredential in database
        if (smtpMailCredentialRepository != null) {
            Optional<SmtpMailCredential> activeCred = smtpMailCredentialRepository.findFirstByIsActiveTrueOrderByIdAsc();
            if (activeCred.isPresent() && hasUsableDetails(activeCred.get())) {
                logger.info("Using active SMTP credential from database (Host: {}, Port: {}, Username: {})",
                        activeCred.get().getHost(), activeCred.get().getPort(), activeCred.get().getUsername());
                return activeCred.get();
            }
        }

        // 3. Fallback to application.properties configuration
        logger.info("Falling back to default application properties for SMTP configuration");
        return createFallbackCredentialFromProperties();
    }

    /**
     * Creates and configures a JavaMailSender dynamically using the supplied SmtpMailCredential.
     *
     * @param credential SmtpMailCredential containing host, port, username, password, etc.
     * @return configured JavaMailSender
     */
    public JavaMailSender createJavaMailSender(SmtpMailCredential credential) {
        if (credential == null) {
            credential = createFallbackCredentialFromProperties();
        }

        JavaMailSenderImpl mailSender = new JavaMailSenderImpl();

        String host = (credential.getHost() != null && !credential.getHost().trim().isEmpty())
                ? credential.getHost().trim() : defaultHost;
        Integer port = (credential.getPort() != null && credential.getPort() > 0)
                ? credential.getPort() : defaultPort;
        String username = (credential.getUsername() != null && !credential.getUsername().trim().isEmpty())
                ? credential.getUsername().trim() : defaultUsername;
        String password = (credential.getPassword() != null && !credential.getPassword().trim().isEmpty())
                ? credential.getPassword().trim() : defaultPassword;
        if (password != null && host != null && host.toLowerCase().contains("gmail")) {
            password = password.replaceAll("\\s+", "");
        }
        String protocol = (credential.getProtocol() != null && !credential.getProtocol().trim().isEmpty())
                ? credential.getProtocol().trim() : defaultProtocol;

        boolean auth = credential.getSmtpAuth() != null ? credential.getSmtpAuth() : defaultSmtpAuth;
        boolean startTls = credential.getStartTlsEnable() != null ? credential.getStartTlsEnable() : defaultStartTlsEnable;

        String trust = "*";
        if (credential.getSmtpSslTrust() != null && !credential.getSmtpSslTrust().trim().isEmpty()) {
            trust = credential.getSmtpSslTrust().trim();
        } else if (defaultSmtpSslTrust != null && !defaultSmtpSslTrust.trim().isEmpty()) {
            trust = defaultSmtpSslTrust.trim();
        }

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

        String sslProtocols = (defaultSslProtocols != null && !defaultSslProtocols.trim().isEmpty())
                ? defaultSslProtocols.trim() : "TLSv1.2";

        logger.info("Building dynamic JavaMailSender -> Host: {}, Port: {}, Username: {}, Auth: {}, StartTLS: {}, Trust: {}",
                host, port, username, auth, startTls, trust);

        mailSender.setHost(host);
        mailSender.setPort(port);
        mailSender.setUsername(username);
        mailSender.setPassword(password);
        mailSender.setProtocol(protocol);
        mailSender.setDefaultEncoding("UTF-8");

        Properties javaMailProperties = new Properties();
        javaMailProperties.put("mail.smtp.auth", String.valueOf(auth));
        javaMailProperties.put("mail.smtp.starttls.enable", String.valueOf(startTls));
        javaMailProperties.put("mail.smtp.starttls.required", String.valueOf(defaultStartTlsRequired));
        javaMailProperties.put("mail.smtp.ssl.trust", trust);
        javaMailProperties.put("mail.smtps.ssl.trust", trust);
        javaMailProperties.put("mail.smtp.ssl.checkserveridentity", "false");
        javaMailProperties.put("mail.smtps.ssl.checkserveridentity", "false");
        javaMailProperties.put("mail.smtp.ssl.protocols", sslProtocols);
        javaMailProperties.put("mail.smtps.ssl.protocols", sslProtocols);

        mailSender.setJavaMailProperties(javaMailProperties);
        return mailSender;
    }

    private boolean hasUsableDetails(SmtpMailCredential credential) {
        return credential != null && (
                (credential.getHost() != null && !credential.getHost().trim().isEmpty()) ||
                (credential.getUsername() != null && !credential.getUsername().trim().isEmpty())
        );
    }

    private SmtpMailCredential createFallbackCredentialFromProperties() {
        SmtpMailCredential fallback = new SmtpMailCredential();

        String host = defaultHost;
        int port = defaultPort;
        String username = defaultUsername;
        String password = defaultPassword;

        if (mailProperties != null && mailProperties.getAccounts() != null && !mailProperties.getAccounts().isEmpty()) {
            if (mailProperties.getAccounts().get(0).getHost() != null && !mailProperties.getAccounts().get(0).getHost().trim().isEmpty()) {
                host = mailProperties.getAccounts().get(0).getHost().trim();
            }
            if (mailProperties.getAccounts().get(0).getPort() > 0) {
                port = mailProperties.getAccounts().get(0).getPort();
            }
            if (mailProperties.getAccounts().get(0).getUsername() != null) {
                username = mailProperties.getAccounts().get(0).getUsername();
            }
            if (mailProperties.getAccounts().get(0).getPassword() != null) {
                password = mailProperties.getAccounts().get(0).getPassword();
            }
        }

        fallback.setHost(host);
        fallback.setPort(port);
        fallback.setUsername(username);
        fallback.setPassword(password);
        fallback.setProtocol(defaultProtocol);
        fallback.setSmtpAuth(defaultSmtpAuth);
        fallback.setStartTlsEnable(defaultStartTlsEnable);
        fallback.setSmtpSslTrust(defaultSmtpSslTrust);
        fallback.setFromEmail(username);
        fallback.setIsActive(true);
        return fallback;
    }

    private SmtpMailCredential mapRowToCredential(ResultSet rs) throws SQLException {
        SmtpMailCredential cred = new SmtpMailCredential();
        ResultSetMetaData meta = rs.getMetaData();
        int colCount = meta.getColumnCount();

        for (int i = 1; i <= colCount; i++) {
            String colName = meta.getColumnLabel(i);
            if (colName == null || colName.isEmpty()) {
                colName = meta.getColumnName(i);
            }
            if (colName == null) {
                continue;
            }

            String normalized = colName.toLowerCase().replace("_", "").replace("-", "");

            if ("host".equals(normalized) || "smtphost".equals(normalized) || "mailhost".equals(normalized)) {
                cred.setHost(rs.getString(i));
            } else if ("port".equals(normalized) || "smtpport".equals(normalized) || "mailport".equals(normalized)) {
                int port = rs.getInt(i);
                if (!rs.wasNull() && port > 0) {
                    cred.setPort(port);
                }
            } else if ("username".equals(normalized) || "user".equals(normalized) || "useremail".equals(normalized) || "smtpusername".equals(normalized)) {
                cred.setUsername(rs.getString(i));
            } else if ("password".equals(normalized) || "pass".equals(normalized) || "smtppassword".equals(normalized)) {
                cred.setPassword(rs.getString(i));
            } else if ("protocol".equals(normalized) || "mailprotocol".equals(normalized)) {
                cred.setProtocol(rs.getString(i));
            } else if ("smtpauth".equals(normalized) || "auth".equals(normalized) || "isauth".equals(normalized)) {
                cred.setSmtpAuth(rs.getBoolean(i));
            } else if ("starttlsenable".equals(normalized) || "starttls".equals(normalized) || "istls".equals(normalized)) {
                cred.setStartTlsEnable(rs.getBoolean(i));
            } else if ("smtpssltrust".equals(normalized) || "ssltrust".equals(normalized) || "trust".equals(normalized)) {
                cred.setSmtpSslTrust(rs.getString(i));
            } else if ("fromemail".equals(normalized) || "from".equals(normalized) || "frommail".equals(normalized) || "senderemail".equals(normalized)) {
                cred.setFromEmail(rs.getString(i));
            } else if ("skill".equals(normalized) || "skillset".equals(normalized)) {
                cred.setSkill(rs.getString(i));
            } else if ("id".equals(normalized)) {
                long id = rs.getLong(i);
                if (!rs.wasNull()) {
                    cred.setId(id);
                }
            } else if ("isactive".equals(normalized)) {
                cred.setIsActive(rs.getBoolean(i));
            }
        }
        return cred;
    }
}

