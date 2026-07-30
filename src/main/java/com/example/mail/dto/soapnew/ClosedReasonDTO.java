package com.example.mail.dto.soapnew;

import lombok.Data;

import javax.xml.bind.annotation.*;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class ClosedReasonDTO {
    private String name;
    private Integer numericValue;
}