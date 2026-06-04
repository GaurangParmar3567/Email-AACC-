package com.example.mail;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class AttachmentInfoDTO {
    private Long id;
    private String fileName;
    private String mimeType;
    private long fileSize;
    private String downloadUrl;
}
