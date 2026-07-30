package com.example.mail.repository;

import com.example.mail.model.MailSendStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MailSendStatusRepository extends JpaRepository<MailSendStatus, Long> {}