package com.example.mail.service;

import com.example.mail.dto.soap.aacc.*;
import com.example.mail.dto.soap.common.AgentDTO;
import com.example.mail.dto.soap.common.MillisecondsDTO;
import com.example.mail.dto.soap.common.SkillsetDTO;
import com.example.mail.exception.ContactNotFoundException;
import com.example.mail.model.Attachment;
import com.example.mail.model.ContactAction;
import com.example.mail.model.Email;
import com.example.mail.repository.ContactActionRepository;
import com.example.mail.repository.ClosedReasonRepository;
import com.example.mail.repository.EmailRepository;
import com.example.mail.repository.SkillMasterRepo;
import com.example.mail.util.IdGenerator;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

@Service
public class AaccContactService {

    private final EmailRepository emailRepository;
    private final ContactActionRepository contactActionRepository;
    private final ClosedReasonRepository closedReasonRepository;
    private final SkillMasterRepo skillMasterRepository;

    public AaccContactService(EmailRepository emailRepository,
                              ContactActionRepository contactActionRepository,
                              ClosedReasonRepository closedReasonRepository,
                              SkillMasterRepo skillMasterRepository) {
        this.emailRepository = emailRepository;
        this.contactActionRepository = contactActionRepository;
        this.closedReasonRepository = closedReasonRepository;
        this.skillMasterRepository = skillMasterRepository;
    }

    public ReadContactResult getContactDetails(Long contactId) {
        List<Email> emailThread = emailRepository.findByContactId(contactId);
        if (emailThread == null || emailThread.isEmpty()) {
            throw new ContactNotFoundException(String.valueOf(contactId));
        }

        Email rootEmail = emailThread.stream()
                .filter(email -> email.getParentEmail() == null)
                .findFirst()
                .orElse(emailThread.get(0));
        ReadContactResult result = new ReadContactResult();
        result.setId(rootEmail.getContactId());
        result.setCustomerID(rootEmail.getCustomerId());
        result.setOriginalSubject(rootEmail.getOriginalSubject());
        result.setSource(rootEmail.getSource());
        result.setStatus(rootEmail.getStatus());
        result.setPriority(rootEmail.getPriority());
        result.setTimezone(rootEmail.getTimezone());
        result.setOpenDuration(rootEmail.getOpenDuration());
        result.setMailTo(rootEmail.getMailTo());
        result.setMailFrom(rootEmail.getMailFrom());
        result.setMailCc(rootEmail.getMailCc());
        result.setContactType(rootEmail.getContactType());

        if (rootEmail.getSkillsetId() != null) {
            SkillsetDTO skillset = new SkillsetDTO();
            skillset.setId(rootEmail.getSkillsetId());
            skillset.setName(rootEmail.getSkillsetName());
            result.setSkillset(skillset);
        }
        if (rootEmail.getArrivalTime() != null) {
            MillisecondsDTO arrivalTime = new MillisecondsDTO();
            arrivalTime.setMilliseconds(rootEmail.getArrivalTime());
            result.setArrivalTime(arrivalTime);
        }
        if (rootEmail.getOpenTime() != null) {
            MillisecondsDTO openTime = new MillisecondsDTO();
            openTime.setMilliseconds(rootEmail.getOpenTime());
            result.setOpenTime(openTime);
        }
        if (rootEmail.getAgentId() != null) {
            AgentDTO agent = new AgentDTO();
            agent.setId(rootEmail.getAgentId());
            agent.setFirstName(rootEmail.getAgentFirstName());
            agent.setLastName(rootEmail.getAgentLastName());
            result.setAgent(agent);
        }

        ActionListDTO actionList = new ActionListDTO();
        for (Email threadEmail : emailThread) {
            if (threadEmail.getContactActions() == null) {
                continue;
            }
            for (ContactAction action : threadEmail.getContactActions()) {
                if (action == null) {
                    continue;
                }
                AWActionDTO actionDto = new AWActionDTO();
                actionDto.setId(action.getActionId());
                actionDto.setContactID(action.getContactId());
                actionDto.setSubject(action.getSubject());
                actionDto.setText(action.getTextContent());
                actionDto.setTextHTML(action.getTextHtml());
                actionDto.setCallbackStatus(action.getCallbackStatus());
                actionDto.setSource(action.getSource());
                actionDto.setComment(action.getComment());
                actionDto.setMailFrom(action.getMailFrom());
                actionDto.setMailTo(action.getMailTo());
                actionDto.setMailCC(action.getMailCc());
                actionDto.setTimeAllocated(action.getTimeAllocated());
                actionDto.setOutboundTalkTime(0);
                actionDto.setOutboundDispositionCode(action.getOutboundDispositionCode());
                actionDto.setActionType(action.getActionType());

                if (action.getCreationTime() != null) {
                    MillisecondsDTO creationTime = new MillisecondsDTO();
                    creationTime.setMilliseconds(action.getCreationTime());
                    actionDto.setCreationTime(creationTime);
                }
                if (action.getAgentId() != null) {
                    AgentDTO agent = new AgentDTO();
                    agent.setId(action.getAgentId());
                    agent.setFirstName(action.getAgentFirstName());
                    agent.setLastName(action.getAgentLastName());
                    actionDto.setAgent(agent);
                }
                if (action.getClosedReasonName() != null || action.getClosedReasonNumericValue() != null) {
                    ClosedReasonDTO reason = new ClosedReasonDTO();
                    reason.setName(action.getClosedReasonName());
                    reason.setNumericValue(action.getClosedReasonNumericValue());
                    actionDto.setClosedReason(reason);
                }
                if (action.getAttachments() != null && !action.getAttachments().isEmpty()) {
                    AttachmentListDTO attachments = new AttachmentListDTO();
                    for (Attachment attachment : action.getAttachments()) {
                        AWAttachmentDTO attachmentDto = new AWAttachmentDTO();
                        attachmentDto.setId(attachment.getId());
                        attachmentDto.setDisplayFileName(attachment.getDisplayName());
                        attachmentDto.setInternalFileName(attachment.getInternalPath());
                        attachments.getAwAttachments().add(attachmentDto);
                    }
                    actionDto.setAttachmentList(attachments);
                }
                actionList.getAwActions().add(actionDto);
            }
        }
        result.setActionList(actionList);
        return result;
    }

