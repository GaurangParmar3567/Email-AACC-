package com.example.mail.dto.soap.aacc;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@XmlAccessorType(XmlAccessType.FIELD)
public class SoapRequestBody {

    @XmlElement(name = "ReadContact", namespace = "http://nortel.com/CCMMAgentWebservices/")
    private ReadContactRequest readContact;

    @XmlElement(name = "GetAllClosedReasonCodes", namespace = "http://nortel.com/CCMMAgentWebservices/")
    private GetAllClosedReasonCodesRequest getAllClosedReasonCodes;

    @XmlElement(name = "CloseContact", namespace = "http://nortel.com/CCMMAgentWebservices/")
    private CloseContactRequest closeContact;

    public ReadContactRequest getReadContact() { return readContact; }
    public void setReadContact(ReadContactRequest readContact) { this.readContact = readContact; }

    public GetAllClosedReasonCodesRequest getGetAllClosedReasonCodes() {
        return getAllClosedReasonCodes;
    }

    public void setGetAllClosedReasonCodes(GetAllClosedReasonCodesRequest getAllClosedReasonCodes) {
        this.getAllClosedReasonCodes = getAllClosedReasonCodes;
    }

    public CloseContactRequest getCloseContact() {
        return closeContact;
    }

    public void setCloseContact(CloseContactRequest closeContact) {
        this.closeContact = closeContact;
    }
}
