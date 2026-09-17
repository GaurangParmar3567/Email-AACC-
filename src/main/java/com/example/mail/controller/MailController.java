package com.example.mail.controller;

import com.example.mail.dto.request.EmailFilterRequestDTO;
import com.example.mail.dto.request.EmailRequestAIDTO;
import com.example.mail.dto.response.EmailThreadResponseDTO;
import com.example.mail.model.Attachment;
import com.example.mail.dto.response.EmailResponseDTO;
import com.example.mail.service.EmailDisplayService;
import com.example.mail.service.MailSyncService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/email")
@CrossOrigin(origins = "https://ccdemsuat.sbi:6001")
public class MailController {

    private final MailSyncService service;
    private final EmailDisplayService emailDisplayService;

    private final Logger logger = LoggerFactory.getLogger("MAIL_SYNC_LOGGER");

    public MailController(MailSyncService service, EmailDisplayService emailDisplayService) {
        this.service = service;
        this.emailDisplayService = emailDisplayService;
    }

    @GetMapping("/inbox")
    public ResponseEntity<Page<EmailResponseDTO>> getInbox(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        logger.info("Fetching unassigned emails");
        return ResponseEntity.ok(emailDisplayService.getUnassignedEmails(PageRequest.of(page, size)));
    }

//    @GetMapping
//    public ResponseEntity<List<EmailResponseDTO>> getAllEmails() {
//        List<EmailResponseDTO> emails = emailDisplayService.getAllEmails();
//        return ResponseEntity.ok(emails);
//    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailResponseDTO> getEmailById(@PathVariable Long id) {
        logger.info("Fetching email by id: {}", id);
        EmailResponseDTO email = emailDisplayService.getEmailById(id);
        return ResponseEntity.ok(email);
    }

    @PutMapping("/{id}/assign/{agentId}")
    public ResponseEntity<Void> assignEmail(@PathVariable Long id, @PathVariable Long agentId) {
        logger.info("Assigning email id: {} to agent id: {}", id, agentId);
        emailDisplayService.markAsAssigned(id, agentId);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/attachments/{attachmentId}")
    public ResponseEntity<byte[]> getAttachment(@PathVariable Long attachmentId) {
        logger.info("Fetching attachment by id: {}", attachmentId);
        Attachment attachment = emailDisplayService.getAttachmentById(attachmentId);

        String mimeType = attachment.getMimeType() != null ? attachment.getMimeType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        logger.info("Attachment found: id={}, fileName={}, mimeType={}", attachment.getId(), attachment.getFileName(), mimeType);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(mimeType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + attachment.getFileName() + "\"")
                .body(attachment.getFileData());
    }

    @PostMapping("/get-email-details")
    public ResponseEntity<EmailThreadResponseDTO> getEmailDetails(@RequestBody EmailRequestAIDTO emailRequestDTO) {
        logger.info("Fetching complete interaction thread history tracking contactId: {}", emailRequestDTO.getContactId());

        EmailThreadResponseDTO conversationThread = emailDisplayService.getEmailByIdAI(emailRequestDTO.getContactId());
        if (conversationThread == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(conversationThread);
    }

    /**
     * Endpoint to fetch all emails with custom filter parameters via POST (JSON body):
     * e.g. POST /api/v1/email/filter with { "contactId": 123, "status": "Open", "page": 0, "size": 20 }
     */
    @PostMapping("/filter")
    public ResponseEntity<Page<EmailResponseDTO>> filterEmails(@RequestBody(required = false) EmailFilterRequestDTO filter) {
        logger.info("Fetching emails with filters (POST): {}", filter);
        Page<EmailResponseDTO> emails = emailDisplayService.filterEmails(filter);
        return ResponseEntity.ok(emails);
    }

    /**
     * Endpoint to fetch all emails with custom filter parameters via GET (query parameters):
     * e.g. GET /api/v1/email/filter?contactId=123&status=Open&page=0&size=20
     */
    @GetMapping("/filter")
    public ResponseEntity<Page<EmailResponseDTO>> filterEmailsGet(@ModelAttribute EmailFilterRequestDTO filter) {
        logger.info("Fetching emails with filters (GET): {}", filter);
        Page<EmailResponseDTO> emails = emailDisplayService.filterEmails(filter);
        return ResponseEntity.ok(emails);
    }
}