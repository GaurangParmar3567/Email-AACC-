package com.example.mail.dto.soapnew;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class SoapRequestBody {

    @XmlElement(name = "ReadContact", namespace = "http://nortel.com/CCMMAgentWebservices/")
    private ReadContactRequest readContact;

    public ReadContactRequest getReadContact() { return readContact; }
    public void setReadContact(ReadContactRequest readContact) { this.readContact = readContact; }
}
