package com.example.mail.dto.soap.aacc;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "UpdateAuxDetails", namespace = "http://tempuri.org/")
public class UpdateAuxDetailsRequest {

    @XmlElement(name = "AgentID", namespace = "http://tempuri.org/")
    private String agentID;

    @XmlElement(name = "AuxCode", namespace = "http://tempuri.org/")
    private String auxCode;

    @XmlElement(name = "StartTime", namespace = "http://tempuri.org/")
    private String startTime;

    @XmlElement(name = "EndTime", namespace = "http://tempuri.org/")
    private String endTime;
}
