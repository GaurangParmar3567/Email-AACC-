package com.example.mail.dto.soap.aacc;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "UpdateAuxDetails",namespace = "")
public class UpdateAuxDetailsRequest {

    private String agentID;
    private String auxCode;
    private String startTime;
    private String endTime;

    public UpdateAuxDetailsRequest() {}

    @XmlElement(name = "AgentID")
    public String getAgentID() { return agentID; }
    public void setAgentID(String agentID) { this.agentID = agentID; }

    @XmlElement(name = "AuxCode")
    public String getAuxCode() { return auxCode; }
    public void setAuxCode(String auxCode) { this.auxCode = auxCode; }

    @XmlElement(name = "StartTime")
    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    @XmlElement(name = "EndTime")
    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}
