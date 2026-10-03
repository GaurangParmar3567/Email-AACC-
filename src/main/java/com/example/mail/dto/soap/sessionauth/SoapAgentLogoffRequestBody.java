package com.example.mail.dto.soap.sessionauth;

import lombok.Data;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SoapAgentLogoffRequestBody {

    @XmlElement(name = "AgentDesktopLogoff", namespace = "http://nortel.com/CCMMAgentWebservices/")
    private AgentDesktopLogoff agentDesktopLogoff;
}

