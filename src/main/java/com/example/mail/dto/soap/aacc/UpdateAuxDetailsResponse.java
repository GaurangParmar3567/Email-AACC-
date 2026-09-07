package com.example.mail.dto.soap.aacc;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

@Data
@XmlRootElement(name = "UpdateAuxDetailsResponse", namespace = "")
public class UpdateAuxDetailsResponse {

    private String message;

    public UpdateAuxDetailsResponse() {}

    public UpdateAuxDetailsResponse(String message) {
        this.message = message;
    }

    @XmlElement(name = "Message")
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}