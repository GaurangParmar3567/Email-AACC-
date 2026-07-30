package com.example.mail.service;

import com.example.mail.dto.request.ContactRequestDTO;
import com.example.mail.dto.response.ContactResponseDTO;
import com.example.mail.dto.response.EmailDetailDTO;
import com.example.mail.dto.response.EmailThreadResponseDTO;
import com.example.mail.model.Attachment;
import com.example.mail.dto.AttachmentDTO;
import com.example.mail.model.Email;
import com.example.mail.dto.EmailResponseDTO;
import com.example.mail.repository.AttachmentRepository;
import com.example.mail.repository.EmailRepository;
import com.example.mail.repository.SkillMasterRepo;
import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.io.ByteArrayInputStream;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class EmailDisplayService {

    private final EmailRepository emailRepository;
    private final AttachmentRepository attachmentRepository;
    private final SkillMasterRepo skillMasterRepository;
    private final Tika tika = new Tika();
    private final Logger logger = LoggerFactory.getLogger("EMAIL_DISPLAY_SERVICE_LOGGER");

    public EmailDisplayService(EmailRepository emailRepository, AttachmentRepository attachmentRepository, SkillMasterRepo skillMasterRepository) {
        this.emailRepository = emailRepository;
        this.attachmentRepository = attachmentRepository;
        this.skillMasterRepository = skillMasterRepository;
    }

    public List<EmailResponseDTO> getUnassignedEmails() {
        List<Email> unassignedEmails = emailRepository.findByAssignedFalseOrderByReceivedDateDesc();

        return unassignedEmails.stream()
                .map(this::mapToDTO)
                .collect(Collectors.toList());
    }


    public Page<EmailResponseDTO> getUnassignedEmails(Pageable pageable) {
        Page<Email> emails = emailRepository.findByAssignedFalseAndNotToBeDownloadedFalseOrderByReceivedDateDesc(pageable);
        return emails.map(this::mapToDTO);
    }

    public EmailResponseDTO getEmailById(Long id) {
        Email email = emailRepository.findByIdWithAttachments(id)
                .orElseThrow(() -> new RuntimeException("Email not found"));
        return mapToDTO(email);
    }

    @Transactional
    public void markAsAssigned(Long id, Long agentId) {
        Email email = emailRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Email not found"));
        email.setAssigned(true);
        emailRepository.save(email);
    }

    private EmailResponseDTO mapToDTO(Email email) {
        EmailResponseDTO dto = new EmailResponseDTO();
        dto.setId(email.getId());
        dto.setSubject(email.getSubject());
        dto.setSender(email.getSender());
        dto.setReceivedDate(email.getReceivedDate());
        dto.setRepeat(email.isRepeatFlag());
        dto.setHtml(email.isHtml());
        dto.setCc(email.getCc());
        dto.setBcc(email.getBcc());
        dto.setRecipient(email.getRecipient());
        boolean isReply = email.getInReplyTo() != null && !email.getInReplyTo().isEmpty();
        dto.setInReplyTo(isReply);
        dto.setContactId(email.getContactId());
        List<Attachment> attachments = attachmentRepository.findByEmailId(email.getId())
                .stream()
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());

        // Map Attachment entities to a simpler AttachmentDTO (don't send raw BLOBs!)
        List<AttachmentDTO> attachmentDTOs = attachments.stream().map(att -> {
            AttachmentDTO aDto = new AttachmentDTO();
            aDto.setId(att.getId());
            aDto.setFileName(att.getFileName());
            aDto.setDownloadUrl("/api/v1/email/attachments/" + att.getId());
            return aDto;
        }).collect(Collectors.toList());

        dto.setAttachments(attachmentDTOs);
        dto.setBody(email.getBody());
        dto.setText(email.getText());
        return dto;
//        String snippet = email.getBody();
//        if (snippet != null) {
//            // Strip HTML tags for the snippet if it's an HTML email
//            if (email.isHtml()) {
//                snippet = snippet.replaceAll("<[^>]*>", " ");
//            }
//            if (snippet.length() > 100) {
//                snippet = snippet.substring(0, 100) + "...";
//            }
//        }
//        dto.setBody(snippet);
//        return dto;
    }
    public Attachment getAttachmentById(Long attachmentId) {
        return attachmentRepository.findById(attachmentId)
                .orElseThrow(() -> new RuntimeException("Attachment not found with ID: " + attachmentId));
    }

    @Transactional
    public EmailThreadResponseDTO getEmailByIdAI(Long contactId) {
        List<Email> emails = emailRepository.findByContactId(contactId);
        if (emails == null || emails.isEmpty()) {
            return null;
        }

        emails.sort(Comparator.comparing(Email::getId));

        EmailThreadResponseDTO threadResponse = new EmailThreadResponseDTO();
        threadResponse.setContactId(contactId);

        threadResponse.setCustomerId(emails.get(0).getCustomerId());
        threadResponse.setTotalMessagesInThread(emails.size());

        AtomicInteger indexer = new AtomicInteger(1);
        Map<String, EmailDetailDTO> trailMap = emails.stream()
                .collect(Collectors.toMap(
                        email -> String.valueOf(indexer.getAndIncrement()),
                        this::mapToDetailDTO,
                        (oldValue, newValue) -> oldValue,
                        LinkedHashMap::new // Maintains incremental sort sequence insertion orders
                ));

        threadResponse.setThreadTrail(trailMap);
        return threadResponse;
    }

    private EmailDetailDTO mapToDetailDTO(Email email) {
        EmailDetailDTO dto = new EmailDetailDTO();

        EmailDetailDTO.MessageMeta meta = new EmailDetailDTO.MessageMeta();
        meta.setId(email.getId());
        meta.setMessageId(email.getMessageId());
        meta.setParentEmailId(email.getParentEmail() != null ? email.getParentEmail().getId() : null);
        meta.setInReplyTo(email.getInReplyTo());
        meta.setReferencesHeader(email.getReferencesHeader());
        meta.setReceivedDate(email.getReceivedDate());
        meta.setArrivalTime(email.getArrivalTime());
        dto.setMessageMeta(meta);

        EmailDetailDTO.RoutingAndSkill routing = new EmailDetailDTO.RoutingAndSkill();
        routing.setSource(email.getSource());
        routing.setStatus(email.getStatus());
        routing.setPriority(email.getPriority());
        routing.setPriorityId(email.getPriorityId());
        routing.setSkillsetId(email.getSkillsetId());
        routing.setSkillsetName(email.getSkillsetName());
        routing.setSkillId(email.getSkillId());
        routing.setTimezone(email.getTimezone());
        routing.setNotToBeDownloaded(email.isNotToBeDownloaded());
        routing.setRepeatFlag(email.isRepeatFlag());
        routing.setAssigned(email.isAssigned());
        routing.setResponded(email.isResponded());
        dto.setRoutingAndSkill(routing);

        EmailDetailDTO.AgentHandling agent = new EmailDetailDTO.AgentHandling();
        agent.setAgentId(email.getAgentId());
        agent.setAgentFirstName(email.getAgentFirstName());
        agent.setAgentLastName(email.getAgentLastName());
        agent.setOpenTime(email.getOpenTime());
        agent.setOpenDuration(email.getOpenDuration());
        dto.setAgentHandling(agent);

        EmailDetailDTO.Participants participants = new EmailDetailDTO.Participants();
        participants.setSender(email.getSender());
        participants.setRecipient(email.getRecipient());
        participants.setMailFrom(email.getMailFrom());
        participants.setMailTo(email.getMailTo());
        participants.setCc(email.getCc());
        participants.setBcc(email.getBcc());
        dto.setParticipants(participants);

        EmailDetailDTO.Content content = new EmailDetailDTO.Content();
        content.setSubject(email.getSubject());
        content.setOriginalSubject(email.getOriginalSubject());
        content.setHtml(email.isHtml());
        content.setText(email.getText());
        content.setBody(email.getBody());
        content.setBodyHtml(email.getBodyHtml());
        dto.setContent(content);

        if (email.getAttachments() != null) {
            List<EmailDetailDTO.AttachmentDTO> attachmentDTOs = email.getAttachments().stream()
                    .map(this::mapToAttachmentDTO)
                    .collect(Collectors.toList());
            dto.setAttachments(attachmentDTOs);
        } else {
            dto.setAttachments(new ArrayList<>());
        }

        return dto;
    }

    private EmailDetailDTO.AttachmentDTO mapToAttachmentDTO(Attachment attachment) {
        EmailDetailDTO.AttachmentDTO dto = new EmailDetailDTO.AttachmentDTO();
        dto.setId(attachment.getId());
        dto.setFileName(attachment.getFileName());
        dto.setMimeType(attachment.getMimeType());

        if(attachment.getFileData() != null && attachment.getFileData().length>0){
            dto.setFileSizeSummary((attachment.getFileData().length / 1024) + " KB (Raw binary data excluded from payload)");
            String mimetype = attachment.getMimeType() != null ? attachment.getMimeType().toLowerCase() : "";
            if(mimetype.contains("image/")){
                dto.setFileDataBase64(Base64.getEncoder().encodeToString(attachment.getFileData()));
                dto.setExtractedText("");
            } else {
                dto.setFileDataBase64("");
                try (ByteArrayInputStream stream = new ByteArrayInputStream(attachment.getFileData())){
                    String extracted = tika.parseToString(stream);
                    dto.setExtractedText(extracted.trim());
                } catch (Exception e) {
                    logger.error("Failed to extract text from attachment", e);
                    dto.setExtractedText("[Text Extraction failed for this document]");
                }
            }
        } else {
            dto.setExtractedText("");
            dto.setFileDataBase64("");
            dto.setFileSizeSummary("0 KB");
        }
//        dto.setFileDataBase64("");

        if (attachment.getFileData() != null) {

        } else {
            dto.setFileSizeSummary("0 KB");
        }
        return dto;
    }
}
