package com.example.mail.model;

import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Entity
@Table(name = "aux_details")
public class UpdateAuxDetails {

    @Id
    private String agentID;
    private String auxCode;
    private String startTime;
    private String endTime;

    public UpdateAuxDetails() {}

    public String getAgentID() { return agentID; }
    public void setAgentID(String agentID) { this.agentID = agentID; }

    public String getAuxCode() { return auxCode; }
    public void setAuxCode(String auxCode) { this.auxCode = auxCode; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public String getEndTime() { return endTime; }
    public void setEndTime(String endTime) { this.endTime = endTime; }
}