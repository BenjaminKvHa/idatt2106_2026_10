package com.nettdet.backend.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class ClassroomCodeGeneratorTest {

    private final ClassroomCodeGenerator generator = new ClassroomCodeGenerator();

    @DisplayName("Generates a classroom code with exactly six characters")
    @Test
    void generateCode_whenCalled_returnsSixCharacterCode() {
        String code = generator.generateCode();

        assertEquals(6, code.length());
    }

    @DisplayName("Generates a classroom code containing only uppercase letters")
    @Test
    void generateCode_whenCalled_returnsOnlyUppercaseLetters() {
        String code = generator.generateCode();

        assertTrue(code.matches("[A-Z]{6}"));
    }
}
