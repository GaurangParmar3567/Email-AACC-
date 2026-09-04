package com.example.mail.dto.soap.checker;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import java.util.List;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class ObjMail {
    private static final String SOAP_NAMESPACE = "http://tempuri.org/";

    @XmlElement(name = "MailId", namespace = SOAP_NAMESPACE)
    private Long mailId;
    @XmlElement(name = "FromEmail", namespace = SOAP_NAMESPACE)
    private String fromEmail;
    @XmlElement(name = "ToEmail", namespace = SOAP_NAMESPACE)
    private String toEmail;
    @XmlElement(name = "CCEmail", namespace = SOAP_NAMESPACE)
    private String ccEmail;
    @XmlElement(name = "BCCEmail", namespace = SOAP_NAMESPACE)
    private String bccEmail;
    @XmlElement(name = "Subject", namespace = SOAP_NAMESPACE)
    private String subject;
    @XmlElement(name = "BodyContent", namespace = SOAP_NAMESPACE)
    private String bodyContent;
    @XmlElementWrapper(name = "AttachmentFiles", namespace = SOAP_NAMESPACE)
    @XmlElement(name = "string", namespace = SOAP_NAMESPACE)
    private List<String> attachmentFiles;
    @XmlElement(name = "ContactID", namespace = SOAP_NAMESPACE)
    private String contactId;
    @XmlElement(name = "AgentID", namespace = SOAP_NAMESPACE)
    private String agentId;
    @XmlElement(name = "ClosedReason", namespace = SOAP_NAMESPACE)
    private String closedReason;
    @XmlElement(name = "Comment", namespace = SOAP_NAMESPACE)
    private String comment;
    @XmlElement(name = "Remarks", namespace = SOAP_NAMESPACE)
    private String remarks;
    @XmlElement(name = "IsSent", namespace = SOAP_NAMESPACE)
    private Boolean isSent;
    @XmlElement(name = "ActionID", namespace = SOAP_NAMESPACE)
    private Long actionId;
    @XmlElement(name = "AnsweredDateTime", namespace = SOAP_NAMESPACE)
    private String answeredDateTime;
    @XmlElement(name = "SKILLSET", namespace = SOAP_NAMESPACE)
    private Integer skillset;
}
