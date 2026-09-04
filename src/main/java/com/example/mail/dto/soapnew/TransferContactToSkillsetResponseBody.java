package com.example.mail.dto.soapnew;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class TransferContactToSkillsetResponseBody {
    @XmlElement(name = "TransferContactToSkillsetResponse", namespace = "http://nortel.com/CCMMAgentWebservices/")
    private TransferContactToSkillsetResult transferContactToSkillsetResponse;
}
