package com.example.mail.dto.soap.checker;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "SendMailResponse", namespace = "http://tempuri.org/")
@Data
public class SendMailResponse {
    @XmlElement(name = "SendMailResult")
    private SendMailResult sendMailResult;
}
