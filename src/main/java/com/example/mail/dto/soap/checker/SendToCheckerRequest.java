package com.example.mail.dto.soap.checker;

import com.example.mail.dto.soap.checker.ObjMail;
import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SendToCheckerRequest {
    @XmlElement(name = "objMail", namespace = "http://tempuri.org/")
    private ObjMail objMail;
}
