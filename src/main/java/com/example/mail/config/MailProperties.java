package com.example.mail.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ConfigurationProperties(prefix = "mail")
public class MailProperties {

    private List<MailAccount> accounts = new ArrayList<>();

    public List<MailAccount> getAccounts() { return accounts; }
    public void setAccounts(List<MailAccount> accounts) { this.accounts = accounts; }

    public static class MailAccount {
        private String host;
        private String username;
        private String password;
        private int port;
        private String protocol;

        // Getters and Setters
        public String getHost() { return host; }
        public void setHost(String host) { this.host = host; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
        public int getPort() { return port; }
        public void setPort(int port) { this.port = port; }
        public String getProtocol() { return protocol; }
        public void setProtocol(String protocol) { this.protocol = protocol; }
    }
}



