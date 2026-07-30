package com.example.mail.dto.soapnew;

import javax.xml.bind.annotation.*;

@XmlRootElement(name = "Envelope", namespace = "http://schemas.xmlsoap.org/soap/envelope/")
@XmlAccessorType(XmlAccessType.FIELD)
public class SoapRequestEnvelope {

    @XmlElement(name = "Body", namespace = "http://schemas.xmlsoap.org/soap/envelope/")
    private SoapRequestBody body;

    public SoapRequestBody getBody() { return body; }
    public void setBody(SoapRequestBody body) { this.body = body; }
}
