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
@XmlRootElement(name = "UpdateAuxDetailsResponse", namespace = "http://tempuri.org/")
public class UpdateAuxDetailsResponse {

    @XmlElement(name = "Message", namespace = "http://tempuri.org/")
    private String message;

    @XmlElement(name = "UpdateAuxDetailsResult", namespace = "http://tempuri.org/")
    private String updateAuxDetailsResult;

    public UpdateAuxDetailsResponse(String message) {
        this.message = message;
        this.updateAuxDetailsResult = message;
    }
}