package com.example.mail.dto.soap.aacc;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class CustomerMailResponseBody {
    @XmlElement(name = "GetCustomerMailforMakerResponse", namespace = "")
    private MakerCustomerMailResponse getCustomerMailforMakerResponse;

    @XmlElement(name = "GetCustomerMailforCheckerResponse", namespace = "")
    private CheckerCustomerMailResponse getCustomerMailforCheckerResponse;
}