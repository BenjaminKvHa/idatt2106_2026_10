package com.nettdet.backend.validation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.nettdet.backend.domain.enums.ImageCategory;
import com.nettdet.backend.dto.ImageVerificationRequest;
import com.nettdet.backend.dto.ImageVerificationTask;

class ImageVerificationTaskValidatorTest {

    private ImageVerificationTaskValidator validator;

    @BeforeEach
    void setUp() {
        validator = new ImageVerificationTaskValidator();
    }

    @DisplayName("Validate returns true when both category and justification match")
    @Test
    void validate_whenCategoryAndJustificationMatch_returnsTrue() {
        ImageVerificationTask task = task(ImageCategory.AI_GENERATED, "right");
        ImageVerificationRequest request = new ImageVerificationRequest(ImageCategory.AI_GENERATED, "right");

        assertTrue(validator.validate(request, task));
    }

    @DisplayName("Validate returns false when category does not match")
    @Test
    void validate_whenCategoryDoesNotMatch_returnsFalse() {
        ImageVerificationTask task = task(ImageCategory.REAL, "right");
        ImageVerificationRequest request = new ImageVerificationRequest(ImageCategory.MANIPULATED, "right");

        assertFalse(validator.validate(request, task));
    }

    @DisplayName("Validate returns false when justification does not match")
    @Test
    void validate_whenJustificationDoesNotMatch_returnsFalse() {
        ImageVerificationTask task = task(ImageCategory.REAL, "right");
        ImageVerificationRequest request = new ImageVerificationRequest(ImageCategory.REAL, "wrong");

        assertFalse(validator.validate(request, task));
    }

    @DisplayName("Feedback returns the correctDescription when answer is correct")
    @Test
    void getFeedback_whenCorrect_returnsCorrectDescription() {
        ImageVerificationTask task = task(ImageCategory.REAL, "right");

        assertEquals("ok", validator.getFeedback(task, true));
    }

    @DisplayName("Feedback returns the falseDescription when answer is incorrect")
    @Test
    void getFeedback_whenIncorrect_returnsFalseDescription() {
        ImageVerificationTask task = task(ImageCategory.REAL, "right");

        assertEquals("nope", validator.getFeedback(task, false));
    }

    @DisplayName("Chapter type identifies the validator as FOTOGRAFEN")
    @Test
    void getChapterType_returnsFotografen() {
        assertEquals("FOTOGRAFEN", validator.getChapterType());
    }

    @DisplayName("Task type identifies the storage DTO as ImageVerificationTask")
    @Test
    void taskType_returnsImageVerificationTaskClass() {
        assertEquals(ImageVerificationTask.class, validator.taskType());
    }

    private ImageVerificationTask task(ImageCategory category, String justification) {
        return new ImageVerificationTask(
                "desc",
                42L,
                category,
                justification,
                List.of(justification, "other-1", "other-2"),
                "ok",
                "nope");
    }
}
