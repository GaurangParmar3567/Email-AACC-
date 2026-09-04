package com.example.mail.dto.soap.aacc;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "GetHistoryFromAACCResponse", namespace = "")
public class GetHistoryFromAACCResponse {

    @XmlElement(name = "GetHistoryFromAACCResult")
    private GetHistoryFromAACCResult getHistoryFromAACCResult;

    public GetHistoryFromAACCResult getGetHistoryFromAACCResult() {
        return getHistoryFromAACCResult;
    }

    public void setGetHistoryFromAACCResult(GetHistoryFromAACCResult getHistoryFromAACCResult) {
        this.getHistoryFromAACCResult = getHistoryFromAACCResult;
    }
}
