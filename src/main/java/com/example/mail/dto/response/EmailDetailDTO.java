package com.example.mail.dto.response;

import lombok.Data;

import java.util.Date;
import java.util.List;

@Data
public class EmailDetailDTO {
    private MessageMeta messageMeta;
    private RoutingAndSkill routingAndSkill;
    private AgentHandling agentHandling;
    private Participants participants;
    private Content content;
    private List<AttachmentDTO> attachments;

    @Data
    public static class MessageMeta {
        private Long id;
        private String messageId;
        private Long parentEmailId;
        private String inReplyTo;
        private String referencesHeader;
        private Date receivedDate;
        private Long arrivalTime;
    }

    @Data
    public static class RoutingAndSkill {
        private String source;
        private String status;
        private String priority;
        private Long skillsetId;
        private String skillsetName;
        private Long skillId;
        private Integer timezone;
        private boolean notToBeDownloaded;
        private boolean repeatFlag;
        private boolean assigned;
        private boolean responded;
    }

    @Data
    public static class AgentHandling {
        private Long agentId;
        private String agentFirstName;
        private String agentLastName;
        private Long openTime;
        private Integer openDuration;
    }

    @Data
    public static class Participants {
        private String sender;
        private String recipient;
        private String mailFrom;
        private String mailTo;
        private String cc;
        private String bcc;
    }

    @Data
    public static class Content {
        private String subject;
        private String originalSubject;
        private boolean isHtml;
        private String text;
        private String body;
        private String bodyHtml;
    }

    @Data
    public static class AttachmentDTO {
        private Long id;
        private String fileName;
        private String mimeType;
        private String fileDataBase64;
        private String fileSizeSummary;
    }
}
