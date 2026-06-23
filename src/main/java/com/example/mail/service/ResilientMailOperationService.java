package com.example.mail.service;

import com.example.mail.repository.EmailRepository;
import com.example.mail.config.MailProperties;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.mail.*;
import javax.mail.internet.MimeMessage;
import java.util.Properties;

@Service
public class ResilientMailOperationService {

    private final Logger logger = LoggerFactory.getLogger("MAIL_RESILIENCE_SERVICE");
    private final EmailRepository emailRepository;

    public ResilientMailOperationService(EmailRepository emailRepository) {
        this.emailRepository = emailRepository;
    }

    @Retry(name = "mailConnectionRetry", fallbackMethod = "fallbackConnection")
    @CircuitBreaker(name = "mailConnectionCircuitBreaker", fallbackMethod = "fallbackConnection")
    @TimeLimiter(name = "mailConnectionTimeLimiter")
    public Store connectToStore(MailProperties.MailAccount account) throws Exception {
        logger.info("Attempting to connect to mail store for account: {}", account.getUsername());

        Properties props = new Properties();
        props.put("mail.store.protocol", account.getProtocol());
        props.put("mail.imap.port", String.valueOf(account.getPort()));
        props.put("mail.imap.connectiontimeout", "5000");
        props.put("mail.imap.timeout", "10000");

        Session session = Session.getInstance(props);
        Store store = session.getStore(account.getProtocol());
        store.connect(account.getHost(), account.getPort(), account.getUsername(), account.getPassword());

        return store;
    }

    private Store fallbackConnection(MailProperties.MailAccount account, Exception e) {
        logger.error("Fallback: Unable to connect to mail store for account: {}. Error: {}",
                account.getUsername(), e.getMessage());
        return null;
    }

    @Retry(name = "folderOperationRetry", fallbackMethod = "fallbackFolderSync")
    @CircuitBreaker(name = "folderOperationCircuitBreaker", fallbackMethod = "fallbackFolderSync")
    public void syncFolderWithRetry(Store store, String folderName, MailProperties.MailAccount account) throws Exception {
        syncFolder(store, folderName, account);
    }

    private void fallbackFolderSync(Store store, String folderName, MailProperties.MailAccount account, Exception e) {
        logger.error("Fallback: Unable to sync folder '{}' for account: {}. Error: {}",
                folderName, account.getUsername(), e.getMessage());
    }

    private void syncFolder(Store store, String folderName, MailProperties.MailAccount account) throws Exception {
        Folder folder = null;
        try {
            folder = store.getFolder(folderName);
            if (!folder.exists()) {
                logger.warn("[{}] Folder '{}' does not exist on server. Skipping.", account.getUsername(), folderName);
                return;
            }

            folder.open(Folder.READ_WRITE);
            int messageCount = folder.getMessageCount();
            logger.info("[{}] Folder '{}' opened. Total messages: {}", account.getUsername(), folderName, messageCount);

            Message[] messages = folder.getMessages();
            for (Message message : messages) {
                if (!(message instanceof MimeMessage)) continue;

                MimeMessage mimeMessage = (MimeMessage) message;
                String messageId = mimeMessage.getMessageID();
                String subject = mimeMessage.getSubject();

                if (messageId != null && emailRepository.existsByMessageId(messageId)) {
                    logger.debug("[{}] Skipping duplicate email. ID: {} | Subject: {}", account.getUsername(), messageId, subject);
                    message.setFlag(Flags.Flag.DELETED, true);
                    continue;
                }

                logger.info("[{}] Processing new email. ID: {} | Subject: {}", account.getUsername(), messageId, subject);
                processAndSaveMessageWithRetry(mimeMessage, account.getUsername());
                message.setFlag(Flags.Flag.DELETED, true);
                logger.info("[{}] Successfully archived email: {}", account.getUsername(), subject);
            }
        } finally {
            closeFolder(folder);
        }
    }

    @Retry(name = "messageProcessingRetry", fallbackMethod = "fallbackMessageProcessing")
    @CircuitBreaker(name = "messageProcessingCircuitBreaker")
    public void processAndSaveMessageWithRetry(MimeMessage message, String currentAccountUsername) throws Exception {
        processAndSaveMessage(message, currentAccountUsername);
    }

    private void fallbackMessageProcessing(MimeMessage message, String currentAccountUsername, Exception e) {
        logger.error("Fallback: Unable to process message for account: {}. Error: {}",
                currentAccountUsername, e.getMessage());
    }

    private void processAndSaveMessage(MimeMessage message, String currentAccountUsername) throws Exception {
        // Your existing processAndSaveMessage logic here
        // ...
    }

    private void closeFolder(Folder folder) {
        try {
            if (folder != null && folder.isOpen()) folder.close(true);
        } catch (Exception e) {
            logger.error("Error closing folder", e);
        }
    }
}
