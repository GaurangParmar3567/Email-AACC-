package com.example.mail.service;

import com.example.mail.model.SmtpMailCredential;
import com.example.mail.repository.SmtpMailCredentialRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class SmtpMailCredentialService {

    private final SmtpMailCredentialRepository smtpMailCredentialRepository;

    public SmtpMailCredentialService(SmtpMailCredentialRepository smtpMailCredentialRepository) {
        this.smtpMailCredentialRepository = smtpMailCredentialRepository;
    }

    @Transactional(readOnly = true)
    public Optional<SmtpMailCredential> getActiveSmtpCredential() {
        return smtpMailCredentialRepository.findFirstByIsActiveTrueOrderByIdAsc();
    }

    @Transactional
    public SmtpMailCredential save(SmtpMailCredential credential) {
        if (credential.getCreatedDate() == null) {
            credential.setCreatedDate(LocalDateTime.now());
        }
        credential.setUpdatedDate(LocalDateTime.now());
        return smtpMailCredentialRepository.save(credential);
    }
}
