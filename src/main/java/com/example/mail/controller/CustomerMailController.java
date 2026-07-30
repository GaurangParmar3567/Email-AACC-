package com.example.mail.controller;

import com.example.mail.dto.response.CustomerMailForReplyDTO;
import com.example.mail.dto.response.MakerMailActionDTO;
import com.example.mail.exception.ContactNotFoundException;
import com.example.mail.model.MakerTransferStatus;
import com.example.mail.repository.ContactActionRepository;
import com.example.mail.repository.MakerTransferStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Optional;
import java.text.SimpleDateFormat;

/**
 * Replacement for the legacy GetCustomerMailforChecker WebMethod.
 * Authentication and host/contact-id validation are intentionally not included.
 */
@Controller
@RequestMapping("/email-service/sbi")
public class CustomerMailController {

    private static final String LEGACY_SIGNATURE_URL =
            "http://10.48.2.20/IResolveAdmin/Editor/editor/filemanager/connectors/aspx/userfiles/signature1(2).jpg";
    private static final String CURRENT_SIGNATURE_URL =
            "http://10.48.12.94/IResolveAdmin/Editor/editor/filemanager/connectors/aspx/userfiles/signature1(2).jpg";

    private final MakerTransferStatusRepository makerTransferStatusRepository;
    private final ContactActionRepository contactActionRepository;
    private final Logger logger = LoggerFactory.getLogger("Customer_Mail_LOGGER");

    public CustomerMailController(MakerTransferStatusRepository makerTransferStatusRepository,
                                  ContactActionRepository contactActionRepository) {
        this.makerTransferStatusRepository = makerTransferStatusRepository;
        this.contactActionRepository = contactActionRepository;
    }

    @GetMapping("/checker")
    @ResponseBody
    public CustomerMailForReplyDTO getCustomerMailforChecker(
            @RequestParam("ContactID") String contactId) {

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
            String replyText = latestMail.getBodyContent();
            if (replyText != null) {
                replyText = replyText.replace(LEGACY_SIGNATURE_URL, CURRENT_SIGNATURE_URL);
            }

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

    @GetMapping("/maker")
    @ResponseBody
    public CustomerMailForReplyDTO getCustomerMailforMaker(
            @RequestParam("ContactID") String contactId) {

        logger.info("Maker mail requested for ContactID {}", contactId);
        try {
            java.util.List<MakerMailActionDTO> actions = contactActionRepository
                    .findMakerMailFieldsByContactId(toContactId(contactId));
            if (actions.isEmpty()) {
                logger.warn("No maker action found for ContactID {}", contactId);
                throw new ContactNotFoundException(contactId);
            }
            MakerMailActionDTO action = actions.get(0);
            logger.debug("Maker action found for ContactID {}; {} matching action(s)", contactId, actions.size());

            CustomerMailForReplyDTO reply = new CustomerMailForReplyDTO();
            reply.setMessage("Valid ContactID");
            reply.setReplyText(buildMakerReplyText(action));
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

    private String buildMakerReplyText(MakerMailActionDTO action) {
        StringBuilder replyText = new StringBuilder();
        replyText.append("<span style='font-size:11.5pt;line-height:115%;font-family:\"Arial\",\"sans-serif\"; color:black'><b>From:</b> ")
                .append(valueOrEmpty(action.getMailFrom()))
                .append("<br>");

        if (action.getCreationTime() != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, MMMM d, yyyy h:mm:ss a");
            replyText.append("<b>Sent:</b>")
                    .append(dateFormat.format(new java.util.Date(action.getCreationTime())))
                    .append("<br>");
        }

        replyText.append("<b>To:</b> ").append(valueOrEmpty(action.getMailTo())).append("<br>");
        if (hasText(action.getMailCc())) {
            replyText.append("<b>Cc:</b> ").append(action.getMailCc()).append("<br>");
        }
        replyText.append("<b>Subject:</b> ").append(valueOrEmpty(action.getSubject())).append("</span><br>");
        replyText.append(hasText(action.getTextHtml()) ? action.getTextHtml() : valueOrEmpty(action.getText()));
        return replyText.toString();
    }

    private boolean hasText(String value) {
        return value != null && !value.isEmpty();
    }

    private String valueOrEmpty(String value) {
        return value == null ? "" : value;
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
