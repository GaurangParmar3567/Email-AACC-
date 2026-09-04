package com.example.mail.dto.soap.common;

import lombok.Data;

import javax.xml.bind.annotation.*;

@Data
@XmlAccessorType(XmlAccessType.FIELD)
public class AgentDTO {
    private Long id;
    private String firstName;
    private String lastName;
}
