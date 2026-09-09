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
    List<Email> findByAgentId(Long agentId);
    List<Email> findByStatusIgnoreCase(String status);
    List<Email> findByContactIdGreaterThan(Long contactId);
    List<Email> findByContactIdLessThan(Long contactId);
    Email findByMessageId(String messageId);

    @Query(value = "SELECT * FROM emails WHERE LOWER(CAST(mail_to AS NVARCHAR(MAX))) LIKE LOWER(CONCAT('%', :mailTo, '%'))", nativeQuery = true)
    List<Email> findByMailToContainingIgnoreCase(@Param("mailTo") String mailTo);

    @Query(value = "SELECT * FROM emails WHERE LOWER(CAST(mail_from AS NVARCHAR(MAX))) LIKE LOWER(CONCAT('%', :mailFrom, '%'))", nativeQuery = true)
    List<Email> findByMailFromContainingIgnoreCase(@Param("mailFrom") String mailFrom);

    @Query(value = "SELECT * FROM emails WHERE LOWER(CAST(original_subject AS NVARCHAR(MAX))) LIKE LOWER(CONCAT('%', :subject, '%'))", nativeQuery = true)
    List<Email> findByOriginalSubjectContainingIgnoreCase(@Param("subject") String subject);

    @Query("SELECT e FROM Email e WHERE e.status IS NULL OR UPPER(e.status) <> 'NEW'")
    List<Email> findAllExceptNewStatus();

    @Query("SELECT e FROM Email e WHERE e.skillId = :skillId AND e.status = 'Open' AND e.assigned = false ORDER BY e.priorityId ASC, e.arrivalTime ASC")
    List<Email> findTopPendingEmailsBySkill(@Param("skillId") Long skillId);
}
