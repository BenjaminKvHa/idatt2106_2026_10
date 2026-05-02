package com.nettdet.backend.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.nettdet.backend.dto.NewsVerificationRequest;
import com.nettdet.backend.dto.NewsVerificationTask;

class NewsVerificationTaskValidatorTest {

    private NewsVerificationTaskValidator validator;

    @BeforeEach
    void setUp() {
        validator = new NewsVerificationTaskValidator();
    }

    @DisplayName("Validate returns true when selected file id matches the correct file id")
    @Test
    void validate_whenSelectedFileMatchesCorrectFile_returnsTrue() {
        NewsVerificationTask task = task(101L);
        NewsVerificationRequest request = new NewsVerificationRequest(101L);

        assertTrue(validator.validate(request, task));
    }

    @DisplayName("Validate returns false when selected file id does not match the correct file id")
    @Test
    void validate_whenSelectedFileDoesNotMatchCorrectFile_returnsFalse() {
        NewsVerificationTask task = task(101L);
        NewsVerificationRequest request = new NewsVerificationRequest(202L);

        assertFalse(validator.validate(request, task));
    }

    @DisplayName("Feedback returns the correctDescription when answer is correct")
    @Test
    void getFeedback_whenCorrect_returnsCorrectDescription() {
        NewsVerificationTask task = task(101L);

        assertEquals("good", validator.getFeedback(task, true));
    }

    @DisplayName("Feedback returns the incorrectDescription when answer is incorrect")
    @Test
    void getFeedback_whenIncorrect_returnsIncorrectDescription() {
        NewsVerificationTask task = task(101L);

        assertEquals("bad", validator.getFeedback(task, false));
    }

    @DisplayName("Chapter type identifies the validator as NYHETSKVARTALET")
    @Test
    void getChapterType_returnsNyhetskvartalet() {
        assertEquals("NYHETSKVARTALET", validator.getChapterType());
    }

    @DisplayName("Task type identifies the storage DTO as NewsVerificationTask")
    @Test
    void taskType_returnsNewsVerificationTaskClass() {
        assertEquals(NewsVerificationTask.class, validator.taskType());
    }

    private NewsVerificationTask task(long correctFileId) {
        return new NewsVerificationTask("desc", 1L, 2L, correctFileId, "good", "bad", null, null);
    }
}
