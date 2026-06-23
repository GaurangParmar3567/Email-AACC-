package com.example.mail.config;

public class EmailCleaner {

    public static String cleanBody(String body) {
        if (body == null) return "";

        // 1. Remove long disclaimers (e.g., from "Disclaimer:" to end of string)
        body = body.replaceAll("(?i)Disclaimer:.*", "");

        // 2. Remove "IMPORTANT COMMUNICATION UPDATE" blocks
        body = body.replaceAll("(?i)IMPORTANT COMMUNICATION.*", "");

        // 3. Remove long strings of dashes or whitespace before forwarded headers
        body = body.replaceAll("-{10,}\\s*Forwarded message\\s*-{10,}", "<hr/>");

        // 4. Remove excessive newlines (replace 3+ newlines with 2)
        body = body.replaceAll("(\\r?\\n){3,}", "\n\n");

        return body.trim();
    }
}
