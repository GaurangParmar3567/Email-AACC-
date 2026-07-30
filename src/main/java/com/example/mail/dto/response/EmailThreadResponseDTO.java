package com.example.mail.dto.response;

import lombok.Data;

import java.util.Map;

@Data
public class EmailThreadResponseDTO {
    private Long contactId;
    private Long customerId;
    private int totalMessagesInThread;
    private String sortOrder = "ASCENDING";
    private Map<String, EmailDetailDTO> threadTrail;
}
