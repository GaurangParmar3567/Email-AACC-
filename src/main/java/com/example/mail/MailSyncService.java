package com.example.mail;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;

@Service
public class MailSyncService {

    private final Logger logger = LoggerFactory.getLogger("MAIL_SYNC_SERVICE_SBI");

    private final EmailRepository emailRepository;
    private final MailProperties mailProperties;

//    @Value("${mail.imap.host}")
//    private String host;
//
//    @Value("${mail.imap.username}")
//    private String username;
//
//    @Value("${mail.imap.port}")
//    private int port;
//
//    @Value("${mail.imap.password}")
//    private String password;
//
//    @Value("${mail.store.protocol}")
//    private String protocol;

    public MailSyncService(EmailRepository emailRepository, MailProperties mailProperties) {
        this.emailRepository = emailRepository;
        this.mailProperties = mailProperties;
        logger.info("MailSyncService initialized with EmailRepository.");
    }

    @Scheduled(fixedRate = 300000)
    public void fetchEmails() {
        logger.info("=============================Starting mail sync task...===================================");

        for (MailProperties.MailAccount account : mailProperties.getAccounts()) {
            logger.info("Syncing emails for account: {}", account.getUsername());
            syncAccount(account);
        }

        logger.info("=================================Mail sync task completed.====================================");
    }

    private void syncAccount(MailProperties.MailAccount account) {
        Store store = null;
        Folder inbox = null;

        try {
            Properties props = new Properties();
            props.put("mail.store.protocol", account.getProtocol());
            props.put("mail.imap.port", String.valueOf(account.getPort()));

            Session session = Session.getInstance(props);
            store = session.getStore(account.getProtocol());
            store.connect(account.getHost(), account.getPort(), account.getUsername(), account.getPassword());
            // 1. Define folders to scan.
            // Note: Gmail uses "[Gmail]/Sent Mail", most others use "Sent".
            String[] folderNames = {"INBOX", "Sent", "[Gmail]/Sent Mail"};

            for (String folderName : folderNames) {
                syncFolder(store, folderName, account);
            }

        } catch (Exception e) {
            logger.error("Error connecting to mail store for account: " + account.getUsername(), e);
        } finally {
            closeStore(store);
        }
    }

