package com.example.mail.dto.soap.aacc;
import lombok.Data;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "GetContactResponse", namespace = "")
@Data
public class GetContactResponse {
    @XmlElement(name = "GetContactResult")
    private GetContactResult getContactResult;
}