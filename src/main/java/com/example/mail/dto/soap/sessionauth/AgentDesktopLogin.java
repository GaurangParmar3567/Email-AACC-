package com.example.mail.dto.soap.sessionauth;

import lombok.Data;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "AgentDesktopLogin", namespace = "http://nortel.com/CCMMAgentWebservices/")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class AgentDesktopLogin {

    @XmlElement(name = "username", namespace = "http://nortel.com/CCMMAgentWebservices/")
    private String username;

    @XmlElement(name = "password", namespace = "http://nortel.com/CCMMAgentWebservices/")
    private String password;
}

