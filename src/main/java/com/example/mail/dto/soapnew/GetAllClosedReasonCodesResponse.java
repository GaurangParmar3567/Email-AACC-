package com.example.mail.dto.soapnew;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "GetAllClosedReasonCodesResponse", namespace = "http://nortel.com/CCMMAgentWebservices/")
public class GetAllClosedReasonCodesResponse {

    @XmlElement(name = "GetAllClosedReasonCodesResult")
    private GetAllClosedReasonCodesResult getAllClosedReasonCodesResult;

    public GetAllClosedReasonCodesResult getGetAllClosedReasonCodesResult() {
        return getAllClosedReasonCodesResult;
    }

    public void setGetAllClosedReasonCodesResult(GetAllClosedReasonCodesResult getAllClosedReasonCodesResult) {
        this.getAllClosedReasonCodesResult = getAllClosedReasonCodesResult;
    }
}
