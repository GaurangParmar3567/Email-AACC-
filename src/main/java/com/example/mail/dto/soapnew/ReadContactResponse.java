package com.example.mail.dto.soapnew;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "ReadContactResponse")
public class ReadContactResponse {

    @XmlElement(
            name = "ReadContactResult"
    )
    private ReadContactResult readContactResult;

    public ReadContactResult getReadContactResult() { return readContactResult; }
    public void setReadContactResult(ReadContactResult r) { this.readContactResult = r; }
}
