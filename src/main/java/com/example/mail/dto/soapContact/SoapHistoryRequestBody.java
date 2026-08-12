package com.example.mail.dto.soapContact;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class SoapHistoryRequestBody {

    @XmlElement(name = "GetHistoryFromAACC", namespace = "http://tempuri.org/")
    private GetHistoryFromAACCRequest getHistoryFromAACC;

    public GetHistoryFromAACCRequest getGetHistoryFromAACC() {
        return getHistoryFromAACC;
    }

    public void setGetHistoryFromAACC(GetHistoryFromAACCRequest getHistoryFromAACC) {
        this.getHistoryFromAACC = getHistoryFromAACC;
    }
}
