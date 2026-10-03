package com.example.mail.dto.soap.aacc;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "GetAgentSessionKeyResult", namespace = "http://tempuri.org/")
public class GetAgentSessionKeyResult {

    @XmlElement(name = "AvayaAgentID", namespace = "http://tempuri.org/")
    private String avayaAgentID;

    @XmlElement(name = "SessionKey", namespace = "http://tempuri.org/")
    private String sessionKey;

    @XmlElement(name = "Message", namespace = "http://tempuri.org/")
    private String message;
}
