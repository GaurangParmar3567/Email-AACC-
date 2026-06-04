package com.example.mail;

import javax.persistence.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity
@Table(name = "emails")
public class Email {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "message_id", unique = true, nullable = false)
    private String messageId;

    @Column(name = "in_reply_to")
    private String inReplyTo;

    private String sender;
    private String recipient;

    @Column(length = 4000)
    private String cc;

    @Column(length = 4000)
    private String bcc;

    private String subject;

    @Lob
    private String body;

    private boolean isHtml;

    @Temporal(TemporalType.TIMESTAMP)
    private Date receivedDate;

    // --- New Workflow & Routing Fields ---
    private boolean notToBeDownloaded;
    private boolean repeatFlag;
    private Long skillId;
    private boolean assigned;
    private boolean responded;

    // One email can have many attachments
    // In your Email.java entity
    @OneToMany(mappedBy = "email", cascade = CascadeType.ALL)
    private List<Attachment> attachments = new ArrayList<>(); // Initialize to empty list

    // One email can have multiple assignment history records
    @OneToMany(mappedBy = "email", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<EmailAssignment> assignments = new ArrayList<>();

    @Column(length = 4000)
    private String referencesHeader;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_email_id")
    private Email parentEmail;

    @OneToMany(mappedBy = "parentEmail", cascade = CascadeType.ALL)
    private List<Email> replies = new ArrayList<>();

    public Email() {
    }

    public Email(Long id, String messageId, String inReplyTo, String sender, String recipient, String cc, String bcc, String subject, String body, boolean isHtml, Date receivedDate, boolean notToBeDownloaded, boolean repeatFlag, Long skillId, boolean assigned, boolean responded, List<Attachment> attachments) {
        this.id = id;
        this.messageId = messageId;
        this.inReplyTo = inReplyTo;
        this.sender = sender;
        this.recipient = recipient;
        this.cc = cc;
        this.bcc = bcc;
        this.subject = subject;
        this.body = body;
        this.isHtml = isHtml;
        this.receivedDate = receivedDate;
        this.notToBeDownloaded = notToBeDownloaded;
        this.repeatFlag = repeatFlag;
        this.skillId = skillId;
        this.assigned = assigned;
        this.responded = responded;
        this.attachments = attachments;
    }

    // --- Standard Getters and Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMessageId() { return messageId; }
    public void setMessageId(String messageId) { this.messageId = messageId; }

    public String getInReplyTo() { return inReplyTo; }
    public void setInReplyTo(String inReplyTo) { this.inReplyTo = inReplyTo; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getRecipient() { return recipient; }
    public void setRecipient(String recipient) { this.recipient = recipient; }

    public String getCc() { return cc; }
    public void setCc(String cc) { this.cc = cc; }

    public String getBcc() { return bcc; }
    public void setBcc(String bcc) { this.bcc = bcc; }

    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }

    public String getBody() { return body; }
    public void setBody(String body) { this.body = body; }

    public boolean isHtml() { return isHtml; }
    public void setHtml(boolean html) { isHtml = html; }

    public Date getReceivedDate() { return receivedDate; }
    public void setReceivedDate(Date receivedDate) { this.receivedDate = receivedDate; }

    // --- Workflow Getters and Setters ---
    public boolean isNotToBeDownloaded() { return notToBeDownloaded; }
    public void setNotToBeDownloaded(boolean notToBeDownloaded) { this.notToBeDownloaded = notToBeDownloaded; }

    public boolean isRepeatFlag() { return repeatFlag; }
    public void setRepeatFlag(boolean repeatFlag) { this.repeatFlag = repeatFlag; }

    public Long getSkillId() { return skillId; }
    public void setSkillId(Long skillId) { this.skillId = skillId; }

    public boolean isAssigned() { return assigned; }
    public void setAssigned(boolean assigned) { this.assigned = assigned; }

    public boolean isResponded() { return responded; }
    public void setResponded(boolean responded) { this.responded = responded; }

    // --- Relationship Getters and Setters ---
    public List<Attachment> getAttachments() { return attachments; }
    public void setAttachments(List<Attachment> attachments) { this.attachments = attachments; }

    public List<EmailAssignment> getAssignments() { return assignments; }
    public void setAssignments(List<EmailAssignment> assignments) { this.assignments = assignments; }

    public String getReferencesHeader() { return referencesHeader; }
    public void setReferencesHeader(String referencesHeader) { this.referencesHeader = referencesHeader; }

    public Email getParentEmail() { return parentEmail; }
    public void setParentEmail(Email parentEmail) { this.parentEmail = parentEmail; }

    public List<Email> getReplies() { return replies; }
    public void setReplies(List<Email> replies) { this.replies = replies; }

    @Override
    public String toString() {
        return "Email{" +
                "id=" + id +
                ", messageId='" + messageId + '\'' +
                ", sender='" + sender + '\'' +
                ", subject='" + subject + '\'' +
                ", skillId=" + skillId +
                ", assigned=" + assigned +
                ", responded=" + responded +
                '}';
    }
}
