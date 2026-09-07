package com.example.mail.dto.soap.aacc;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

@Data
@XmlRootElement(name = "GetAgentSessionKeyResult", namespace = "")
public class GetAgentSessionKeyResult {

    private String AvayaAgentID;
    private String SessionKey;
    private String message;
    
    public GetAgentSessionKeyResult() {}

    public GetAgentSessionKeyResult(String AvayaAgentID, String SessionKey,String message) {
        this.AvayaAgentID = AvayaAgentID;
        this.SessionKey = SessionKey;
        this.message = message;
    }
    
    @XmlElement(name = "AvayaAgentID")
    public String getAvayaAgentID() { return AvayaAgentID; }
    public void setAvayaAgentID(String AvayaAgentID) { this.AvayaAgentID = AvayaAgentID; }
    
    @XmlElement(name = "SessionKey")
    public String getSessionKey() { return SessionKey; }
    public void setSessionKey(String SessionKey) { this.SessionKey = SessionKey; }
    
    @XmlElement(name = "Message")
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}
