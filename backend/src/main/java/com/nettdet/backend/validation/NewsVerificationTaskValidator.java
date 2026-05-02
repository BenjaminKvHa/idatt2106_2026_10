package com.nettdet.backend.validation;

import org.springframework.stereotype.Component;

import com.nettdet.backend.dto.NewsVerificationRequest;
import com.nettdet.backend.dto.NewsVerificationTask;

/**
 * Validator implementation for news-based verification tasks ("Nyhetskvartalet").
 * 
 * <p>Validates that the pupil has correctly identified the original file or source
 * of a news item. This validator is used in Chapter 2.
 */
@Component
public class NewsVerificationTaskValidator
        implements TaskValidator<NewsVerificationRequest, NewsVerificationTask> {

    /**
     * Validates the news verification request by comparing the pupil's selected file ID
     * with the task's correct file ID.
     *
     * @param request the pupil's submission containing the selected file ID
     * @param task    the task configuration with the correct file ID
     * @return true if the selected file ID is correct
     */
    @Override
    public boolean validate(NewsVerificationRequest request, NewsVerificationTask task) {
        return task.correctFileId().equals(request.selectedFileId());
    }

    /**
     * Returns the description associated with the task's correct or incorrect outcome.
     *
     * @param task    the news verification task
     * @param correct whether the pupil's submission was accurate
     * @return the appropriate pedagogical description (correct or incorrect)
     */
    @Override
    public String getFeedback(NewsVerificationTask task, boolean correct) {
        return correct ? task.correctDescription() : task.incorrectDescription();
    }

    /**
     * Identifies this validator as belonging to the "NYHETSKVARTALET" chapter.
     *
     * @return the string "NYHETSKVARTALET"
     */
    @Override
    public String getChapterType() {
        return "NYHETSKVARTALET";
    }

    /**
     * Returns the {@link NewsVerificationTask} class type.
     *
     * @return the task type class
     */
    @Override
    public Class<NewsVerificationTask> taskType() {
        return NewsVerificationTask.class;
    }
}
