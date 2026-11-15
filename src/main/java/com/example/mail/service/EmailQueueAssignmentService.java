package com.example.mail.service;

import com.example.mail.dto.soap.aacc.ReadContactResult;
import com.example.mail.model.AgentSession;
import com.example.mail.model.Email;
import com.example.mail.model.EmailQueueAssignment;
import com.example.mail.model.UserMaster;
import com.example.mail.repository.AgentSessionRepository;
import com.example.mail.repository.EmailQueueAssignmentRepository;
import com.example.mail.repository.EmailRepository;
import com.example.mail.repository.UserMasterRepo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
public class EmailQueueAssignmentService {

    private static final Logger log = LoggerFactory.getLogger("MAIL_SERVICES_AVAAYA_LOGGER");

    private final EmailQueueAssignmentRepository emailQueueAssignmentRepository;
    private final EmailRepository emailRepository;
    private final UserMasterRepo userMasterRepo;
    private final AgentSessionRepository agentSessionRepository;

    public EmailQueueAssignmentService(EmailQueueAssignmentRepository emailQueueAssignmentRepository,
                                       EmailRepository emailRepository,
                                       UserMasterRepo userMasterRepo,
                                       AgentSessionRepository agentSessionRepository) {
        this.emailQueueAssignmentRepository = emailQueueAssignmentRepository;
        this.emailRepository = emailRepository;
        this.userMasterRepo = userMasterRepo;
        this.agentSessionRepository = agentSessionRepository;
    }

