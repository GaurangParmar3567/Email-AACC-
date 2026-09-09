package com.example.mail.dto.soap.sessionauth;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "AgentLoginRequest")
@XmlAccessorType(XmlAccessType.FIELD)
public class AgentLoginRequest {

    private String agentLoginId;
    private String agentLoginPwd;
    private String webServiceIp;
    private String agentSessionId;
    private Integer agentId;
    private String ipAddress;
    private String deviceName;
    private String tokenId;

    public String getAgentLoginId() {
        return agentLoginId;
    }

    public void setAgentLoginId(String agentLoginId) {
        this.agentLoginId = agentLoginId;
    }

    public String getAgentLoginPwd() {
        return agentLoginPwd;
    }

    public void setAgentLoginPwd(String agentLoginPwd) {
        this.agentLoginPwd = agentLoginPwd;
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
