package com.example.mail.dto.soap.sessionauth;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "AgentLogoutRequest")
@XmlAccessorType(XmlAccessType.FIELD)
public class AgentLogoutRequest {

    private String agentSessionId;
    private String username;

    public String getAgentSessionId() {
        return agentSessionId;
    }

    public void setAgentSessionId(String agentSessionId) {
        this.agentSessionId = agentSessionId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