    public EmailQueueAssignment recordQueueAssignment(Long contactId, String sessionId, ReadContactResult result) {
        try {
            log.info("recordQueueAssignment() - Recording queue assignment for ContactID: {}, SessionID: {}",
                    contactId, sessionId);

            // 1. Resolve matching Email entity if contactId is present
            Email rootEmail = null;
            if (contactId != null) {
                List<Email> emails = emailRepository.findByContactId(contactId);
                if (emails != null && !emails.isEmpty()) {
                    rootEmail = emails.stream()
                            .filter(e -> e.getParentEmail() == null)
                            .findFirst()
                            .orElse(emails.get(0));
                }
            }

            // 2. Check if a queue assignment already exists for this contact or email (upsert pattern)
            EmailQueueAssignment assignment = null;
            if (contactId != null) {
                assignment = emailQueueAssignmentRepository.findFirstByContactId(contactId).orElse(null);
            }
            if (assignment == null && rootEmail != null) {
                assignment = emailQueueAssignmentRepository.findFirstByEmail(rootEmail).orElse(null);
            }
            if (assignment == null) {
                assignment = new EmailQueueAssignment();
            }

            // 3. Populate core landing fields
            assignment.setContactId(contactId);
            assignment.setSessionId(sessionId);

            Date now = new Date();
            assignment.setTiming(now);
            assignment.setAssignedAt(now);
            assignment.setCompleted(false);

            if (rootEmail != null) {
                assignment.setEmail(rootEmail);
            }

            // 4. Populate fields from ReadContactResult and fallback to Email entity
            if (result != null) {
                if (result.getSkillset() != null) {
                    assignment.setSkillId(result.getSkillset().getId());
                    assignment.setSkillsetName(result.getSkillset().getName());
                }
                assignment.setPriorityLevel(result.getPriority());
                assignment.setCustomerId(result.getCustomerID());
                assignment.setStatus(result.getStatus());
                assignment.setSource(result.getSource());
                assignment.setSubject(result.getOriginalSubject());

                if (result.getAgent() != null && result.getAgent().getId() != null) {
                    assignment.setAgentId(result.getAgent().getId());
                }
            }

            // Fallback for fields from rootEmail if not set
            if (rootEmail != null) {
                if (assignment.getSkillId() == null) {
                    assignment.setSkillId(rootEmail.getSkillId());
                }
                if (assignment.getSkillsetName() == null) {
                    assignment.setSkillsetName(rootEmail.getSkillsetName());
                }
                if (assignment.getPriorityLevel() == null) {
                    assignment.setPriorityLevel(rootEmail.getPriority());
                }
                if (assignment.getCustomerId() == null) {
                    assignment.setCustomerId(rootEmail.getCustomerId());
                }
                if (assignment.getStatus() == null) {
                    assignment.setStatus(rootEmail.getStatus());
                }
                if (assignment.getSource() == null) {
                    assignment.setSource(rootEmail.getSource());
                }
                if (assignment.getSubject() == null) {
                    assignment.setSubject(rootEmail.getOriginalSubject());
                }
                if (assignment.getAgentId() == null && rootEmail.getAgentId() != null) {
                    assignment.setAgentId(rootEmail.getAgentId());
                }
            }

            // Default priority if still null
            if (assignment.getPriorityLevel() == null) {
                assignment.setPriorityLevel("Normal");
            }

            // 5. Fallback agentId from AgentSession if still null
            if (assignment.getAgentId() == null && sessionId != null && !sessionId.trim().isEmpty()) {
                Optional<AgentSession> sessionOpt = agentSessionRepository.findByTokenId(sessionId.trim());
                if (!sessionOpt.isPresent()) {
                    try {
                        sessionOpt = agentSessionRepository.findById(Long.parseLong(sessionId.trim()));
                    } catch (NumberFormatException ignored) {
                        // sessionId is not a Long primary key
                    }
                }
                if (sessionOpt.isPresent() && sessionOpt.get().getAgentId() != null) {
                    assignment.setAgentId(Long.valueOf(sessionOpt.get().getAgentId()));
                }
            }

            // 6. Resolve UserMaster if agentId is known
            if (assignment.getAgentId() != null) {
                try {
                    UserMaster userMaster = userMasterRepo.findByAgentId(assignment.getAgentId());
                    if (userMaster != null) {
                        assignment.setAssignedUser(userMaster);
                    }
                } catch (Exception ex) {
                    log.debug("recordQueueAssignment() - Could not resolve UserMaster for agentId {}: {}",
                            assignment.getAgentId(), ex.getMessage());
                }
            }

            try {
                EmailQueueAssignment saved = emailQueueAssignmentRepository.save(assignment);
                log.info("recordQueueAssignment() - Queue assignment saved successfully with ID: {} for ContactID: {}",
                        saved.getId(), contactId);
                return saved;
            } catch (Exception saveEx) {
                log.warn("recordQueueAssignment() - Insert/save encountered constraint, updating existing record: {}", saveEx.getMessage());
                // Fallback for concurrent/legacy constraints: lookup existing and update
                EmailQueueAssignment existing = null;
                if (rootEmail != null) {
                    existing = emailQueueAssignmentRepository.findFirstByEmail(rootEmail).orElse(null);
                }
                if (existing == null && contactId != null) {
                    existing = emailQueueAssignmentRepository.findFirstByContactId(contactId).orElse(null);
                }
                if (existing != null) {
                    existing.setContactId(contactId);
                    existing.setSessionId(sessionId);
                    existing.setTiming(now);
                    existing.setAssignedAt(now);
                    existing.setStatus(assignment.getStatus());
                    existing.setSkillId(assignment.getSkillId());
                    existing.setSkillsetName(assignment.getSkillsetName());
                    existing.setPriorityLevel(assignment.getPriorityLevel());
                    existing.setCustomerId(assignment.getCustomerId());
                    existing.setAgentId(assignment.getAgentId());
                    existing.setAssignedUser(assignment.getAssignedUser());
                    return emailQueueAssignmentRepository.save(existing);
                }
                throw saveEx;
            }
        } catch (Exception e) {
            log.error("recordQueueAssignment() - Error saving queue assignment for ContactID {}: {}",
                    contactId, e.getMessage(), e);
            return null;
        }
    }
}
