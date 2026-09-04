package com.example.mail.dto.soap.common;

import lombok.Data;

import javax.xml.bind.annotation.*;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class MillisecondsDTO {
    private Long milliseconds;
}
