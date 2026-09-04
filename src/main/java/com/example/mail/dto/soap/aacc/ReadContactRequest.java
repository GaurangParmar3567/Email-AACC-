package com.example.mail.dto.soap.aacc;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import lombok.Data;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class ReadContactRequest {

    @XmlElement(name = "id", namespace = "http://nortel.com/CCMMAgentWebservices/")
    private Long id;

    @XmlElement(name = "sessionKey", namespace = "http://nortel.com/CCMMAgentWebservices/")
    private String sessionKey;
}
