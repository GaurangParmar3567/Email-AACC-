package com.example.mail.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AgentSessionResponseDTO {
    private Long sessionId;
    private Integer agentId;
    private String firstName;
    private String lastName;
    private String fullName;
    private Long skillId;
    private String status;
    private LocalDateTime loginTime;
    private LocalDateTime logoutTime;
    private LocalDateTime lastActivity;
    private String ipAddress;
    private String deviceName;
    private String tokenId;
}

