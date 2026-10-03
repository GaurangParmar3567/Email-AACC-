package com.example.mail.dto.soap.sessionauth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "AgentDesktopLogoffResponse", namespace = "http://nortel.com/CCMMAgentWebservices/")
@XmlAccessorType(XmlAccessType.FIELD)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AgentDesktopLogoffResponse {

    @XmlElement(name = "AgentDesktopLogoffResult", namespace = "http://nortel.com/CCMMAgentWebservices/")
    private String agentDesktopLogoffResult;
}

