package com.example.mail.repository;

import com.example.mail.model.ClosedReason;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ClosedReasonRepository extends JpaRepository<ClosedReason, Long> {
}
