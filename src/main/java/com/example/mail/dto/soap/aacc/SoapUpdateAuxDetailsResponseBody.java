package com.example.mail.dto.soap.aacc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@NoArgsConstructor
@AllArgsConstructor
public class SoapUpdateAuxDetailsResponseBody {

    @XmlElement(name = "UpdateAuxDetailsResponse", namespace = "http://tempuri.org/")
    private UpdateAuxDetailsResponse updateAuxDetailsResponse;
}

