package com.example.mail.dto.soap.aacc;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
@XmlRootElement(name = "DeleteAgentSessionKey", namespace = "")
public class DeleteAgentSessionKeyRequest {

    private String strAvayaAgentID;
    private String strSessionKey;

    public DeleteAgentSessionKeyRequest() {}

    @XmlElement(name = "strAvayaAgentID")
    public String getStrAvayaAgentID() { return strAvayaAgentID; }
    public void setStrAvayaAgentID(String strAvayaAgentID) { this.strAvayaAgentID = strAvayaAgentID; }

    @XmlElement(name = "strSessionKey")
    public String getStrSessionKey() { return strSessionKey; }
    public void setStrSessionKey(String strSessionKey) { this.strSessionKey = strSessionKey; }
}