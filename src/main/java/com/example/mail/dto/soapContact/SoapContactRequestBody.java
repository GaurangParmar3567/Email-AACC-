package com.example.mail.dto.soapContact;
import lombok.Data;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SoapContactRequestBody {
    @XmlElement(name = "GetContact", namespace = "http://tempuri.org/")
    private GetContactRequest GetContact;
}