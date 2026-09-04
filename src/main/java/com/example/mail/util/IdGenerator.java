package com.example.mail.util;

public final class IdGenerator {

    private IdGenerator() {
    }

    public static Long generateContactId() {
        return System.currentTimeMillis() % 10000000L + 1000000L;
    }
}
