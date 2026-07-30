package com.example.mail.dto.sendMail;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class ObjMail {
    @XmlElement(name = "MailId")
    private Long mailId;
    @XmlElement(name = "FromEmail")
    private String fromEmail;
    @XmlElement(name = "ToEmail")
    private String toEmail;
    @XmlElement(name = "CCEmail")
    private String ccEmail;
    @XmlElement(name = "BCCEmail")
    private String bccEmail;
    @XmlElement(name = "Subject")
    private String subject;
    @XmlElement(name = "BodyContent")
    private String bodyContent;
    @XmlElementWrapper(name = "AttachmentFiles")
    @XmlElement(name = "string")
    private List<String> attachmentFiles;
    @XmlElement(name = "ContactID")
    private String contactId;
    @XmlElement(name = "AgentID")
    private String agentId;
    @XmlElement(name = "ClosedReason")
    private String closedReason;
    @XmlElement(name = "Comment")
    private String comment;
    @XmlElement(name = "Remarks")
    private String remarks;
    @XmlElement(name = "IsSent")
    private Boolean isSent;
    @XmlElement(name = "ActionID")
    private Long actionId;
    @XmlElement(name = "AnsweredDateTime")
    private String answeredDateTime;
    @XmlElement(name = "SKILLSET")
    private Integer skillset;
}
