package com.example.mail.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "AgentAuthorization")
public class UpdateAgentContact {

    @Id
    @Column(name = "AvayaAgentID")
    private String agentID;

    @Column(name = "SessionKey")
    private String sessionKey;

    public UpdateAgentContact() {}

    public UpdateAgentContact(String agentID, String sessionKey) {
        this.agentID = agentID;
        this.sessionKey = sessionKey;
    }

    public String getAgentID() {
        return agentID;
    }

    public void setAgentID(String agentID) {
        this.agentID = agentID;
    }

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }
}
