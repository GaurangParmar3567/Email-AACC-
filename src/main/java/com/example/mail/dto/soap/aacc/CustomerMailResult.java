package com.example.mail.dto.soap.aacc;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class CustomerMailResult {
    @XmlElement(name = "Message", namespace = "")
    private String message;

    @XmlElement(name = "ReplyText", namespace = "")
    private String replyText;

    @XmlElement(name = "ClosedReason", namespace = "")
    private String closedReason;

    @XmlElement(name = "Comment", namespace = "")
    private String comment;

    @XmlElement(name = "GetError", namespace = "")
    private String getError;
}