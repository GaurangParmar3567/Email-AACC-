package com.example.mail.dto.soapContact;

import lombok.Data;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class GetContactResult {
    @XmlElement(name = "ContactID")
    private Long contactId;

    @XmlElement(name = "Message")
    private String message;
}