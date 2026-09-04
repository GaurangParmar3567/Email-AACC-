package com.example.mail.model;

import lombok.Data;
import javax.persistence.*;
import java.time.LocalDateTime;
import java.sql.Timestamp;

@Data
@Entity
@Table(name = "MakerTransferStatus")
@NamedStoredProcedureQuery(
    name = "MakerTransferStatus.saveMakerTransferDetails",
    procedureName = "dbo.USP_SaveMakerTransferDetails",
    parameters = {
        @StoredProcedureParameter(name = "FromEmail", mode = ParameterMode.IN, type = String.class),
        @StoredProcedureParameter(name = "ToEmail", mode = ParameterMode.IN, type = String.class),
        @StoredProcedureParameter(name = "Subject", mode = ParameterMode.IN, type = String.class),
        @StoredProcedureParameter(name = "BodyContent", mode = ParameterMode.IN, type = String.class),
        @StoredProcedureParameter(name = "ContactID", mode = ParameterMode.IN, type = String.class),
        @StoredProcedureParameter(name = "AgentID", mode = ParameterMode.IN, type = Long.class),
        @StoredProcedureParameter(name = "ClosedReason", mode = ParameterMode.IN, type = String.class),
        @StoredProcedureParameter(name = "Comment", mode = ParameterMode.IN, type = String.class),
        @StoredProcedureParameter(name = "ActionID", mode = ParameterMode.IN, type = Long.class),
        @StoredProcedureParameter(name = "ERRORNO", mode = ParameterMode.OUT, type = Integer.class),
        @StoredProcedureParameter(name = "ERRORMSG", mode = ParameterMode.OUT, type = String.class),
        @StoredProcedureParameter(name = "MailID", mode = ParameterMode.OUT, type = Long.class),
        @StoredProcedureParameter(name = "AnswerDT", mode = ParameterMode.IN, type = Timestamp.class)
    }
)
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
    // @Column(name = "BodyContent")
    @Column(name = "BodyContent", columnDefinition = "TEXT")
    private String bodyContent;

    // @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "CreatedDate")
    private LocalDateTime createdDate;

    @Column(name = "AgentID", length = 50)
    private String agentId;

    @Column(name = "ClosedReason", length = 255)
    private String closedReason;

    @Column(name = "Comment", length = 255)
    private String comment;

    @Column(name = "ActionID")
    private Long actionId;

    // @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "AnsweredDateTime")
    // private Date answeredDateTime;
    private LocalDateTime answeredDateTime;

    @Column(name = "SKILLSET")
    private Integer skillset;
}