    @Transactional
    public long transferToSkillset(long contactId, long skillsetId) {
        List<Email> emails = requireEmails(contactId);
        String skillsetName = skillMasterRepository.findById(skillsetId)
                .orElseThrow(() -> new IllegalArgumentException("Skillset not found for ID " + skillsetId))
                .getName();
        for (Email email : emails) {
            email.setSkillsetId(skillsetId);
            email.setSkillsetName(skillsetName);
            email.setAssigned(false);
        }
        emailRepository.saveAll(emails);
        return contactId;
    }

    @Transactional
    public long closeContact(long contactId, String closureText, Long reasonCode, Boolean reasonSpecified) {
        ContactAction action = new ContactAction();
        action.setActionId(IdGenerator.generateContactId());
        action.setContactId(contactId);
        action.setSubject("CloseContact");
        action.setTextContent(closureText);
        action.setTextHtml(closureText);
        action.setSource("CloseContact");
        action.setComment(closureText);
        action.setCreationTime(System.currentTimeMillis());
        if (Boolean.TRUE.equals(reasonSpecified) && reasonCode != null) {
            action.setClosedReasonNumericValue(reasonCode.intValue());
        }
        contactActionRepository.save(action);

        List<Email> emails = requireEmails(contactId);
        for (Email email : emails) {
            email.setStatus("Closed");
        }
        emailRepository.saveAll(emails);
        return action.getActionId();
    }

    public List<AWClosedReasonCode> getClosedReasonCodes() {
        List<AWClosedReasonCode> result = new java.util.ArrayList<>();
        closedReasonRepository.findAll().forEach(closedReason -> {
            if (closedReason == null) {
                return;
            }
            AWClosedReasonCode code = new AWClosedReasonCode();
            code.setName(closedReason.getName());
            code.setNumericValue(closedReason.getOldCodeMappingID());
            result.add(code);
        });
        return result;
    }

    @Transactional(readOnly = true)
    public String getHistoryForContactId(String contactIdValue) {
        StringBuilder html = new StringBuilder("<html><p align=\"center\"><b>Contact ID<br>")
                .append(escapeHtml(contactIdValue)).append("</b></p>");
        Long contactId;
        try {
            contactId = Long.parseLong(contactIdValue == null ? "" : contactIdValue.trim());
        } catch (NumberFormatException exception) {
            return html.append("<p><b>Error: Invalid contact ID</b></p></html>").toString();
        }

        List<Email> emails = emailRepository.findByContactId(contactId);
        if (emails == null || emails.isEmpty()) {
            return html.append("<p><b>No history found</b></p></html>").toString();
        }
        List<ContactAction> actions = contactActionRepository.findByContactId(contactId);
        actions.sort(Comparator.comparing(ContactAction::getCreationTime,
                Comparator.nullsLast(Comparator.naturalOrder())));
        if (actions.isEmpty()) {
            for (Email email : emails) {
                appendHistoryBlock(html, email.getSource(), email.getReceivedDate(), email.getMailFrom(),
                        email.getMailTo(), email.getMailCc(), email.getSubject(), email.getBodyHtml(),
                        email.getText(), email.getStatus(), null, null, email.getAttachments());
            }
        } else {
            for (ContactAction action : actions) {
                appendHistoryBlock(html, action.getSource(), action.getCreationTime(), action.getMailFrom(),
                        action.getMailTo(), action.getMailCc(), action.getSubject(), action.getTextHtml(),
                        action.getTextContent(), action.getClosedReasonName(), action.getComment(),
                        action.getAgentFirstName(), action.getAttachments());
            }
        }
        return html.append("</html>").toString();
    }

