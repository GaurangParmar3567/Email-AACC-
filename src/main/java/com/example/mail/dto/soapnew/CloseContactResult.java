package com.example.mail.dto.soapnew;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlValue;

@XmlAccessorType(XmlAccessType.FIELD)
public class CloseContactResult {

    @XmlValue
    private Long value;

    public CloseContactResult() {}
    public CloseContactResult(Long value) { this.value = value; }

    public Long getValue() { return value; }
    public void setValue(Long value) { this.value = value; }
}
