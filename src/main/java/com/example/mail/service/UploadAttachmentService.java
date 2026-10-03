package com.example.mail.service;

import com.example.mail.dto.soap.aacc.UploadAttachmentRequest;
import com.example.mail.dto.soap.aacc.UploadAttachmentResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
public class UploadAttachmentService {

    private static final Logger log = LoggerFactory.getLogger("MAIL_SERVICES_AVAAYA_LOGGER");

    @Value("${upload.path:C:/Uploads/}")
    private String baseUploadPath;

    public UploadAttachmentResponse uploadAttachment(UploadAttachmentRequest request) {
        log.info("uploadAttachment() - Starting file upload process");
        UploadAttachmentResponse objResult = new UploadAttachmentResponse();
        objResult.setMessageDetails("UploadAttachment Triggered");

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
            File dir = new File(strPath);
            if (!dir.exists()) {
                dir.mkdirs();
            }
        }

        String fileName = cleanField(request.getStrFileName());
        strPath = strPath + fileName;

        try {
            Path path = Paths.get(strPath);
            if (path.getParent() != null && !Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
            }
            if (request.getStrContent() != null) {
                Files.write(path, request.getStrContent());
            }

            objResult.setResultID(0);
            objResult.setMessageDetails("Success");
            log.info("uploadAttachment() - File successfully saved to: {}", strPath);
        } catch (Exception ex) {
            objResult.setResultID(-1);
            objResult.setMessageDetails("Error : " + ex.getMessage());
            log.error("uploadAttachment() - Upload failed for path: " + strPath, ex);
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