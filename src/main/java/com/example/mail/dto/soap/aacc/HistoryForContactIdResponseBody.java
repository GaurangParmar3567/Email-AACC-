package com.example.mail.dto.soap.aacc;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class HistoryForContactIdResponseBody {
    @XmlElement(name = "GetHistoryForContactIDResponse", namespace = "http://tempuri.org/")
    private HistoryForContactIdResponse response = new HistoryForContactIdResponse();

    public HistoryForContactIdResponse getResponse() {
        return response;
    }
}
