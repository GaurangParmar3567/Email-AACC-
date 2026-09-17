package com.example.mail.repository;

import com.example.mail.model.SmtpMailCredential;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SmtpMailCredentialRepository extends JpaRepository<SmtpMailCredential, Long> {

    Optional<SmtpMailCredential> findFirstByIsActiveTrueOrderByIdAsc();
}
