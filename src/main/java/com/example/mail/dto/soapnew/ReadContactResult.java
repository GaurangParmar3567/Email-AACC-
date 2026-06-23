package com.example.mail.dto.soapnew;

import lombok.Data;

import javax.xml.bind.annotation.*;

@XmlAccessorType(XmlAccessType.FIELD)
@Data
public class ReadContactResult {

    private static final String DT_NS = "http://datatypes.agentwebservices.ccmm.applications.nortel.com";

    @XmlElement(name = "id", namespace = DT_NS)
    private Long id;

    @XmlElement(name = "customerID", namespace = DT_NS)
    private Long customerID;

    @XmlElement(name = "originalSubject", namespace = DT_NS)
    private String originalSubject;

    @XmlElement(name = "source", namespace = DT_NS)
    private String source;

    @XmlElement(name = "status", namespace = DT_NS)
    private String status;

    @XmlElement(name = "skillset", namespace = DT_NS)
    private SkillsetDTO skillset;

    @XmlElement(name = "priority", namespace = DT_NS)
    private String priority;

    @XmlElement(name = "timezone", namespace = DT_NS)
    private Integer timezone;

    @XmlElement(name = "arrivalTime", namespace = DT_NS)
    private MillisecondsDTO arrivalTime;

    @XmlElement(name = "openTime", namespace = DT_NS)
    private MillisecondsDTO openTime;

    @XmlElement(name = "openDuration", namespace = DT_NS)
    private Integer openDuration;

    @XmlElement(name = "MailTo", namespace = DT_NS)
    private String mailTo = "";

    @XmlElement(name = "MailFrom", namespace = DT_NS)
    private String mailFrom = "";

    @XmlElement(name = "MailCC", namespace = DT_NS)
    private String mailCc = "";

    @XmlElement(name = "contactType", namespace = DT_NS)
    private String contactType;

    @XmlElement(name = "agent", namespace = DT_NS)
    private AgentDTO agent;

    @XmlElement(name = "actionList", namespace = DT_NS)
    private ActionListDTO actionList;
}