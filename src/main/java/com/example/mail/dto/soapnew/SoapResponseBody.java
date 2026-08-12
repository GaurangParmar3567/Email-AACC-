package com.example.mail.dto.soapnew;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class SoapResponseBody {

    @XmlElement(name = "ReadContactResponse")
    private ReadContactResponse readContactResponse;

    @XmlElement(name = "GetAllClosedReasonCodesResponse", namespace = "http://nortel.com/CCMMAgentWebservices/")
    private GetAllClosedReasonCodesResponse getAllClosedReasonCodesResponse;

    @XmlElement(name = "CloseContactResponse", namespace = "http://nortel.com/CCMMAgentWebservices/")
    private CloseContactResponse closeContactResponse;

    public ReadContactResponse getReadContactResponse() { return readContactResponse; }
    public void setReadContactResponse(ReadContactResponse r) { this.readContactResponse = r; }

    public GetAllClosedReasonCodesResponse getGetAllClosedReasonCodesResponse() {
        return getAllClosedReasonCodesResponse;
    }

    public void setGetAllClosedReasonCodesResponse(GetAllClosedReasonCodesResponse getAllClosedReasonCodesResponse) {
        this.getAllClosedReasonCodesResponse = getAllClosedReasonCodesResponse;
    }

    public CloseContactResponse getCloseContactResponse() {
        return closeContactResponse;
    }

    public void setCloseContactResponse(CloseContactResponse closeContactResponse) {
        this.closeContactResponse = closeContactResponse;
    }
}