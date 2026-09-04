package com.example.mail.dto.soap.aacc;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "CloseContactResponse", namespace = "")
public class CloseContactResponse {

    @XmlElement(name = "CloseContactResult")
    private CloseContactResult closeContactResult;

    public CloseContactResult getCloseContactResult() { return closeContactResult; }
    public void setCloseContactResult(CloseContactResult closeContactResult) { this.closeContactResult = closeContactResult; }
}
