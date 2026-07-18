package com.example.mail.model;

import lombok.*;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "contact_actions")
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ContactAction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "action_id", nullable = false)
    private Long actionId;  // Maps to <id>16041775</id> in XML

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "contact_id")
    private Email contact;

    @Column(name = "contact_id", insertable = false, updatable = false)
    private Long contactId;

    @Lob
    private String subject;

    @Lob
    private String textContent;

    @Lob
    private String textHtml;

    @Column(name = "callback_status", length = 100)
    private String callbackStatus;  // Unspecified, Contact_Transferred, etc.

    @Lob
    private String source;  // EMail_from_Customer, Transfer_to_Skillset, etc.

    @Lob
    private String comment;

    @Column(name = "agent_id")
    private Long agentId;

    @Column(name = "agent_first_name")
    private String agentFirstName;

    @Column(name = "agent_last_name")
    private String agentLastName;

    @Column(name = "time_allocated")
    private Integer timeAllocated;

    @Column(name = "outbound_talk_time")
    private Integer outboundTalkTime;

    @Column(name = "outbound_disposition_code")
    private String outboundDispositionCode;

    @Column(name = "action_type")
    private String actionType;  // Email, Other, Unspecified

    @Column(name = "closed_reason_name")
    private String closedReasonName;

    @Column(name = "closed_reason_numeric_value")
    private Integer closedReasonNumericValue;

    @Column(name = "creation_time")
    private Long creationTime;  // milliseconds

    @Column(name = "mail_to")
    @Lob
    private String mailTo;

    @Column(name = "mail_from")
    @Lob
    private String mailFrom;

    @Column(name = "mail_cc")
    @Lob
    private String mailCc;

    @OneToMany(mappedBy = "contactAction", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Attachment> attachments = new ArrayList<>();

}
