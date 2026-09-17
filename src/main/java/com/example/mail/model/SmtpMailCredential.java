package com.example.mail.model;

import lombok.Data;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "SmtpMailCredential")
public class SmtpMailCredential {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID")
    private Long id;

    @Column(name = "Host", nullable = false, length = 255)
    private String host;

    @Column(name = "Port")
    private Integer port;

    @Column(name = "Username", nullable = false, length = 255)
    private String username;

    @Column(name = "Password", nullable = false, length = 255)
    private String password;

    @Column(name = "Protocol", length = 50)
    private String protocol;

    @Column(name = "SmtpAuth")
    private Boolean smtpAuth;

    @Column(name = "StartTlsEnable")
    private Boolean startTlsEnable;

    @Column(name = "SmtpSslTrust", length = 255)
    private String smtpSslTrust;

    @Column(name = "FromEmail", length = 255)
    private String fromEmail;

    @Column(name = "skill", length = 100)
    private String skill;

    @Column(name = "IsActive")
    private Boolean isActive = Boolean.TRUE;

    @Column(name = "CreatedDate")
    private LocalDateTime createdDate;

    @Column(name = "UpdatedDate")
    private LocalDateTime updatedDate;
}
