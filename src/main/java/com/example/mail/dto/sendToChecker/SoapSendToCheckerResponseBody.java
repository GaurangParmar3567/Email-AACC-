package com.example.mail.dto.sendToChecker;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SoapSendToCheckerResponseBody {
    @XmlElement(name = "SendToCheckerResponse", namespace = "http://tempuri.org/")
    private SendToCheckerResult sendToCheckerResponse;
}
