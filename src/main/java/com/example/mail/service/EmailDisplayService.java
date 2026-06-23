package com.example.mail.service;

import com.example.mail.model.Attachment;
import com.example.mail.dto.AttachmentDTO;
import com.example.mail.model.Email;
import com.example.mail.dto.EmailResponseDTO;
import com.example.mail.repository.AttachmentRepository;
import com.example.mail.repository.EmailRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EmailDisplayService {

    private final EmailRepository emailRepository;
    private final AttachmentRepository attachmentRepository;

    public EmailDisplayService(EmailRepository emailRepository, AttachmentRepository attachmentRepository) {
        this.emailRepository = emailRepository;
        this.attachmentRepository = attachmentRepository;
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
}