    private void appendHistoryBlock(StringBuilder html, String source, Object creationTime, String mailFrom,
                                    String mailTo, String mailCc, String subject, String textHtml, String text,
                                    String closedReason, String comment, String agentName,
                                    List<Attachment> attachments) {
        html.append("<hr><table border=1 cellspacing=0 cellpadding=4 width=90% align=center><tr><td><b>")
                .append(escapeHtml(source == null ? "" : source.replace('_', ' '))).append("</b>");
        appendHistoryField(html, "Date", formatHistoryDate(creationTime));
        appendHistoryField(html, "From", mailFrom);
        appendHistoryField(html, "To", mailTo);
        appendHistoryField(html, "CC", mailCc);
        appendHistoryField(html, "Subject", subject);
        if (textHtml != null && !textHtml.trim().isEmpty()) {
            html.append("<br><b>Content:</b><br>").append(textHtml);
        } else {
            appendHistoryField(html, "Content", text);
        }
        if (attachments != null) {
            for (Attachment attachment : attachments) {
                html.append("<br><a target=\"_blank\" href=\"")
                        .append(escapeHtml(attachment.getInternalPath())).append("\">")
                        .append(escapeHtml(attachment.getDisplayName())).append("</a>");
            }
        }
        appendHistoryField(html, "Closed Reason", closedReason);
        appendHistoryField(html, "Agent Note", comment);
        appendHistoryField(html, "Created by Agent", agentName);
        html.append("</td></tr></table>");
    }

    private void appendHistoryField(StringBuilder html, String label, String value) {
        if (value != null && !value.trim().isEmpty()) {
            html.append("<br><b>").append(label).append(":</b> ").append(escapeHtml(value));
        }
    }

    private String formatHistoryDate(Object value) {
        if (value instanceof Long) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date((Long) value));
        }
        if (value instanceof Date) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format((Date) value);
        }
        return value == null ? null : value.toString();
    }

    private String escapeHtml(String value) {
        return value == null ? "" : value.replace("&", "&amp;").replace("<", "&lt;")
                .replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;");
    }

    public GetHistoryFromAACCResult getHistory(String searchType, String searchValue) {
        List<Email> emails = emailRepository.findAll();
        if (searchType != null && searchValue != null) {
            String type = searchType.toUpperCase();
            String value = searchValue.trim();
            if ("STATUS".equals(type)) {
                emails = "NEW".equalsIgnoreCase(value)
                        ? emailRepository.findByStatusIgnoreCase("NEW")
                        : emailRepository.findAllExceptNewStatus();
            } else if ("TOMAIL".equals(type)) {
                emails = emailRepository.findByMailToContainingIgnoreCase(value);
            } else if ("SUBJECT".equals(type)) {
                emails = emailRepository.findByOriginalSubjectContainingIgnoreCase(value);
            } else if ("AGENTID".equals(type)) {
                emails = findByLong(value, emailRepository::findByAgentId);
            } else if ("ID".equals(type)) {
                emails = findByLong(value, emailRepository::findByContactId);
            } else if ("GTID".equals(type) || "LTID".equals(type)) {
                emails = findByLong(value, "GTID".equals(type)
                        ? emailRepository::findByContactIdGreaterThan
                        : emailRepository::findByContactIdLessThan);
            } else {
                emails = emailRepository.findByMailFromContainingIgnoreCase(value);
            }
        }

        GetHistoryFromAACCResult result = new GetHistoryFromAACCResult();
        for (Email email : emails) {
            MailHistory history = new MailHistory();
            history.setContactId(email.getContactId() == null ? "" : email.getContactId().toString());
            history.setCreatedTime(email.getReceivedDate() == null ? "" : email.getReceivedDate().toString());
            history.setSubject(email.getSubject());
            history.setMailFrom(email.getMailFrom());
            history.setMailTo(email.getMailTo());
            history.setMailCC(email.getMailCc());
            history.setSkillSet(email.getSkillsetName());
            history.setClosedReason(email.getStatus());
            history.setStatus(email.getStatus());
            history.setMessage("Valid Input");
            result.getMailHistory().add(history);
        }
        return result;
    }

    private List<Email> requireEmails(long contactId) {
        List<Email> emails = emailRepository.findByContactId(contactId);
        if (emails == null || emails.isEmpty()) {
            throw new ContactNotFoundException(String.valueOf(contactId));
        }
        return emails;
    }

    private List<Email> findByLong(String value, java.util.function.Function<Long, List<Email>> finder) {
        try {
            Long id = Long.parseLong(value);
            return finder.apply(id);
        } catch (NumberFormatException exception) {
            return Collections.emptyList();
        }
    }
}
