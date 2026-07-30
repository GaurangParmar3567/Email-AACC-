package com.example.mail.service;

import com.example.mail.model.Email;
import com.example.mail.repository.ContactActionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Timestamp;

@Service
public class MakerTransferStatusService {

    private static final Logger logger = LoggerFactory.getLogger(MakerTransferStatusService.class);
    private static final int DEFAULT_SKILLSET = 16;

    private final ContactActionRepository contactActionRepository;

    public MakerTransferStatusService(ContactActionRepository contactActionRepository) {
        this.contactActionRepository = contactActionRepository;
    }

}
