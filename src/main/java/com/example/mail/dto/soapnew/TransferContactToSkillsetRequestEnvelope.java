package com.example.mail.dto.soapnew;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Data
@XmlRootElement(name = "Envelope", namespace = "http://schemas.xmlsoap.org/soap/envelope/")
@XmlAccessorType(XmlAccessType.FIELD)
public class TransferContactToSkillsetRequestEnvelope {
    @XmlElement(name = "Body", namespace = "http://schemas.xmlsoap.org/soap/envelope/")
    private TransferContactToSkillsetRequestBody body;
}
