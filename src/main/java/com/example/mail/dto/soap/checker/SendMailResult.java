package com.example.mail.dto.soap.checker;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class SendMailResult {
    @XmlElement(name = "ResultID", namespace = "http://tempuri.org/")
    private int resultID;

    @XmlElement(name = "MessageDetails", namespace = "http://tempuri.org/")
    private String messageDetails;
}
