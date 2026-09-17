package com.example.mail.controller;

import com.example.mail.model.MailSendStatus;
import com.example.mail.model.MakerTransferStatus;
import com.example.mail.repository.MailSendStatusRepository;
import com.example.mail.repository.MakerTransferStatusRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping({"/api/status", "/api/mail-status"})
@CrossOrigin(origins = "*")
public class MailStatusController {

    private static final Logger logger = LoggerFactory.getLogger("MAIL_SERVICES_AVAAYA_LOGGER");

    private final MakerTransferStatusRepository makerTransferStatusRepository;
    private final MailSendStatusRepository mailSendStatusRepository;

    public MailStatusController(MakerTransferStatusRepository makerTransferStatusRepository,
                                MailSendStatusRepository mailSendStatusRepository) {
        this.makerTransferStatusRepository = makerTransferStatusRepository;
        this.mailSendStatusRepository = mailSendStatusRepository;
    }

    /**
     * Endpoint 1: Fetches all details from MakerTransferStatus.
     * Optional filter by ContactID: ?contactId=...
     *
     * URLs:
     *   GET /api/status/maker-transfer
     *   GET /api/status/makerTransferStatus
     *   GET /api/mail-status/maker-transfer
     *   GET /api/mail-status/makerTransferStatus
     */
    @GetMapping({"/maker-transfer", "/makerTransferStatus"})
    public ResponseEntity<List<MakerTransferStatus>> getAllMakerTransferStatus(
            @RequestParam(required = false) String contactId) {
        logger.info("Fetching MakerTransferStatus details{}",
                (contactId != null && !contactId.trim().isEmpty()) ? " for ContactID: " + contactId.trim() : " for all records");

        List<MakerTransferStatus> results;
        if (contactId != null && !contactId.trim().isEmpty()) {
            results = makerTransferStatusRepository.findByContactIdOrderByMailIdDesc(contactId.trim());
        } else {
            results = makerTransferStatusRepository.findAll(Sort.by(Sort.Direction.DESC, "mailId"));
        }

        logger.info("Completed fetching MakerTransferStatus details. Count: {}", results.size());
        return ResponseEntity.ok(results);
    }

    /**
     * Fetches a single MakerTransferStatus by its MailID.
     */
    @GetMapping({"/maker-transfer/{mailId}", "/makerTransferStatus/{mailId}"})
    public ResponseEntity<MakerTransferStatus> getMakerTransferStatusById(@PathVariable Long mailId) {
        logger.info("Fetching MakerTransferStatus for MailID: {}", mailId);
        return makerTransferStatusRepository.findById(mailId)
                .map(status -> {
                    logger.info("Found MakerTransferStatus for MailID: {}", mailId);
                    return ResponseEntity.ok(status);
                })
                .orElseGet(() -> {
                    logger.warn("MakerTransferStatus not found for MailID: {}", mailId);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Endpoint 2: Fetches all details from MailSendStatus.
     * Optional filter by ContactID: ?contactId=...
     *
     * URLs:
     *   GET /api/status/mail-send
     *   GET /api/status/mailSendStatus
     *   GET /api/mail-status/mail-send
     *   GET /api/mail-status/mailSendStatus
     */
    @GetMapping({"/mail-send", "/mailSendStatus"})
    public ResponseEntity<List<MailSendStatus>> getAllMailSendStatus(
            @RequestParam(required = false) String contactId) {
        logger.info("Fetching MailSendStatus details{}",
                (contactId != null && !contactId.trim().isEmpty()) ? " for ContactID: " + contactId.trim() : " for all records");

        List<MailSendStatus> results;
        if (contactId != null && !contactId.trim().isEmpty()) {
            results = mailSendStatusRepository.findByContactIdOrderByMailIdDesc(contactId.trim());
        } else {
            results = mailSendStatusRepository.findAll(Sort.by(Sort.Direction.DESC, "mailId"));
        }

        logger.info("Completed fetching MailSendStatus details. Count: {}", results.size());
        return ResponseEntity.ok(results);
    }

    /**
     * Fetches a single MailSendStatus by its MailID.
     */
    @GetMapping({"/mail-send/{mailId}", "/mailSendStatus/{mailId}"})
    public ResponseEntity<MailSendStatus> getMailSendStatusById(@PathVariable Long mailId) {
        logger.info("Fetching MailSendStatus for MailID: {}", mailId);
        return mailSendStatusRepository.findById(mailId)
                .map(status -> {
                    logger.info("Found MailSendStatus for MailID: {}", mailId);
                    return ResponseEntity.ok(status);
                })
                .orElseGet(() -> {
                    logger.warn("MailSendStatus not found for MailID: {}", mailId);
                    return ResponseEntity.notFound().build();
                });
    }
}

