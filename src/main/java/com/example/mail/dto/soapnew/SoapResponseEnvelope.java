package com.example.mail.dto.soapnew;

import javax.xml.bind.annotation.*;

@XmlRootElement(name = "Envelope")
@XmlAccessorType(XmlAccessType.FIELD)
public class SoapResponseEnvelope {

    @XmlElement(name = "Body")
    private SoapResponseBody body;

    public SoapResponseBody getBody() {
        return body;
    }

    public void setBody(SoapResponseBody body) {
        this.body = body;
    }
}