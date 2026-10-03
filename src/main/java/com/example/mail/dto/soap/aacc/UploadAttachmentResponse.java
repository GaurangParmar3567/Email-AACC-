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
@XmlRootElement(name = "UploadAttachmentResponse", namespace = "http://tempuri.org/")
public class UploadAttachmentResponse {

    @XmlElement(name = "UploadAttachmentResult", namespace = "http://tempuri.org/")
    private UploadAttachmentResult uploadAttachmentResult;

    public UploadAttachmentResponse(int resultID, String messageDetails) {
        this.uploadAttachmentResult = new UploadAttachmentResult(resultID, messageDetails);
    }

    public int getResultID() {
        return uploadAttachmentResult != null ? uploadAttachmentResult.getResultID() : 0;
    }

    public void setResultID(int resultID) {
        if (uploadAttachmentResult == null) {
            uploadAttachmentResult = new UploadAttachmentResult();
        }
        uploadAttachmentResult.setResultID(resultID);
    }

    public String getMessageDetails() {
        return uploadAttachmentResult != null ? uploadAttachmentResult.getMessageDetails() : null;
    }

    public void setMessageDetails(String messageDetails) {
        if (uploadAttachmentResult == null) {
            uploadAttachmentResult = new UploadAttachmentResult();
        }
        uploadAttachmentResult.setMessageDetails(messageDetails);
    }
}