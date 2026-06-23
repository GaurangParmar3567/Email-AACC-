package com.example.mail.repository;

import com.example.mail.model.ContactAction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ContactActionRepository extends JpaRepository<ContactAction, Long> {
    List<ContactAction> findByContactId(Long contactId);
}
