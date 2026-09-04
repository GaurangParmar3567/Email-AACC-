package com.example.mail.service;

import com.example.mail.util.EmailCleaner;
import com.example.mail.config.MailProperties;
import com.example.mail.model.*;
import com.example.mail.repository.*;
import lombok.var;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import javax.mail.*;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

@Service
public class MailSyncService {

    private final Logger logger = LoggerFactory.getLogger("MAIL_SYNC_LOGGER");

    private final EmailRepository emailRepository;
    private final MailProperties mailProperties;
    private final ContactActionRepository contactActionRepository;
    private final SkillMasterRepo skillMasterRepository;
    private final PriorityMasterRepo priorityMasterRepository;
    private final UserMasterRepo userMasterRepo;

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

    public MailSyncService(EmailRepository emailRepository, MailProperties mailProperties, ContactActionRepository contactActionRepository, SkillMasterRepo skillMasterRepository, PriorityMasterRepo priorityMasterRepository, UserMasterRepo userMasterRepo) {
        this.emailRepository = emailRepository;
        this.mailProperties = mailProperties;
        this.contactActionRepository = contactActionRepository;
        this.skillMasterRepository = skillMasterRepository;
        this.priorityMasterRepository = priorityMasterRepository;
        this.userMasterRepo = userMasterRepo;
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
            props.put("mail." + account.getProtocol() + ".host", account.getHost());
            props.put("mail." + account.getProtocol() + ".port", String.valueOf(account.getPort()));
            props.put("mail." + account.getProtocol() + ".auth", "true");
            props.put("mail." + account.getProtocol() + ".ssl.enable", "true");
            props.put("mail." + account.getProtocol() + ".ssl.trust", "*");
            props.put("mail." + account.getProtocol() + ".connectiontimeout", "20000");
            props.put("mail." + account.getProtocol() + ".timeout", "20000");
//            props.put("mail." + account.getProtocol() + ".auth.mechanisms", "LOGIN PLAIN");

//            props.put("mail." + account.getProtocol() + ".socketFactory.class", "javax.net.ssl.SSLSocketFactory");
//            props.put("mail." + account.getProtocol() + ".socketFactory.fallback", "false");
//            props.put("mail." + account.getProtocol() + ".socketFactory.port", String.valueOf(account.getPort()));

//            props.put("mail.debug", "true");
//            props.put("mail.debug.auth", "true");

            Session session = Session.getInstance(props);
            store = session.getStore(account.getProtocol());
//            session.setDebug(true);
            logger.info("Attempting standard authentication connection for user: {}", account.getUsername());
            store.connect(account.getHost(), account.getPort(), account.getUsername(), account.getPassword());
            logger.info("Successfully established connection to corporate mail store.");

            String[] folderNames = {"INBOX"};
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
//            String processedFolderName = "Processed-" + java.time.LocalDate.now();
//            Folder processedFolder = store.getFolder(processedFolderName);
//            if (!processedFolder.exists()) {
//                processedFolder.create(Folder.HOLDS_MESSAGES);
//                logger.info("[{}] Created new archive folder: {}", account.getUsername(), processedFolderName);
//            }

            Message[] messages = folder.getMessages();
            for (Message message : messages) {
                if (!(message instanceof MimeMessage)) continue;

                MimeMessage mimeMessage = (MimeMessage) message;
                String messageId = mimeMessage.getMessageID();
                String subject = mimeMessage.getSubject();

                if (messageId != null && emailRepository.existsByMessageId(messageId)) {
                    logger.debug("[{}] Skipping duplicate email. ID: {} | Subject: {}", account.getUsername(), messageId, subject);
//                    moveAndFlag(folder, message, processedFolder);
                    message.setFlag(Flags.Flag.DELETED, true);
                    continue;
                }

                logger.info("[{}] Processing new email. ID: {} | Subject: {}", account.getUsername(), messageId, subject);
                processAndSaveMessage(mimeMessage, account.getUsername());
//                moveAndFlag(folder, message, processedFolder);
                message.setFlag(Flags.Flag.DELETED, true);
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

        Long contactId = generateContactId();
        email.setContactId(contactId);

        String[] inReplyToHeaders = message.getHeader("In-Reply-To");
        if (inReplyToHeaders != null && inReplyToHeaders.length > 0) {
            email.setInReplyTo(inReplyToHeaders[0].trim());
        }

        String[] referencesHeaders = message.getHeader("References");
        if (referencesHeaders != null && referencesHeaders.length > 0) {
            email.setReferencesHeader(String.join(" ", referencesHeaders));
        }

        Email resolvedParent = null;

        if (email.getInReplyTo() != null) {
            resolvedParent = emailRepository.findByMessageId(email.getInReplyTo());
        }

        if (resolvedParent == null && referencesHeaders != null && referencesHeaders.length > 0) {
            String[] refIds = referencesHeaders[0].split("\\s+");
            for (int i = refIds.length - 1; i >= 0; i--) {
                String refId = refIds[i].trim();
                if (refId.isEmpty()) continue;

                Email ancestor = emailRepository.findByMessageId(refId);
                if (ancestor != null) {
                    resolvedParent = ancestor;
                    break;
                }
            }
        }

        if (resolvedParent != null) {
            email.setParentEmail(resolvedParent);
            email.setContactId(resolvedParent.getContactId());
            logger.info("[Thread Match] Linked email to parent Message-ID: {} under Contact ID: {}",
                    resolvedParent.getMessageId(), resolvedParent.getContactId());
        } else {
            email.setContactId(generateContactId());
            logger.info("[New Thread] No parent found. Generated new Contact ID: {}", email.getContactId());
        }
        if (email.getContactId() == null) {
            email.setContactId(generateContactId());
        }

        email.setCustomerId(extractCustomerIdFromEmail(message));
        InternetAddress from = (InternetAddress) message.getFrom()[0];
        email.setSender(from.getAddress());
        email.setMailFrom(from.getAddress());
//        email.setSender(message.getFrom()[0].toString());
        email.setSubject(message.getSubject());
        email.setOriginalSubject(message.getSubject());
        email.setReceivedDate(message.getReceivedDate());

//        email.setMailFrom(message.getFrom()[0].toString());

        Address[] toAddresses = message.getRecipients(Message.RecipientType.TO);
        if (toAddresses != null && toAddresses.length > 0) {
            email.setMailTo(addressArrayToString(toAddresses));
        }
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

//        email.setSkillId(determineSkillId(email));
        email.setAssigned(false);
        email.setResponded(false);
        email.setSource("EMail");
        email.setStatus("Open");
        email.setContactType("Email");
        email.setTimezone(0);
        Long currentTime = System.currentTimeMillis();
        email.setArrivalTime(currentTime);

        email.setOpenTime(currentTime);
        email.setOpenDuration(20);

        email.setAttachments(new ArrayList<>());
        parseContent(message, email);
        if (email.getBody() != null) {
            email.setBody(EmailCleaner.cleanBody(email.getBody()));
        }
        if (email.getText() != null) {
            email.setText(EmailCleaner.cleanBody(email.getText()));
        }
        if (email.getBodyHtml() != null) {
            email.setBodyHtml(EmailCleaner.cleanBody(email.getBodyHtml())); // clean HTML helper if applicable
        }
        String cleanedBody = EmailCleaner.cleanBody(email.getBody());
        email.setBody(cleanedBody);
        determineSkillsetAndPriority(email);
        emailRepository.save(email);
        logger.info("[{}] Saved email ID: {}. Attachments count: {}", currentAccountUsername, email.getMessageId(), email.getAttachments().size());
        createInitialContactAction(email, currentAccountUsername);
    }

    private void closeFolder(Folder folder) {
        try { if (folder != null && folder.isOpen()) folder.close(true); }
        catch (Exception e) { logger.error("Error closing folder", e); }
    }

    private void closeStore(Store store) {
        try { if (store != null && store.isConnected()) store.close(); }
        catch (Exception e) { logger.error("Error closing store", e); }
    }

    private Long determineSkillId(Email email) {
        return 1L;
    }

    private String addressArrayToString(Address[] addresses) {
        if (addresses == null || addresses.length == 0) {
            return "";
        }

        return Arrays.stream(addresses)
                .map(address -> {
                    if (address instanceof InternetAddress) {
                        return ((InternetAddress) address).getAddress();
                    }
                    return address.toString();
                })
                .collect(Collectors.joining(","));
    }

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

        if (part.isMimeType("text/plain") && email.getBody() == null) {
            email.setBody(part.getContent().toString());
            email.setHtml(false);
            String currentText = email.getBody() != null ? email.getBody() : "";
            email.setText(currentText + part.getContent().toString());
        } else if (part.isMimeType("text/html")) {
            String currentHtml = email.getBodyHtml() != null ? email.getBodyHtml() : "";
            email.setBodyHtml(currentHtml + part.getContent().toString());
            email.setBody(part.getContent().toString());
            email.setHtml(true);
        }
        else if (part.isMimeType("message/rfc822")) {
            logger.info("Found nested RFC822 message. Diving in...");
            parseContent((Part) part.getContent(), email);
        }
        else if (part.isMimeType("multipart/*")) {
            Multipart multipart = (Multipart) part.getContent();
            int partCount = multipart.getCount();
            logger.info("Identified 'multipart/*' content with {} sub-parts.", partCount);

            for (int i = 0; i < partCount; i++) {
                BodyPart bodyPart = multipart.getBodyPart(i);
                String fileName = bodyPart.getFileName();
                String disposition = bodyPart.getDisposition();
                if (fileName != null || Part.ATTACHMENT.equalsIgnoreCase(disposition) || Part.INLINE.equalsIgnoreCase(disposition)) {

                    if (fileName != null) {
                        logger.info("Found attachment: '{}'.", fileName);
                        Attachment attachment = new Attachment();
                        attachment.setFileName(fileName);

                        String rawContentType = bodyPart.getContentType();
                        try {
                            javax.mail.internet.ContentType ct = new javax.mail.internet.ContentType(rawContentType);
                            attachment.setMimeType(ct.getBaseType());
                        } catch (javax.mail.internet.ParseException e) {
                            attachment.setMimeType(rawContentType);
                        }

                        attachment.setFileData(StreamUtils.copyToByteArray(bodyPart.getInputStream()));
                        attachment.setEmail(email);
                        attachment.setInternalPath(fileName);
                        attachment.setDisplayName(fileName);
                        email.getAttachments().add(attachment);
                    } else {
                        parseContent(bodyPart, email);
                    }
                } else {
                    parseContent(bodyPart, email);
                }
            }
        } else {
            logger.debug("Unhandled/Other MIME type: {}.", contentType);
        }
    }
    private Long generateContactId() {
        return System.currentTimeMillis() % 10000000L + 1000000L;
    }

