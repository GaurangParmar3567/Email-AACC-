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
@XmlRootElement(name = "UpdateAgentContactResponse", namespace = "http://tempuri.org/")
public class UpdateAgentContactResponse {

    @XmlElement(name = "UpdateAgentContactResult", namespace = "http://tempuri.org/")
    private UpdateAgentContactResult updateAgentContactResult;

    public UpdateAgentContactResponse(int contactResultID, String message) {
        this.updateAgentContactResult = new UpdateAgentContactResult(contactResultID, message);
    }

    public int getContactResultID() {
        return updateAgentContactResult != null ? updateAgentContactResult.getContactResultID() : 0;
    }

    public void setContactResultID(int contactResultID) {
        if (updateAgentContactResult == null) {
            updateAgentContactResult = new UpdateAgentContactResult();
        }
        updateAgentContactResult.setContactResultID(contactResultID);
    }

    public String getMessage() {
        return updateAgentContactResult != null ? updateAgentContactResult.getMessage() : null;
    }

    public void setMessage(String message) {
        if (updateAgentContactResult == null) {
            updateAgentContactResult = new UpdateAgentContactResult();
        }
        updateAgentContactResult.setMessage(message);
    }
}