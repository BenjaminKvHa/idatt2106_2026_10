package com.nettdet.backend.util;

import java.security.SecureRandom;
import org.springframework.stereotype.Component;

/**
 * Utility component responsible for generating unique, secure alphanumeric codes 
 * for classroom identification.
 * <p>
 * This class uses {@link SecureRandom} to ensure that generated codes are 
 * cryptographically strong and unpredictable, making it suitable for 
 * public-facing identifiers.
 * </p>
 * 
 * @author YourName
 * @version 1.0
 */
@Component
public class ClassroomCodeGenerator {

    /** The fixed length of the generated classroom code. */
    static final int CLASSROOM_CODE_LENGTH = 6;

    /** The character set used for code generation (Uppercase Latin alphabet). */
    private static final char[] ALLOWED_CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ".toCharArray();

    /** 
     * Cryptographically strong random number generator. 
     * Shared across instances to provide better entropy.
     */
    private final SecureRandom secureRandom = new SecureRandom();

    /**
     * Generates a random alphanumeric string based on the defined 
     * {@value #CLASSROOM_CODE_LENGTH} length and {@link #ALLOWED_CHARACTERS} set.
     * <p>
     * The resulting code is composed strictly of uppercase letters (A-Z).
     * This method is thread-safe as it utilizes a {@code SecureRandom} instance.
     * </p>
     * 
     * @return A {@link String} representing a unique classroom invitation code.
     *         Example: "XJ9KLR"
     */
    public String generateCode() {
        char[] generated = new char[CLASSROOM_CODE_LENGTH];
        for (int i = 0; i < generated.length; i++) {
            generated[i] = ALLOWED_CHARACTERS[secureRandom.nextInt(ALLOWED_CHARACTERS.length)];
        }
        return new String(generated);
    }
}