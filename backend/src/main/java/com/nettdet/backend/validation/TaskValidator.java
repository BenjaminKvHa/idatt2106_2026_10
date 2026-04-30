package com.nettdet.backend.validation;

import org.springframework.stereotype.Component;

/**
 * Generic interface for task validation logic within the educational system.
 * 
 * <p>Implementations of this interface define how to validate a pupil's response against
 * a specific task type, provide contextual feedback, and identify the chapter category 
 * they belong to.
 * 
 * @param <R> the type of the pupil's response or request
 * @param <T> the type of the task configuration or domain object
 */
@Component
public interface TaskValidator<R, T> {

    /**
     * Validates the pupil's response against the task's correct solution.
     *
     * @param request the pupil's submitted answer or action
     * @param task    the task configuration containing the correct solution
     * @return true if the response is correct, false otherwise
     */
    boolean validate(R request, T task);

    /**
     * Generates pedagogical feedback based on whether the pupil's answer was correct.
     *
     * @param task    the task configuration
     * @param correct whether the pupil's response passed validation
     * @return a feedback string to be displayed to the pupil
     */
    String getFeedback(T task, boolean correct);

    /**
     * Returns the identifier for the chapter type this validator handles.
     *
     * @return the chapter type string (e.g., "IMAGE_VERIFICATION")
     */
    String getChapterType();

    /**
     * Returns the class type of the task this validator is designed for.
     *
     * @return the task's class object
     */
    Class<T> taskType();
}
