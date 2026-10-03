package com.example.mail.repository;

import com.example.mail.model.Attachment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AttachmentRepository extends JpaRepository<Attachment, Long> {

    @Query("SELECT e FROM Attachment e WHERE e.email.id = :id")
    List<Optional<Attachment>> findByEmailId(@Param("id") Long id);

    @Query("SELECT a FROM Attachment a WHERE a.contactAction.id = :actionId")
    List<Attachment> findByContactActionId(@Param("actionId") Long actionId);

    @Query("SELECT a FROM Attachment a WHERE a.contactAction.id = :actionId OR a.contactAction.actionId = :actionId")
    List<Attachment> findByContactActionIdOrActionId(@Param("actionId") Long actionId);

    @Query("SELECT a FROM Attachment a WHERE a.email.contactId = :contactId")
    List<Attachment> findByEmailContactId(@Param("contactId") Long contactId);

    @Query("SELECT a FROM Attachment a WHERE a.email.id IN :emailIds")
    List<Attachment> findByEmailIdIn(@Param("emailIds") List<Long> emailIds);
}