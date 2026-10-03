package com.example.mail.dto.soap.aacc;

import lombok.Data;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class SoapUploadAttachmentRequestBody {

    @XmlElement(name = "UploadAttachment", namespace = "http://tempuri.org/")
    private UploadAttachmentRequest uploadAttachment;
}
