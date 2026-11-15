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
@XmlRootElement(name = "UnableToReadContact", namespace = "http://tempuri.org/")
public class UnableToReadContactRequest {

    @XmlElement(name = "strAgentID", namespace = "http://tempuri.org/")
    private String strAgentID;

    @XmlElement(name = "strContactID", namespace = "http://tempuri.org/")
    private String strContactID;

    @XmlElement(name = "strError", namespace = "http://tempuri.org/")
    private String strError;
}

