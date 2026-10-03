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
@XmlRootElement(name = "DeleteAttachmentFileResponse", namespace = "http://tempuri.org/")
public class DeleteAttachmentFileResponse {

    @XmlElement(name = "DeleteAttachmentFileResult", namespace = "http://tempuri.org/")
    private DeleteAttachmentFileResult deleteAttachmentFileResult;

    public DeleteAttachmentFileResponse(int resultID, String messageDetails) {
        this.deleteAttachmentFileResult = new DeleteAttachmentFileResult(resultID, messageDetails);
    }

    public int getResultID() {
        return deleteAttachmentFileResult != null ? deleteAttachmentFileResult.getResultID() : 0;
    }

    public void setResultID(int resultID) {
        if (deleteAttachmentFileResult == null) {
            deleteAttachmentFileResult = new DeleteAttachmentFileResult();
        }
        deleteAttachmentFileResult.setResultID(resultID);
    }

    public String getMessageDetails() {
        return deleteAttachmentFileResult != null ? deleteAttachmentFileResult.getMessageDetails() : null;
    }

    public void setMessageDetails(String messageDetails) {
        if (deleteAttachmentFileResult == null) {
            deleteAttachmentFileResult = new DeleteAttachmentFileResult();
        }
        deleteAttachmentFileResult.setMessageDetails(messageDetails);
    }
}