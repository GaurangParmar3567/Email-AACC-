package com.example.mail.dto.soap.aacc;

import lombok.Data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class AWAttachmentDTO {
    
    @XmlElement(name = "id")
    private Long id;

    @XmlElement(name = "idSpecified")
    private Boolean idSpecified = true;

    @XmlElement(name = "displayFileName")
    private String displayFileName;

    @XmlElement(name = "internalFileName")
    private String internalFileName;

    @XmlElement(name = "fileContents")
    private String fileContents = "";
}
