package com.example.mail;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/email")
@CrossOrigin(origins = "http://localhost:5173")
public class MailController {

    private final MailSyncService service;
    private final EmailDisplayService emailDisplayService;

    public MailController(MailSyncService service, EmailDisplayService emailDisplayService) {
        this.service = service;
        this.emailDisplayService = emailDisplayService;
    }

    // Supports Pagination: /api/v1/email/inbox?page=0&size=20
    @GetMapping("/inbox")
    public ResponseEntity<Page<EmailResponseDTO>> getInbox(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(emailDisplayService.getUnassignedEmails(PageRequest.of(page, size)));
    }

//    @GetMapping
//    public ResponseEntity<List<EmailResponseDTO>> getAllEmails() {
//        List<EmailResponseDTO> emails = emailDisplayService.getAllEmails();
//        return ResponseEntity.ok(emails);
//    }

    @GetMapping("/{id}")
    public ResponseEntity<EmailResponseDTO> getEmailById(@PathVariable Long id) {
        EmailResponseDTO email = emailDisplayService.getEmailById(id);
        return ResponseEntity.ok(email);
    }

    @PutMapping("/{id}/assign/{agentId}")
    public ResponseEntity<Void> assignEmail(@PathVariable Long id, @PathVariable Long agentId) {
        emailDisplayService.markAsAssigned(id, agentId);
        return ResponseEntity.ok().build();
    }

    // Download Attachment API
    @GetMapping("/attachments/{attachmentId}")
    public ResponseEntity<byte[]> getAttachment(@PathVariable Long attachmentId) {
        // Fetch the attachment entity from the database
        Attachment attachment = emailDisplayService.getAttachmentById(attachmentId);

        // Determine the content type (fallback to generic binary if unknown)
        String mimeType = attachment.getMimeType() != null ? attachment.getMimeType() : MediaType.APPLICATION_OCTET_STREAM_VALUE;

        return ResponseEntity.ok()
                // Set the content type so the browser knows if it's a PDF, Image, etc.
                .contentType(MediaType.parseMediaType(mimeType))

                // Use "inline" to view in browser, or "attachment" to force a file download
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + attachment.getFileName() + "\"")

                // Return the byte array stored in the database
                .body(attachment.getFileData());
    }


}