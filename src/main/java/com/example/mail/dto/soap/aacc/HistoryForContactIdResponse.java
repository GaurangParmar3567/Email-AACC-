package com.example.mail.dto.soap.aacc;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class HistoryForContactIdResponse {
    @XmlElement(name = "GetHistoryForContactIDResult", namespace = "http://tempuri.org/")
    private String result;

    public void setResult(String result) {
        this.result = result;
    }
}
