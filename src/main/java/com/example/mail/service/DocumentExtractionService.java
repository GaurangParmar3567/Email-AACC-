package com.example.mail.service;

import org.apache.tika.Tika;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;

@Service
public class DocumentExtractionService {

    private final Tika tika = new Tika();

    public String extractText(byte[] fileData) throws Exception {
        try (ByteArrayInputStream stream = new ByteArrayInputStream(fileData)) {
            return tika.parseToString(stream).trim();
        }
    }
}
