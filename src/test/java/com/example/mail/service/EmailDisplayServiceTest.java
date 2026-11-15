package com.example.mail.service;

import com.example.mail.repository.AttachmentRepository;
import com.example.mail.repository.EmailRepository;
import com.example.mail.repository.SkillMasterRepo;
import com.example.mail.repository.UserMasterRepo;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class EmailDisplayServiceTest {

    @Test
    void marksContactEmailsAsReplied() {
        EmailRepository emailRepository = mock(EmailRepository.class);
        EmailDisplayService service = new EmailDisplayService(
                emailRepository,
                mock(AttachmentRepository.class),
                mock(SkillMasterRepo.class),
                mock(UserMasterRepo.class),
                mock(DocumentExtractionService.class));
        when(emailRepository.markContactReplied(eq(123L), eq("Replied to Customer"), any(LocalDateTime.class)))
                .thenReturn(2);

        int updatedEmails = service.markContactReplied(123L);

        assertEquals(2, updatedEmails);
        verify(emailRepository).markContactReplied(eq(123L), eq("Replied to Customer"), any(LocalDateTime.class));
    }
}