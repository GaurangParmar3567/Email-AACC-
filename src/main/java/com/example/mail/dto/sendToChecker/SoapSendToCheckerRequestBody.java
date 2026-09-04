package com.example.mail.dto.sendToChecker;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SoapSendToCheckerRequestBody {
    @XmlElement(name = "SendToChecker", namespace = "http://tempuri.org/")
    private SendToCheckerRequest sendToChecker;
}
