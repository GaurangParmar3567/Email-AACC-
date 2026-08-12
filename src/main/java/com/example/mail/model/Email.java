package com.example.mail.model;

import lombok.*;

import javax.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "emails")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class Email {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", unique = true, nullable = false)
    private String messageId;

    @Column(name = "in_reply_to")
    private String inReplyTo;

    @Column(length = 500)
    private String sender;

//    @Column(length = 500)
    //@Lob
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String recipient;

//    @Column(length = 1000)
    //@Lob
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String cc;

    @Column(columnDefinition = "NVARCHAR(MAX)")
//    @Column(length = 1000)
    //@Lob
    private String bcc;

    @Column(length = 1000)
    private String subject;

    //@Lob
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String body;

    //@Lob
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String text;

    //@Lob
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String bodyHtml;

    private boolean isHtml;

    @Temporal(TemporalType.TIMESTAMP)
    private Date receivedDate;

    // --- New Workflow & Routing Fields ---
    private boolean notToBeDownloaded;
    private boolean repeatFlag;
    private Long skillId;
    private boolean assigned;
    private LocalDateTime assignedTime;
    private boolean responded;
    private LocalDateTime respondedTime;

    // --- Avaya Contact Fields ---
    @Column(name = "contact_id")
    private Long contactId;

    @Column(name = "customer_id")
    private Long customerId;

    @Column(name = "original_subject", columnDefinition = "NVARCHAR(MAX)")
    //@Lob
    private String originalSubject;

    @Column(length = 100)
    private String source;  // EMail, Transfer_to_Skillset, etc.

    @Column(length = 100)
    private String status;  // New, Open, Closed, etc.

    @Column(name = "skillset_id")
    private Long skillsetId;

    @Column(name = "skillset_name")
    private String skillsetName;

    private String priority;  // Priority_1_Highest, etc.
    private Long priorityId;

    private Integer timezone;

    @Column(name = "arrival_time")
    private Long arrivalTime;  // milliseconds

    @Column(name = "open_time")
    private Long openTime;  // milliseconds

    @Column(name = "open_duration")
    private Integer openDuration;  // seconds

    @Column(name = "contact_type")
    private String contactType;  // Email, Phone, etc.

    @Column(name = "agent_id")
    private Long agentId;

    @Column(name = "agent_first_name")
    private String agentFirstName;

    @Column(name = "agent_last_name")
    private String agentLastName;

    @Column(name = "mail_to", columnDefinition = "NVARCHAR(MAX)")
    //@Lob
    private String mailTo;

    @Column(name = "mail_from", columnDefinition = "NVARCHAR(MAX)")
    //@Lob
    private String mailFrom;

    @Column(name = "mail_cc", columnDefinition = "NVARCHAR(MAX)")
    //@Lob
    private String mailCc;

    // --- Relations ---
    @OneToMany(mappedBy = "email", cascade = CascadeType.ALL)
    private List<Attachment> attachments = new ArrayList<>();

    @OneToMany(mappedBy = "contact", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<ContactAction> contactActions = new ArrayList<>();

    //@Lob
    @Column(columnDefinition = "NVARCHAR(MAX)")
    private String referencesHeader;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_email_id")
    private Email parentEmail;

    @OneToMany(mappedBy = "parentEmail", cascade = CascadeType.ALL)
    private List<Email> replies = new ArrayList<>();

}
