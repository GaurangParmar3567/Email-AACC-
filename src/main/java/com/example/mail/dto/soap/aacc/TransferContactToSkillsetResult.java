package com.example.mail.dto.soap.aacc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Data
@NoArgsConstructor
@AllArgsConstructor
@XmlRootElement(name = "TransferContactToSkillsetResult", namespace = "")
@XmlAccessorType(XmlAccessType.FIELD)
public class TransferContactToSkillsetResult {
    @XmlElement(name = "TransferContactToSkillsetResult", namespace = "")
    private Long value;
}
