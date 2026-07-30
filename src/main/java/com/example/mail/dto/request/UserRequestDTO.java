package com.example.mail.dto.request;

import lombok.Data;

@Data
public class UserRequestDTO {
    private String firstName;
    private String lastName;
    private Long skillId;
    private Long agentId;
}
