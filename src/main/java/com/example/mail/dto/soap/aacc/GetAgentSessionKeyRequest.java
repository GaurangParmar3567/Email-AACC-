package com.example.mail.dto.soap.aacc;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.Data;

@Data 
@XmlRootElement(name = "GetAgentSessionKey", namespace = "")
public class GetAgentSessionKeyRequest {

    private String strAvayaAgentID;
    private String strSessionKey;

    public GetAgentSessionKeyRequest() {}

    @XmlElement(name = "strAvayaAgentID")
    public String getStrAvayaAgentID() { return strAvayaAgentID; }
    public void setStrAvayaAgentID(String strAvayaAgentID) { this.strAvayaAgentID = strAvayaAgentID; }

    @XmlElement(name = "strSessionKey")
    public String getStrSessionKey() { return strSessionKey; }
    public void setStrSessionKey(String strSessionKey) { this.strSessionKey = strSessionKey; }
}