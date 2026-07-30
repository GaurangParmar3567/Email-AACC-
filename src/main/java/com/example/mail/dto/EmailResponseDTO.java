package com.example.mail.dto;

import lombok.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class EmailResponseDTO {
    private Long id;
    private String subject;
    private String sender;
    private String recipient;
    private String cc;
    private String bcc;
    private String body;
    private String text;
    private String bodyHtml;
    private boolean isHtml;
    private Date receivedDate;
    private List<AttachmentDTO> attachments = new ArrayList<>();
    private boolean isRepeat;
    private String status;
    private boolean inReplyTo;
    private Long contactId;
}
