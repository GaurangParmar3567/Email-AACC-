package com.example.mail.dto.soap.aacc;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "GetAllSkillsetsResponse", namespace = "")
public class GetAllSkillsetsResponse {

    @XmlElement(name = "GetAllSkillsetsResult")
    private GetAllSkillsetsResult getAllSkillsetsResult;

    public GetAllSkillsetsResult getGetAllSkillsetsResult() {
        return getAllSkillsetsResult;
    }

    public void setGetAllSkillsetsResult(GetAllSkillsetsResult getAllSkillsetsResult) {
        this.getAllSkillsetsResult = getAllSkillsetsResult;
    }
}
