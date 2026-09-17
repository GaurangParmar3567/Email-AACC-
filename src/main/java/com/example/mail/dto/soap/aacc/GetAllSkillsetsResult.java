package com.example.mail.dto.soap.aacc;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class GetAllSkillsetsResult {

    @XmlElement(name = "AWSkillset")
    private List<AWSkillset> awsSkillsets = new ArrayList<>();

    public List<AWSkillset> getAwsSkillsets() {
        return awsSkillsets;
    }

    public void setAwsSkillsets(List<AWSkillset> awsSkillsets) {
        this.awsSkillsets = awsSkillsets;
    }
}
