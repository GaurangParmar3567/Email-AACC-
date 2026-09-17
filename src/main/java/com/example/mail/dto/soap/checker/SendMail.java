package com.example.mail.dto.soap.checker;
import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "SendMail", namespace = "http://tempuri.org/")
public class SendMail {
    @XmlElement(name = "objMail", namespace = "http://tempuri.org/")
    private ObjMail objMail;
}
