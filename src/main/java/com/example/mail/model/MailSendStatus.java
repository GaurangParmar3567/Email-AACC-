package com.example.mail.model;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "MailSendStatus")
public class MailSendStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MailID")
    private Long mailId;

    @Column(name = "FromEmail", nullable = false, length = 100)
    private String fromEmail;

    @Column(name = "ToEmail", nullable = false, length = 1000)
    private String toEmail;

    @Column(name = "CCEmail", length = 1000)
    private String ccEmail;

    @Column(name = "BCCEmail", length = 1000)
    private String bccEmail;

    @Column(name = "Subject", length = 1000)
    private String subject;

    @Lob
    @Column(name = "BodyContent", columnDefinition = "TEXT")
    private String bodyContent;

    @Column(name = "IsSent")
    private Boolean isSent;

    @Column(name = "CreatedDate")
    private LocalDateTime createdDate;

    @Column(name = "UpdatedDate")
    private LocalDateTime updatedDate;

    @Column(name = "Remarks", length = 500)
    private String remarks;

    @Column(name = "ContactID", length = 50)
    private String contactId;

    @Column(name = "AgentID", length = 50)
    private String agentId;

    @Column(name = "ClosedReason", length = 255)
    private String closedReason;

    @Column(name = "Comment", length = 255)
    private String comment;

    @Column(name = "ActionID", nullable = false)
    private Long actionId;

    @Column(name = "AnsweredDateTime")
    private LocalDateTime answeredDateTime;

    @Column(name = "SKILLSET")
    private Integer skillset;
}