package com.example.mail.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Entity
@Table(name = "AgentAuthorizationBKP")
public class GetAgentSessionKey {

    @Id
    private String sessionKey;
    private String avayaAgentID;
    private LocalDateTime loginTime;

    public GetAgentSessionKey() {}

    public String getSessionKey() { return sessionKey; }
    public void setSessionKey(String sessionKey) { this.sessionKey = sessionKey; }

    public String getAvayaAgentID() { return avayaAgentID; }
    public void setAvayaAgentID(String avayaAgentID) { this.avayaAgentID = avayaAgentID; }

    public LocalDateTime getLoginTime() { return loginTime; }
    public void setLoginTime(LocalDateTime loginTime) { this.loginTime = loginTime; }
}