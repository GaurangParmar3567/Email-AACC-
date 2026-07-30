package com.example.mail.repository;

import com.example.mail.model.Email;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmailRepository extends JpaRepository<Email, Long> {
    boolean existsByMessageId(String messageId);
    boolean existsBySenderAndSubject(String sender, String subject);
    List<Email> findByAssignedFalseOrderByReceivedDateDesc();
    Page<Email> findByAssignedFalseAndNotToBeDownloadedFalseOrderByReceivedDateDesc(Pageable pageable);

    @Query("SELECT e FROM Email e LEFT JOIN FETCH e.attachments WHERE e.id = :id")
    Optional<Email> findByIdWithAttachments(@Param("id") Long id);

    List<Email> findByContactId(Long contactId);
    Email findByMessageId(String messageId);

    @Query("SELECT e FROM Email e WHERE e.skillId = :skillId AND e.status = 'Open' AND e.assigned = false ORDER BY e.priorityId ASC, e.arrivalTime ASC")
    List<Email> findTopPendingEmailsBySkill(@Param("skillId") Long skillId);
}