    private Long extractCustomerIdFromEmail(MimeMessage message) {
        return (System.currentTimeMillis() / 1000) % 1000000L;
    }

    private void determineSkillsetAndPriority(Email email) {
        String subject = email.getSubject() != null ? email.getSubject().toLowerCase() : "";

        List<SkillMaster> allSkills = skillMasterRepository.findAll();
        SkillMaster matchedSkill = null;

        for (SkillMaster skill : allSkills) {
            if (skill.getKeywords() != null) {
                String[] keywordArray = skill.getKeywords().split(",");
                for (String keyword : keywordArray) {
                    if (subject.contains(keyword.trim().toLowerCase())) {
                        matchedSkill = skill;
                        break;
                    }
                }
            }
            if (matchedSkill != null) break;
        }

        if (matchedSkill != null) {
            email.setSkillsetId(matchedSkill.getId());
            email.setSkillsetName(matchedSkill.getName());
            email.setSkillId(matchedSkill.getId());
        } else {
            SkillMaster defaultSkill = skillMasterRepository.findByName(mailProperties.getDefaultSkillName());
            email.setSkillsetId(defaultSkill.getId());
            email.setSkillsetName(defaultSkill.getName());
            email.setSkillId(defaultSkill.getId());
        }

        List<PriorityMaster> allPriorities = priorityMasterRepository.findAll();
        PriorityMaster defaultPriority = priorityMasterRepository.findByPriorityLevel(mailProperties.getDefaultPriorityName());
        String matchedPriority = defaultPriority.getPriorityLevel();
        Long matchedPriorityId = defaultPriority.getId();

        for (PriorityMaster priority : allPriorities) {
            if (priority.getKeywords() != null) {
                String[] keywordArray = priority.getKeywords().split(",");
                for (String keyword : keywordArray) {
                    if (subject.contains(keyword.trim().toLowerCase())) {
                        matchedPriority = priority.getPriorityLevel();
                        matchedPriorityId = priority.getId();
                        break;
                    }
                }
            }
        }
        email.setPriority(matchedPriority);
        email.setPriorityId(matchedPriorityId);
        logger.debug("Skill: {}, Priority: {}", email.getSkillsetName(), email.getPriority());
    }

    private void createInitialContactAction(Email email, String currentAccountUsername) {
        ContactAction action = new ContactAction();
        action.setActionId(System.currentTimeMillis() % 10000000L + 1000000L);
        action.setContact(email);
        action.setContactId(email.getContactId());
        action.setSubject(email.getSubject());
        action.setTextContent(email.getText());
        action.setTextHtml(email.getBodyHtml());
        action.setCallbackStatus("Unspecified");
        action.setSource("EMail_from_Customer");
        action.setMailFrom(email.getMailFrom());
        action.setMailTo(email.getMailTo());
        action.setMailCc(email.getMailCc());
        action.setActionType("Email");
        action.setCreationTime(System.currentTimeMillis());
        action.setTimeAllocated(20);
        contactActionRepository.save(action);
    }
}