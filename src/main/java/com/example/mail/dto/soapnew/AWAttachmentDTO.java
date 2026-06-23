package com.example.mail.dto.soapnew;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class AWAttachmentDTO {
    
    private Long id;
    private String displayFileName;
    private String internalFileName;
}
