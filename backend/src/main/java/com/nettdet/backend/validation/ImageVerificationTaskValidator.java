package com.nettdet.backend.validation;

import org.springframework.stereotype.Component;

import com.nettdet.backend.dto.ImageVerificationRequest;
import com.nettdet.backend.dto.ImageVerificationTask;

/**
 * Validator implementation for image-based verification tasks ("Fotografen").
 * 
 * <p>Validates that the pupil has correctly identified the category of an image
 * and provided the expected justification. This validator is used in Chapter 1.
 */
@Component
public class ImageVerificationTaskValidator
        implements TaskValidator<ImageVerificationRequest, ImageVerificationTask> {

    /**
     * Validates the image verification request against the task definition.
     * The submission is correct only if both the selected category and the 
     * selected justification match the task's pre-defined correct values.
     *
     * @param request the pupil's submission containing selected category and justification
     * @param task    the task configuration with the correct solution
     * @return true if both category and justification are correct
     */
    @Override
    public boolean validate(ImageVerificationRequest request, ImageVerificationTask task) {
        if (request.selectedCategory() != task.correctCategory()) return false;
        if (!task.correctJustification().equals(request.selectedJustification())) return false;
        return true;
    }

    /**
     * Returns the description associated with the task's correct or incorrect outcome.
     *
     * @param task    the image verification task
     * @param correct whether the pupil's submission was accurate
     * @return the appropriate pedagogical description (correct or false)
     */
    @Override
    public String getFeedback(ImageVerificationTask task, boolean correct) {
        return correct ? task.correctDescription() : task.falseDescription();
    }

    /**
     * Identifies this validator as belonging to the "FOTOGRAFEN" chapter.
     *
     * @return the string "FOTOGRAFEN"
     */
    @Override
    public String getChapterType() {
        return "FOTOGRAFEN";
    }

    /**
     * Returns the {@link ImageVerificationTask} class type.
     *
     * @return the task type class
     */
    @Override
    public Class<ImageVerificationTask> taskType() {
        return ImageVerificationTask.class;
    }
}
