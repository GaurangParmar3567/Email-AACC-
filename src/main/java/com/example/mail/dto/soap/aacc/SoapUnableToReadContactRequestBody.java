package com.example.mail.dto.soap.aacc;

import lombok.Data;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SoapUnableToReadContactRequestBody {

    @XmlElement(name = "UnableToReadContact", namespace = "http://tempuri.org/")
    private UnableToReadContactRequest unableToReadContact;
}

