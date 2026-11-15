package com.example.mail.repository;

import com.example.mail.model.Email;
import com.example.mail.model.EmailQueueAssignment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailQueueAssignmentRepository extends JpaRepository<EmailQueueAssignment, Long> {

    List<EmailQueueAssignment> findByContactId(Long contactId);

    List<EmailQueueAssignment> findByEmail(Email email);

    Optional<EmailQueueAssignment> findFirstByEmail(Email email);

    Optional<EmailQueueAssignment> findFirstByContactId(Long contactId);

    List<EmailQueueAssignment> findBySessionId(String sessionId);

    List<EmailQueueAssignment> findByContactIdOrderByAssignedAtDesc(Long contactId);
}
