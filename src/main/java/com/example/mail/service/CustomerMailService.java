package com.example.mail.service;

import com.example.mail.dto.response.CustomerMailForReplyDTO;
import com.example.mail.dto.response.MakerMailActionDTO;
import com.example.mail.exception.ContactNotFoundException;
import com.example.mail.model.MakerTransferStatus;
import com.example.mail.repository.EmailRepository;
import com.example.mail.repository.MakerTransferStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CustomerMailService {

    private static final String LEGACY_SIGNATURE_URL =
            "http://10.48.2.20/IResolveAdmin/Editor/editor/filemanager/connectors/aspx/userfiles/signature1(2).jpg";
    private static final String CURRENT_SIGNATURE_URL =
            "http://10.48.12.94/IResolveAdmin/Editor/editor/filemanager/connectors/aspx/userfiles/signature1(2).jpg";

    private final MakerTransferStatusRepository makerTransferStatusRepository;
    private final EmailRepository emailRepository;
    private final Logger logger = LoggerFactory.getLogger("Customer_Mail_LOGGER");

    public CustomerMailService(MakerTransferStatusRepository makerTransferStatusRepository,
                               EmailRepository emailRepository) {
        this.makerTransferStatusRepository = makerTransferStatusRepository;
        this.emailRepository = emailRepository;
    }

    public CustomerMailForReplyDTO getCustomerMailforChecker(String contactId) {
        logger.info("Checker mail requested for ContactID {}", contactId);
        CustomerMailForReplyDTO reply = new CustomerMailForReplyDTO();
        reply.setMessage("Valid ContactID");

        try {
            Optional<MakerTransferStatus> result = makerTransferStatusRepository
                    .findFirstByContactIdOrderByMailIdDesc(contactId);

            if (!result.isPresent()) {
                logger.info("No maker transfer status found for ContactID {}; using maker action fallback", contactId);
                return getCustomerMailforMaker(contactId);
            }

            MakerTransferStatus latestMail = result.get();
            logger.debug("Latest maker transfer status found for ContactID {} with MailID {}",
                    contactId, latestMail.getMailId());
            String replyText = replaceLegacySignature(latestMail.getBodyContent());

            reply.setReplyText(replyText);
            reply.setClosedReason(latestMail.getClosedReason());
            reply.setComment(latestMail.getComment());

            if (startsWithMakerPlaceholder(replyText)) {
                logger.info("Maker transfer status for ContactID {} requires maker action fallback", contactId);
                reply.setReplyText(getCustomerMailforMaker(contactId).getReplyText());
            }
            logger.info("Checker mail response prepared for ContactID {}", contactId);
            return reply;
        } catch (ContactNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            logger.error("Unable to fetch checker mail for ContactID {}", contactId, exception);
            reply.setGetError(exception.getMessage());
            return reply;
        }
    }

    public CustomerMailForReplyDTO getCustomerMailforMaker(String contactId) {
        logger.info("Maker mail requested for ContactID {}", contactId);
        try {
            List<com.example.mail.model.Email> emails = emailRepository.findByContactId(toContactId(contactId));
            if (emails.isEmpty()) {
                logger.warn("No maker email found for ContactID {}", contactId);
                throw new ContactNotFoundException(contactId);
            }

            com.example.mail.model.Email email = emails.get(0);
            MakerMailActionDTO action = new MakerMailActionDTO(
                    email.getReceivedDate() == null ? null : email.getReceivedDate().getTime(),
                    email.getMailFrom(),
                    email.getMailTo(),
                    email.getMailCc(),
                    email.getSubject(),
                    email.getBodyHtml(),
                    email.getText());
            logger.debug("Maker email found for ContactID {}; {} matching email(s)", contactId, emails.size());

            String replyText = action.getTextHtml() != null ? action.getTextHtml() : action.getText();
            CustomerMailForReplyDTO reply = new CustomerMailForReplyDTO();
            reply.setMessage("Valid ContactID");
            reply.setReplyText(replaceLegacySignature(replyText));
            logger.info("Maker mail response prepared for ContactID {}", contactId);
            return reply;
        } catch (ContactNotFoundException exception) {
            throw exception;
        } catch (Exception exception) {
            logger.error("Unable to fetch maker mail for ContactID {}", contactId, exception);
            CustomerMailForReplyDTO reply = new CustomerMailForReplyDTO();
            reply.setMessage("Valid ContactID");
            reply.setGetError(exception.getMessage());
            return reply;
        }
    }

    private boolean startsWithMakerPlaceholder(String replyText) {
        return replyText != null && (replyText.startsWith("skillset -") || replyText.startsWith("agent -"));
    }

    private String replaceLegacySignature(String value) {
        return value == null ? null : value.replace(LEGACY_SIGNATURE_URL, CURRENT_SIGNATURE_URL);
    }

    private Long toContactId(String contactId) {
        try {
            return Long.valueOf(contactId);
        } catch (NumberFormatException exception) {
            logger.warn("Non-numeric ContactID received for maker action lookup: {}", contactId);
            throw new ContactNotFoundException(contactId);
        }
    }
}