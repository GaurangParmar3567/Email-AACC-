package com.example.mail.dto.soapnew;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class TransferContactToSkillsetRequest {
    @XmlElement(name = "id", namespace = "http://nortel.com/CCMMAgentWebservices/")
    private String id;

    @XmlElement(name = "skillsetId", namespace = "http://nortel.com/CCMMAgentWebservices/")
    private Long skillsetId;

    @XmlElement(name = "note", namespace = "http://nortel.com/CCMMAgentWebservices/")
    private String note;

    @XmlElement(name = "sessionKey", namespace = "http://nortel.com/CCMMAgentWebservices/")
    private String sessionKey;
}
