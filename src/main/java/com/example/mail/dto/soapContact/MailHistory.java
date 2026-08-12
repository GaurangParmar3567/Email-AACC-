package com.example.mail.dto.soapContact;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class MailHistory {

    @XmlElement(name = "ContactId")
    private String contactId;

    @XmlElement(name = "CreatedTime")
    private String createdTime;

    @XmlElement(name = "Subject")
    private String subject;

    @XmlElement(name = "MailFrom")
    private String mailFrom;

    @XmlElement(name = "MailTo")
    private String mailTo;

    @XmlElement(name = "MailCC")
    private String mailCC;

    @XmlElement(name = "SkillSet")
    private String skillSet;

    @XmlElement(name = "ClosedReason")
    private String closedReason;

    @XmlElement(name = "Status")
    private String status;

    @XmlElement(name = "Message")
    private String message;

    public String getContactId() {
        return contactId;
    }

    public void setContactId(String contactId) {
        this.contactId = contactId;
    }

    public String getCreatedTime() {
        return createdTime;
    }

    public void setCreatedTime(String createdTime) {
        this.createdTime = createdTime;
    }

    public String getSubject() {
        return subject;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public String getMailFrom() {
        return mailFrom;
    }

    public void setMailFrom(String mailFrom) {
        this.mailFrom = mailFrom;
    }

    public String getMailTo() {
        return mailTo;
    }

    public void setMailTo(String mailTo) {
        this.mailTo = mailTo;
    }

    public String getMailCC() {
        return mailCC;
    }

    public void setMailCC(String mailCC) {
        this.mailCC = mailCC;
    }

    public String getSkillSet() {
        return skillSet;
    }

    public void setSkillSet(String skillSet) {
        this.skillSet = skillSet;
    }

    public String getClosedReason() {
        return closedReason;
    }

    public void setClosedReason(String closedReason) {
        this.closedReason = closedReason;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
