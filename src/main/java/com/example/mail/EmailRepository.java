package com.example.mail;

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
    Optional<Email> findByMessageId(String messageId);
    boolean existsBySenderAndSubject(String sender, String subject);
    List<Email> findByAssignedFalseOrderByReceivedDateDesc();
    List<Email> findByAssignedFalseAndSkillIdOrderByReceivedDateDesc(Long skillId);
    // Updated repository call to respect your business logic
    List<Email> findByAssignedFalseAndNotToBeDownloadedFalseOrderByReceivedDateDesc();
    Page<Email> findByAssignedFalseAndNotToBeDownloadedFalseOrderByReceivedDateDesc(Pageable pageable);

    @Query("SELECT e FROM Email e LEFT JOIN FETCH e.attachments WHERE e.id = :id")
    Optional<Email> findByIdWithAttachments(@Param("id") Long id);
}
