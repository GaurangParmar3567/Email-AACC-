package com.example.mail.dto.soap.aacc;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class GetAllSkillsetsRequest {

    @XmlElement(name = "sessionKey", namespace = "http://nortel.com/CCMMAgentWebservices/")
    private String sessionKey;

    @XmlElement(name = "skillsetName", namespace = "http://nortel.com/CCMMAgentWebservices/")
    private String skillsetName;

    @XmlElement(name = "skillsetName")
    private String skillsetNameNoNs;

    @XmlElement(name = "name", namespace = "http://nortel.com/CCMMAgentWebservices/")
    private String name;

    @XmlElement(name = "name")
    private String nameNoNs;

    @XmlElement(name = "skillName", namespace = "http://nortel.com/CCMMAgentWebservices/")
    private String skillName;

    @XmlElement(name = "skillName")
    private String skillNameNoNs;

    public String getSessionKey() {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey) {
        this.sessionKey = sessionKey;
    }

    public String getSkillsetName() {
        return skillsetName;
    }

    public void setSkillsetName(String skillsetName) {
        this.skillsetName = skillsetName;
    }

    public String getSkillsetNameNoNs() {
        return skillsetNameNoNs;
    }

    public void setSkillsetNameNoNs(String skillsetNameNoNs) {
        this.skillsetNameNoNs = skillsetNameNoNs;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getNameNoNs() {
        return nameNoNs;
    }

    public void setNameNoNs(String nameNoNs) {
        this.nameNoNs = nameNoNs;
    }

    public String getSkillName() {
        return skillName;
    }

    public void setSkillName(String skillName) {
        this.skillName = skillName;
    }

    public String getSkillNameNoNs() {
        return skillNameNoNs;
    }

    public void setSkillNameNoNs(String skillNameNoNs) {
        this.skillNameNoNs = skillNameNoNs;
    }

    public String getEffectiveSkillsetName() {
        if (skillsetName != null && !skillsetName.trim().isEmpty()) {
            return skillsetName.trim();
        }
        if (skillsetNameNoNs != null && !skillsetNameNoNs.trim().isEmpty()) {
            return skillsetNameNoNs.trim();
        }
        if (skillName != null && !skillName.trim().isEmpty()) {
            return skillName.trim();
        }
        if (skillNameNoNs != null && !skillNameNoNs.trim().isEmpty()) {
            return skillNameNoNs.trim();
        }
        if (name != null && !name.trim().isEmpty()) {
            return name.trim();
        }
        if (nameNoNs != null && !nameNoNs.trim().isEmpty()) {
            return nameNoNs.trim();
        }
        return null;
    }
}
