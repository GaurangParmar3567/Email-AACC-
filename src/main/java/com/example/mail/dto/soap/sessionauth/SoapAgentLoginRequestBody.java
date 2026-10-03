package com.example.mail.dto.soap.sessionauth;

import lombok.Data;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SoapAgentLoginRequestBody {

    @XmlElement(name = "AgentDesktopLogin", namespace = "http://nortel.com/CCMMAgentWebservices/")
    private AgentDesktopLogin agentDesktopLogin;
}

