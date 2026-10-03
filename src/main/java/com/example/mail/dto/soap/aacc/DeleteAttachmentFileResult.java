package com.example.mail.dto.soap.aacc;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@XmlAccessorType(XmlAccessType.FIELD)
@XmlRootElement(name = "DeleteAttachmentFileResult", namespace = "http://tempuri.org/")
public class DeleteAttachmentFileResult {

    @XmlElement(name = "ResultID", namespace = "http://tempuri.org/")
    private int resultID;

    @XmlElement(name = "MessageDetails", namespace = "http://tempuri.org/")
    private String messageDetails;
}
