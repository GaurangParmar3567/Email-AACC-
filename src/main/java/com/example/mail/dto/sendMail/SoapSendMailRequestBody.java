package com.example.mail.dto.sendMail;
import lombok.Data;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SoapSendMailRequestBody {
    @XmlElement(name = "SendMail", namespace = "http://tempuri.org/")
    private SendMail sendMail;
}