package com.example.mail.dto.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;

/** Only the legacy action fields required to render a maker email reply. */
@Data
@AllArgsConstructor
public class MakerMailActionDTO {

    @JsonProperty("CreationTime")
    private Long creationTime;

    @JsonProperty("MailFrom")
    private String mailFrom;

    @JsonProperty("MailTo")
    private String mailTo;

    @JsonProperty("MailCC")
    private String mailCc;

    @JsonProperty("Subject")
    private String subject;

    @JsonProperty("TextHTML")
    private String textHtml;

    @JsonProperty("Text")
    private String text;
}
