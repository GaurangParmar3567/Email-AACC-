package com.example.mail.controller;

import com.example.mail.model.MailSendStatus;
import com.example.mail.model.MakerTransferStatus;
import com.example.mail.repository.MailSendStatusRepository;
import com.example.mail.repository.MailSendStatusSpecification;
import com.example.mail.repository.MakerTransferStatusRepository;
import com.example.mail.repository.MakerTransferStatusSpecification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Collections;
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
     * Endpoint 1: Fetches MakerTransferStatus (Maker Transfered).
     * Supports pagination (page, size) and date filtering (fromDate, toDate via createdDate).
     *
     * Parameters:
     *   contactId - Existing optional Contact ID filter
     *   fromDate  - Inclusive start date, YYYY-MM-DD
     *   toDate    - Inclusive end date, YYYY-MM-DD
     *   page      - Zero-based page number; default is 0
     *   size      - Number of records per page (e.g. 10, 25, 50); default is 10
     *   unpaged   - Optional boolean; if true returns raw JSON array
     *
     * Example URLs:
     *   GET /api/status/maker-transfer?page=0&size=10
     *   GET /api/status/maker-transfer?fromDate=2026-10-01&toDate=2026-10-07&page=0&size=10
     *   GET /api/status/maker-transfer?contactId=4143121&page=0&size=10
     */
    @GetMapping({"/maker-transfer", "/makerTransferStatus"})
    public ResponseEntity<?> getAllMakerTransferStatus(
            @RequestParam(required = false) String contactId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "false") boolean unpaged) {

        logger.info("getAllMakerTransferStatus() - Fetching MakerTransferStatus (Maker Transfered): contactId={}, fromDate={}, toDate={}, page={}, size={}, unpaged={}",
                contactId, fromDate, toDate, page, size, unpaged);

        LocalDateTime fromDateTime = parseFromDate(fromDate);
        LocalDateTime toDateTime = parseToDate(toDate);

        Specification<MakerTransferStatus> spec = MakerTransferStatusSpecification.withFilters(
                contactId != null ? contactId.trim() : null,
                fromDateTime,
                toDateTime
        );

        if (unpaged) {
            List<MakerTransferStatus> list = makerTransferStatusRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "mailId"));
            logger.info("getAllMakerTransferStatus() - Returning unpaged list of {} records", list.size());
            return ResponseEntity.ok(list);
        }

        int pageNum = Math.max(0, page);
        int pageSize = size > 0 ? size : 10;
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "mailId"));

        Page<MakerTransferStatus> results = executeMakerTransferPage(spec, pageable);
        logger.info("getAllMakerTransferStatus() - Completed. TotalElements={}, TotalPages={}, CurrentPage={}",
                results.getTotalElements(), results.getTotalPages(), results.getNumber());
        return ResponseEntity.ok(results);
    }

    /**
     * Fetches a single MakerTransferStatus by its MailID.
     */
    @GetMapping({"/maker-transfer/{mailId}", "/makerTransferStatus/{mailId}"})
    public ResponseEntity<MakerTransferStatus> getMakerTransferStatusById(@PathVariable Long mailId) {
        logger.info("getMakerTransferStatusById() - Fetching MakerTransferStatus for MailID: {}", mailId);
        return makerTransferStatusRepository.findById(mailId)
                .map(status -> {
                    logger.info("getMakerTransferStatusById() - Found MakerTransferStatus for MailID: {}", mailId);
                    return ResponseEntity.ok(status);
                })
                .orElseGet(() -> {
                    logger.warn("getMakerTransferStatusById() - MakerTransferStatus not found for MailID: {}", mailId);
                    return ResponseEntity.notFound().build();
                });
    }

    /**
     * Endpoint 2: Fetches MailSendStatus (Checker Transfered).
     * Supports pagination (page, size) and date filtering (fromDate, toDate via updatedDate).
     *
     * Parameters:
     *   contactId - Existing optional Contact ID filter
     *   fromDate  - Inclusive start date, YYYY-MM-DD
     *   toDate    - Inclusive end date, YYYY-MM-DD
     *   page      - Zero-based page number; default is 0
     *   size      - Number of records per page (e.g. 10, 25, 50); default is 10
     *   unpaged   - Optional boolean; if true returns raw JSON array
     *
     * Example URLs:
     *   GET /api/status/mail-send?page=0&size=10
     *   GET /api/status/mail-send?fromDate=2026-10-01&toDate=2026-10-07&page=0&size=10
     *   GET /api/status/mail-send?contactId=4143121&page=0&size=10
     */
    @GetMapping({"/mail-send", "/mailSendStatus"})
    public ResponseEntity<?> getAllMailSendStatus(
            @RequestParam(required = false) String contactId,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false, defaultValue = "false") boolean unpaged) {

        logger.info("getAllMailSendStatus() - Fetching MailSendStatus (Checker Transfered): contactId={}, fromDate={}, toDate={}, page={}, size={}, unpaged={}",
                contactId, fromDate, toDate, page, size, unpaged);

        LocalDateTime fromDateTime = parseFromDate(fromDate);
        LocalDateTime toDateTime = parseToDate(toDate);

        Specification<MailSendStatus> spec = MailSendStatusSpecification.withFilters(
                contactId != null ? contactId.trim() : null,
                fromDateTime,
                toDateTime
        );

        if (unpaged) {
            List<MailSendStatus> list = mailSendStatusRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "mailId"));
            logger.info("getAllMailSendStatus() - Returning unpaged list of {} records", list.size());
            return ResponseEntity.ok(list);
        }

        int pageNum = Math.max(0, page);
        int pageSize = size > 0 ? size : 10;
        Pageable pageable = PageRequest.of(pageNum, pageSize, Sort.by(Sort.Direction.DESC, "mailId"));

        Page<MailSendStatus> results = executeMailSendPage(spec, pageable);
        logger.info("getAllMailSendStatus() - Completed. TotalElements={}, TotalPages={}, CurrentPage={}",
                results.getTotalElements(), results.getTotalPages(), results.getNumber());
        return ResponseEntity.ok(results);
    }

    /**
     * Fetches a single MailSendStatus by its MailID.
     */
    @GetMapping({"/mail-send/{mailId}", "/mailSendStatus/{mailId}"})
    public ResponseEntity<MailSendStatus> getMailSendStatusById(@PathVariable Long mailId) {
        logger.info("getMailSendStatusById() - Fetching MailSendStatus for MailID: {}", mailId);
        return mailSendStatusRepository.findById(mailId)
                .map(status -> {
                    logger.info("getMailSendStatusById() - Found MailSendStatus for MailID: {}", mailId);
                    return ResponseEntity.ok(status);
                })
                .orElseGet(() -> {
                    logger.warn("getMailSendStatusById() - MailSendStatus not found for MailID: {}", mailId);
                    return ResponseEntity.notFound().build();
                });
    }

    private Page<MakerTransferStatus> executeMakerTransferPage(Specification<MakerTransferStatus> spec, Pageable pageable) {
        try {
            return makerTransferStatusRepository.findAll(spec, pageable);
        } catch (Exception ex) {
            if (isOffsetNotSupportedException(ex)) {
                logger.warn("executeMakerTransferPage() - SQL dialect offset not supported. Applying in-memory pagination fallback: {}", ex.getMessage());
                List<MakerTransferStatus> allMatching = makerTransferStatusRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "mailId"));
                int start = (int) pageable.getOffset();
                int end = Math.min((start + pageable.getPageSize()), allMatching.size());
                List<MakerTransferStatus> subList = (start <= allMatching.size()) ? allMatching.subList(start, end) : Collections.emptyList();
                return new PageImpl<>(subList, pageable, allMatching.size());
            }
            throw ex;
        }
    }

    private Page<MailSendStatus> executeMailSendPage(Specification<MailSendStatus> spec, Pageable pageable) {
        try {
            return mailSendStatusRepository.findAll(spec, pageable);
        } catch (Exception ex) {
            if (isOffsetNotSupportedException(ex)) {
                logger.warn("executeMailSendPage() - SQL dialect offset not supported. Applying in-memory pagination fallback: {}", ex.getMessage());
                List<MailSendStatus> allMatching = mailSendStatusRepository.findAll(spec, Sort.by(Sort.Direction.DESC, "mailId"));
                int start = (int) pageable.getOffset();
                int end = Math.min((start + pageable.getPageSize()), allMatching.size());
                List<MailSendStatus> subList = (start <= allMatching.size()) ? allMatching.subList(start, end) : Collections.emptyList();
                return new PageImpl<>(subList, pageable, allMatching.size());
            }
            throw ex;
        }
    }

    private boolean isOffsetNotSupportedException(Throwable t) {
        while (t != null) {
            if (t instanceof UnsupportedOperationException && t.getMessage() != null && t.getMessage().contains("offset is not supported")) {
                return true;
            }
            if (t.getMessage() != null && t.getMessage().contains("offset is not supported")) {
                return true;
            }
            t = t.getCause();
        }
        return false;
    }

    private LocalDateTime parseFromDate(String fromDate) {
        if (fromDate == null || fromDate.trim().isEmpty()) {
            return null;
        }
        String trimmed = fromDate.trim();
        try {
            if (trimmed.length() == 10 && trimmed.contains("-")) {
                LocalDate date = LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE);
                return date.atStartOfDay();
            } else if (trimmed.contains("T")) {
                return LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } else {
                LocalDate date = LocalDate.parse(trimmed);
                return date.atStartOfDay();
            }
        } catch (Exception ex) {
            logger.warn("parseFromDate() - Could not parse fromDate '{}': {}", fromDate, ex.getMessage());
            return null;
        }
    }

    private LocalDateTime parseToDate(String toDate) {
        if (toDate == null || toDate.trim().isEmpty()) {
            return null;
        }
        String trimmed = toDate.trim();
        try {
            if (trimmed.length() == 10 && trimmed.contains("-")) {
                LocalDate date = LocalDate.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE);
                return date.atTime(LocalTime.MAX);
            } else if (trimmed.contains("T")) {
                return LocalDateTime.parse(trimmed, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
            } else {
                LocalDate date = LocalDate.parse(trimmed);
                return date.atTime(LocalTime.MAX);
            }
        } catch (Exception ex) {
            logger.warn("parseToDate() - Could not parse toDate '{}': {}", toDate, ex.getMessage());
            return null;
        }
    }
}
