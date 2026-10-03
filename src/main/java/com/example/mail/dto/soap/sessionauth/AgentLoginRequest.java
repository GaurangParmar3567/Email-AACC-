package com.example.mail.dto.soap.sessionauth;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "AgentLoginRequest")
@XmlAccessorType(XmlAccessType.FIELD)
public class AgentLoginRequest {

    private String agentLoginId;
    private String agentLoginPwd;
    private String username;
    private String password;
    private String webServiceIp;
    private String agentSessionId;
    private Integer agentId;
    private String ipAddress;
    private String deviceName;
    private String tokenId;

    public String getAgentLoginId() {
        if (agentLoginId != null && !agentLoginId.trim().isEmpty()) {
            return agentLoginId;
        }
        return username;
    }

    public void setAgentLoginId(String agentLoginId) {
        this.agentLoginId = agentLoginId;
    }

    public String getAgentLoginPwd() {
        if (agentLoginPwd != null && !agentLoginPwd.trim().isEmpty()) {
            return agentLoginPwd;
        }
        return password;
    }

    public void setAgentLoginPwd(String agentLoginPwd) {
        this.agentLoginPwd = agentLoginPwd;
    }

    public String getUsername() {
        if (username != null && !username.trim().isEmpty()) {
            return username;
        }
        return agentLoginId;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        if (password != null && !password.trim().isEmpty()) {
            return password;
        }
        return agentLoginPwd;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getWebServiceIp() {
        return webServiceIp;
    }

    public void setWebServiceIp(String webServiceIp) {
        this.webServiceIp = webServiceIp;
    }

    public String getAgentSessionId() {
        return agentSessionId;
    }

    public void setAgentSessionId(String agentSessionId) {
        this.agentSessionId = agentSessionId;
    }

    public Integer getAgentId() {
        return agentId;
    }

    public void setAgentId(Integer agentId) {
        this.agentId = agentId;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public String getTokenId() {
        return tokenId;
    }

    public void setTokenId(String tokenId) {
        this.tokenId = tokenId;
    }
}
