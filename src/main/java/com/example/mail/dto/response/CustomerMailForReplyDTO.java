package com.example.mail.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Response returned to the checker when it requests the latest maker reply.
 */
@Data
public class CustomerMailForReplyDTO {

    @JsonProperty("Message")
    private String message;

    @JsonProperty("ReplyText")
    private String replyText;

    @JsonProperty("ClosedReason")
    private String closedReason;

    @JsonProperty("Comment")
    private String comment;

    @JsonProperty("GetError")
    private String getError;
}
