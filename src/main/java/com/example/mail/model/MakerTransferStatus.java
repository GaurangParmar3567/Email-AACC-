package com.example.mail.model;

import lombok.Getter;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

@Entity
@Table(name = "maker_transfer_status")
@Getter
@Setter
public class MakerTransferStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "MailID")
    private Long mailId;

    @Column(name = "FromEmail", nullable = false, length = 100)
    private String fromEmail;

    @Column(name = "ToEmail", nullable = false, length = 100)
    private String toEmail;

    @Column(name = "Subject", length = 1000)
    private String subject;

    @Column(name = "ContactID")
    private String contactId;

    @Lob
    @Column(name = "BodyContent")
    private String bodyContent;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CreatedDate")
    private Date createdDate;

    @Column(name = "AgentID", length = 50)
    private String agentId;

    @Column(name = "ClosedReason", length = 255)
    private String closedReason;

    @Column(name = "Comment", length = 255)
    private String comment;

    @Column(name = "ActionID")
    private Long actionId;

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "AnsweredDateTime")
    private Date answeredDateTime;

    @Column(name = "SKILLSET")
    private Integer skillset;
}
