package com.example.mail.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "mail")
@Data
public class MailProperties {

    private List<MailAccount> accounts = new ArrayList<>();

    @Data
    public static class MailAccount {
        private String host;
        private String username;
        private String password;
        private int port;
        private String protocol;
    }

    public String defaultSkillName;
    public String defaultPriorityName;
}



