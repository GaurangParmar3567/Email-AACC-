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
@XmlRootElement(name = "DeleteAgentSessionKey", namespace = "http://tempuri.org/")
public class DeleteAgentSessionKeyRequest {

    @XmlElement(name = "strAvayaAgentID", namespace = "http://tempuri.org/")
    private String strAvayaAgentID;

    @XmlElement(name = "strSessionKey", namespace = "http://tempuri.org/")
    private String strSessionKey;
}