package com.example.mail.dto.soap.sessionauth;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlValue;

@XmlRootElement(name = "AgentDesktopLoginResult")
@XmlAccessorType(XmlAccessType.FIELD)
public class AgentDesktopLoginResult {

    @XmlValue
    private String value;

    public AgentDesktopLoginResult() {
    }

    public AgentDesktopLoginResult(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
