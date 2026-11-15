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
@XmlRootElement(name = "UnableToReadContactResponse", namespace = "http://tempuri.org/")
public class UnableToReadContactResponse {

    @XmlElement(name = "UnableToReadContactResult", namespace = "http://tempuri.org/")
    private UnableToReadContactResult unableToReadContactResult;

    public UnableToReadContactResponse(int contactResultID, String message) {
        this.unableToReadContactResult = new UnableToReadContactResult(contactResultID, message);
    }

    public int getContactResultID() {
        return unableToReadContactResult != null ? unableToReadContactResult.getContactResultID() : 0;
    }

    public void setContactResultID(int contactResultID) {
        if (unableToReadContactResult == null) {
            unableToReadContactResult = new UnableToReadContactResult();
        }
        unableToReadContactResult.setContactResultID(contactResultID);
    }

    public String getMessage() {
        return unableToReadContactResult != null ? unableToReadContactResult.getMessage() : null;
    }

    public void setMessage(String message) {
        if (unableToReadContactResult == null) {
            unableToReadContactResult = new UnableToReadContactResult();
        }
        unableToReadContactResult.setMessage(message);
    }
}

