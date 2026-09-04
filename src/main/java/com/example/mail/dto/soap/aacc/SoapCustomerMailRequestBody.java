package com.example.mail.dto.soap.aacc;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SoapCustomerMailRequestBody {
    @XmlElement(name = "GetCustomerMailforMaker", namespace = "http://tempuri.org/")
    private CustomerMailRequest getCustomerMailforMaker;

    @XmlElement(name = "GetCustomerMailforChecker", namespace = "http://tempuri.org/")
    private CustomerMailRequest getCustomerMailforChecker;
}