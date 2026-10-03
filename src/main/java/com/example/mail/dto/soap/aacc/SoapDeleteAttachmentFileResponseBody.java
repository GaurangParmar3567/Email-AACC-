package com.example.mail.dto.soap.aacc;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
@NoArgsConstructor
@AllArgsConstructor
public class SoapDeleteAttachmentFileResponseBody {

    @XmlElement(name = "DeleteAttachmentFileResponse", namespace = "http://tempuri.org/")
    private DeleteAttachmentFileResponse deleteAttachmentFileResponse;
}
