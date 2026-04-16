package com.nettdet.backend.util;

import java.security.SecureRandom;

import org.springframework.stereotype.Component;

@Component
public class ClassroomCodeGenerator {

    static final int CLASSROOM_CODE_LENGTH = 6;
    private static final char[] ALLOWED_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    private final SecureRandom secureRandom = new SecureRandom();

    public String generateCode() {
        char[] generated = new char[CLASSROOM_CODE_LENGTH];
        for (int i = 0; i < generated.length; i++) {
            generated[i] = ALLOWED_CHARACTERS[secureRandom.nextInt(ALLOWED_CHARACTERS.length)];
        }
        return new String(generated);
    }
}
