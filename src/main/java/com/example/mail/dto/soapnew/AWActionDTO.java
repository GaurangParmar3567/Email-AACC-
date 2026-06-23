package com.example.mail.dto.soapnew;

import lombok.Data;

import javax.xml.bind.annotation.*;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class AWActionDTO {
    
    private Long id;
    private Long contactID;
    private AgentDTO agent;
    private String subject;
    private String text = "";
    private String textHTML = "";
    private String callbackStatus;
    private MillisecondsDTO creationTime;
    private String source;
    private String comment;
    private String mailFrom = "";
    private String mailTo = "";
    private String mailCC = "";
    private Integer timeAllocated;
    private Integer outboundTalkTime;
    private String outboundDispositionCode = "";
    private String actionType;
    private ClosedReasonDTO closedReason;

    @XmlElement(name = "attachmentList")
    private AttachmentListDTO attachmentList;
}