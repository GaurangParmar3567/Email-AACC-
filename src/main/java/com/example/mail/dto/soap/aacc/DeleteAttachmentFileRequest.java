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
@XmlRootElement(name = "DeleteAttachmentFile", namespace = "http://tempuri.org/")
public class DeleteAttachmentFileRequest {

    @XmlElement(name = "strFileName", namespace = "http://tempuri.org/")
    private String strFileName;

    @XmlElement(name = "strLogicalPath", namespace = "http://tempuri.org/")
    private String strLogicalPath;
}