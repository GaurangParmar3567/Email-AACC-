package com.example.mail.dto.soapnew;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import java.util.ArrayList;
import java.util.List;

@XmlAccessorType(XmlAccessType.FIELD)
public class GetAllClosedReasonCodesResult {

    @XmlElement(name = "AWClosedReasonCode")
    private List<AWClosedReasonCode> awClosedReasonCodes = new ArrayList<>();

    public List<AWClosedReasonCode> getAwClosedReasonCodes() {
        return awClosedReasonCodes;
    }

    public void setAwClosedReasonCodes(List<AWClosedReasonCode> awClosedReasonCodes) {
        this.awClosedReasonCodes = awClosedReasonCodes;
    }
}
