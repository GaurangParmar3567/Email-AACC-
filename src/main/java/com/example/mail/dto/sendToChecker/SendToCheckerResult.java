package com.example.mail.dto.sendToChecker;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@Data
@XmlRootElement(name = "SendToCheckerResponse", namespace = "http://tempuri.org/")
@XmlAccessorType(XmlAccessType.FIELD)
public class SendToCheckerResult {
    @XmlElement(name = "SendToCheckerResult")
    private ResultPayload result = new ResultPayload();

    public void setMailId(Long mailId) {
        result.setMailId(mailId);
    }

    public void setMessage(String message) {
        result.setMessage(message);
    }

    @Data
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class ResultPayload {
        @XmlElement(name = "MailId")
        private Long mailId;

        @XmlElement(name = "Message")
        private String message;
    }
}
