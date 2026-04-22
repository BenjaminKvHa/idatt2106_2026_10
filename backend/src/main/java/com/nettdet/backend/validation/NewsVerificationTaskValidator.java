package com.nettdet.backend.validation;

import org.springframework.stereotype.Component;

import com.nettdet.backend.dto.NewsVerificationRequest;
import com.nettdet.backend.dto.NewsVerificationTask;

@Component
public class NewsVerificationTaskValidator
        implements TaskValidator<NewsVerificationRequest, NewsVerificationTask> {

    @Override
    public boolean validate(NewsVerificationRequest request, NewsVerificationTask task) {
        return task.correctFileId().equals(request.selectedFileId());
    }

    @Override
    public String getFeedback(NewsVerificationTask task, boolean correct) {
        return correct ? task.correctDescription() : task.incorrectDescription();
    }

    @Override
    public String getChapterType() {
        return "NYHETSKVARTALET";
    }

    @Override
    public Class<NewsVerificationTask> taskType() {
        return NewsVerificationTask.class;
    }
}
