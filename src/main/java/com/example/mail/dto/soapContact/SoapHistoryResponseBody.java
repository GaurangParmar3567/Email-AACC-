package com.example.mail.dto.soapContact;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SoapHistoryResponseBody {

    @XmlElement(name = "GetHistoryFromAACCResponse", namespace = "http://tempuri.org/")
    private GetHistoryFromAACCResponse getHistoryFromAACCResponse;

}
