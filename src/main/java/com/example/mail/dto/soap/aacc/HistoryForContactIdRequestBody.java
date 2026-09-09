package com.example.mail.dto.soap.aacc;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class HistoryForContactIdRequestBody {
    @XmlElement(name = "GetHistoryForContactID", namespace = "http://tempuri.org/")
    private HistoryForContactIdRequest request;

    public HistoryForContactIdRequest getRequest() {
        return request;
    }
}