    private void syncFolder(Store store, String folderName, MailProperties.MailAccount account) {
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

            // Setup Processed Folder
            String processedFolderName = "Processed-" + java.time.LocalDate.now();
            Folder processedFolder = store.getFolder(processedFolderName);
            if (!processedFolder.exists()) {
                processedFolder.create(Folder.HOLDS_MESSAGES);
                logger.info("[{}] Created new archive folder: {}", account.getUsername(), processedFolderName);
            }

            Message[] messages = folder.getMessages();
            for (Message message : messages) {
                if (!(message instanceof MimeMessage)) continue;

                MimeMessage mimeMessage = (MimeMessage) message;
                String messageId = mimeMessage.getMessageID();
                String subject = mimeMessage.getSubject();

                // LOG: Duplicate tracking
                if (messageId != null && emailRepository.existsByMessageId(messageId)) {
                    logger.debug("[{}] Skipping duplicate email. ID: {} | Subject: {}", account.getUsername(), messageId, subject);
                    moveAndFlag(folder, message, processedFolder);
                    continue;
                }

                logger.info("[{}] Processing new email. ID: {} | Subject: {}", account.getUsername(), messageId, subject);
                processAndSaveMessage(mimeMessage, account.getUsername());
                moveAndFlag(folder, message, processedFolder);
                logger.info("[{}] Successfully archived email: {}", account.getUsername(), subject);
            }
        } catch (Exception e) {
            logger.error("[{}] CRITICAL: Failed to sync folder '{}'. Reason: {}", account.getUsername(), folderName, e.getMessage());
        } finally {
            closeFolder(folder);
        }
    }

    private void moveAndFlag(Folder currentFolder, Message message, Folder destination) throws MessagingException {
        currentFolder.copyMessages(new Message[]{message}, destination);
        message.setFlag(Flags.Flag.DELETED, true);
    }

    public void processAndSaveMessage(MimeMessage message, String currentAccountUsername) throws Exception {
        Email email = new Email();
        String messageId = message.getMessageID();
        email.setMessageId(messageId);

        email.setSender(message.getFrom()[0].toString());
        email.setSubject(message.getSubject());
        email.setReceivedDate(message.getReceivedDate());

        Address[] toAddresses = message.getRecipients(Message.RecipientType.TO);
        Address[] ccAddresses = message.getRecipients(Message.RecipientType.CC);
        Address[] bccAddresses = message.getRecipients(Message.RecipientType.BCC);
        if (bccAddresses != null) {
            email.setBcc(addressArrayToString(bccAddresses));
        }

        if (toAddresses != null) {
            email.setRecipient(addressArrayToString(toAddresses));
        }
        if (ccAddresses != null) {
            email.setCc(addressArrayToString(ccAddresses));
        }
//        if (bccAddresses != null) {
//            email.setBcc(addressArrayToString(bccAddresses));
//        }

        boolean isTo = isAddressInArray(currentAccountUsername, toAddresses);
        boolean isCcOrBcc = isAddressInArray(currentAccountUsername, ccAddresses) ||
                isAddressInArray(currentAccountUsername, bccAddresses);

        if (!isTo && isCcOrBcc) {
            email.setNotToBeDownloaded(true);
            logger.info("Account is in CC/BCC. Setting notToBeDownloaded = true");
        } else {
            email.setNotToBeDownloaded(false);
        }

        boolean isRepeat = emailRepository.existsBySenderAndSubject(email.getSender(), email.getSubject());
        email.setRepeatFlag(isRepeat);

        email.setSkillId(determineSkillId(email));
        email.setAssigned(false);
        email.setResponded(false);

        email.setAttachments(new ArrayList<>());
        parseContent(message, email);
        String cleanedBody = EmailCleaner.cleanBody(email.getBody());
        email.setBody(cleanedBody);
        emailRepository.save(email);
        logger.info("[{}] Saved email ID: {}. Attachments count: {}", currentAccountUsername, email.getMessageId(), email.getAttachments().size());
    }

    // Helper methods for closing resources
    private void closeFolder(Folder folder) {
        try { if (folder != null && folder.isOpen()) folder.close(true); }
        catch (Exception e) { logger.error("Error closing folder", e); }
    }

    private void closeStore(Store store) {
        try { if (store != null && store.isConnected()) store.close(); }
        catch (Exception e) { logger.error("Error closing store", e); }
    }

    // Helper to determine skill ID based on subject/body
    private Long determineSkillId(Email email) {
        // Implement your keyword matching or NLP logic here.
        // Example: if (email.getSubject().contains("Loan")) return 101L;
        return 1L; // Default skill ID
    }

    // Helper to convert Address[] to comma-separated String
    private String addressArrayToString(Address[] addresses) {
        return Arrays.stream(addresses)
                .map(Address::toString)
                .collect(Collectors.joining(","));
    }

    // Helper to check if an email exists in an Address array
    private boolean isAddressInArray(String email, Address[] addresses) {
        if (addresses == null) return false;
        for (Address address : addresses) {
            if (address instanceof InternetAddress) {
                if (((InternetAddress) address).getAddress().equalsIgnoreCase(email)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void closeResources(Folder inbox, Store store) {
        try {
            if (inbox != null && inbox.isOpen()) inbox.close(true);
        } catch (Exception e) { logger.error("Error closing inbox.", e); }
        try {
            if (store != null && store.isConnected()) store.close();
        } catch (Exception e) { logger.error("Error closing store.", e); }
    }

    private void parseContent(Part part, Email email) throws Exception {
        String contentType = part.getContentType();
        logger.debug("Parsing part with Content-Type: {}", contentType);

        // 1. Handle Body Content
        if (part.isMimeType("text/plain") && email.getBody() == null) {
            email.setBody(part.getContent().toString());
            email.setHtml(false);
        } else if (part.isMimeType("text/html")) {
            email.setBody(part.getContent().toString());
            email.setHtml(true);
        }
        // 2. Handle Nested Messages (RFC822) - This is where forwarded attachments live!
        else if (part.isMimeType("message/rfc822")) {
            logger.info("Found nested RFC822 message. Diving in...");
            parseContent((Part) part.getContent(), email);
        }
        // 3. Handle Multipart
        else if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            int partCount = multipart.getCount();
            logger.info("Identified 'multipart/*' content with {} sub-parts.", partCount);

            for (int i = 0; i < partCount; i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                String fileName = bodyPart.getFileName();
                String disposition = bodyPart.getDisposition();

                // CRITICAL CHANGE: Check for fileName FIRST.
                // Many attachments have no disposition but DO have a filename.
                if (fileName != null || Part.ATTACHMENT.equalsIgnoreCase(disposition) || Part.INLINE.equalsIgnoreCase(disposition)) {

                    if (fileName != null) {
                        logger.info("Found attachment: '{}'.", fileName);
                        Attachment attachment = new Attachment();
                        attachment.setFileName(fileName);
                        attachment.setMimeType(bodyPart.getContentType());
                        attachment.setFileData(StreamUtils.copyToByteArray(bodyPart.getInputStream()));
                        attachment.setEmail(email);
                        email.getAttachments().add(attachment);
                    } else {
                        // It might be a nested part without a name, dive deeper
                        parseContent(bodyPart, email);
                    }
                } else {
                    // Not an attachment, keep digging
                    parseContent(bodyPart, email);
                }
            }
        } else {
            logger.debug("Unhandled/Other MIME type: {}.", contentType);
        }
    }
}