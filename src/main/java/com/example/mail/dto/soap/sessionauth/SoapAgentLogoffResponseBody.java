package com.example.mail.dto.soap.sessionauth;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@NoArgsConstructor
@AllArgsConstructor
public class SoapAgentLogoffResponseBody {

    @XmlElement(name = "AgentDesktopLogoffResponse", namespace = "http://nortel.com/CCMMAgentWebservices/")
    private AgentDesktopLogoffResponse agentDesktopLogoffResponse;
}

