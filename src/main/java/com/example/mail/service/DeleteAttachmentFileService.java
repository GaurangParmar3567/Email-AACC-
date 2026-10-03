package com.example.mail.service;

import com.example.mail.dto.soap.aacc.DeleteAttachmentFileRequest;
import com.example.mail.dto.soap.aacc.DeleteAttachmentFileResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;

@Service
public class DeleteAttachmentFileService {

    private static final Logger log = LoggerFactory.getLogger("MAIL_SERVICES_AVAAYA_LOGGER");

    @Value("${upload.path:C:/Uploads/}")
    private String baseUploadPath;

    public DeleteAttachmentFileResponse deleteAttachmentFile(DeleteAttachmentFileRequest request) {
        log.info("deleteAttachmentFile() - Starting file deletion process");
        DeleteAttachmentFileResponse objResult = new DeleteAttachmentFileResponse();
        objResult.setMessageDetails("DeleteAttachmentFile Triggered");

        String strPath = baseUploadPath != null ? baseUploadPath.trim() : "C:/Uploads/";
        if (!strPath.endsWith(File.separator) && !strPath.endsWith("/")) {
            strPath = strPath + File.separator;
        }

        String strLogicalPath = cleanField(request.getStrLogicalPath());
        if (strLogicalPath != null && strLogicalPath.length() > 0) {
            strPath = strPath + strLogicalPath;
            if (!strPath.endsWith(File.separator) && !strPath.endsWith("/")) {
                strPath = strPath + File.separator;
            }
        }

        String fileName = cleanField(request.getStrFileName());
        strPath = strPath + fileName;

        try {
            File fileToDelete = new File(strPath);
            if (fileToDelete.exists()) {
                boolean deleted = fileToDelete.delete();
                log.info("deleteAttachmentFile() - File deletion status: {} for path: {}", deleted, strPath);
            } else {
                log.info("deleteAttachmentFile() - File does not exist at path: {}", strPath);
            }

            objResult.setResultID(0);
            objResult.setMessageDetails("Success");
        } catch (Exception ex) {
            objResult.setResultID(-1);
            objResult.setMessageDetails("Error : " + ex.getMessage());
            log.error("deleteAttachmentFile() - Deletion failed for path: " + strPath, ex);
        }

        return objResult;
    }

    private String cleanField(String val) {
        if (val == null) return "";
        val = val.trim();
        if (val.startsWith("<![CDATA[") && val.endsWith("]]>")) {
            val = val.substring(9, val.length() - 3).trim();
        }
        return val;
    }
}