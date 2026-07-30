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
}