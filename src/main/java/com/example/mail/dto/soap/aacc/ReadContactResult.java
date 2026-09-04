package com.example.mail.dto.soap.aacc;

import com.example.mail.dto.soap.common.AgentDTO;
import com.example.mail.dto.soap.common.MillisecondsDTO;
import com.example.mail.dto.soap.common.SkillsetDTO;

import lombok.Data;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class ReadContactResult {

    @XmlElement(name = "id")
    private Long id;

    @XmlElement(name = "customerID")
    private Long customerID;

    @XmlElement(name = "originalSubject")
    private String originalSubject;

    @XmlElement(name = "source")
    private String source;

    @XmlElement(name = "status")
    private String status;

    @XmlElement(name = "skillset")
    private SkillsetDTO skillset;

    @XmlElement(name = "priority")
    private String priority;

    @XmlElement(name = "timezone")
    private Integer timezone;

    @XmlElement(name = "arrivalTime")
    private MillisecondsDTO arrivalTime;

    @XmlElement(name = "openTime")
    private MillisecondsDTO openTime;

    @XmlElement(name = "openDuration")
    private Integer openDuration;

    @XmlElement(name = "MailTo")
    private String mailTo = "";

    @XmlElement(name = "MailFrom")
    private String mailFrom = "";

    @XmlElement(name = "MailCC")
    private String mailCc = "";

    @XmlElement(name = "contactType")
    private String contactType;

    @XmlElement(name = "agent")
    private AgentDTO agent;

    @XmlElement(name = "actionList")
    private ActionListDTO actionList;
}