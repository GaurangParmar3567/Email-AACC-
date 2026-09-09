package com.example.mail.dto.soap.aacc;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class HistoryForContactIdRequest {
    @XmlElement(name = "contactID", namespace = "http://tempuri.org/")
    private String contactId;

    @XmlElement(name = "sessionKey", namespace = "http://tempuri.org/")
    private String sessionKey;

    public String getContactId() {
        return contactId;
    }

    public String getSessionKey() {
        return sessionKey;
    }
}
