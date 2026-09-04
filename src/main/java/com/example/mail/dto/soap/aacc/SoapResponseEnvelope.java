package com.example.mail.dto.soap.aacc;

import javax.xml.bind.annotation.*;

@XmlRootElement(name = "Envelope", namespace = "http://schemas.xmlsoap.org/soap/envelope/")
@XmlAccessorType(XmlAccessType.FIELD)
public class SoapResponseEnvelope {

    @XmlElement(name = "Body", namespace = "http://schemas.xmlsoap.org/soap/envelope/")
    private SoapResponseBody body;

    public SoapResponseBody getBody() {
        return body;
    }

    public void setBody(SoapResponseBody body) {
        this.body = body;
    }
}