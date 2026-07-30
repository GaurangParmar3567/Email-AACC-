package com.example.mail.dto.sendMail;
import lombok.Data;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SoapSendMailResponseBody {
    @XmlElement(name = "SendMailResponse", namespace = "http://tempuri.org/")
    private SendMailResponse sendMailResponse;
}