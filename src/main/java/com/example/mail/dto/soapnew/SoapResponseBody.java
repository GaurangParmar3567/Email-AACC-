package com.example.mail.dto.soapnew;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class SoapResponseBody {

    @XmlElement(
            name = "ReadContactResponse",
            namespace = "http://nortel.com/CCMMAgentWebservices/"
    )
    private ReadContactResponse readContactResponse;

    public ReadContactResponse getReadContactResponse() { return readContactResponse; }
    public void setReadContactResponse(ReadContactResponse r) { this.readContactResponse = r; }
}