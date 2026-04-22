package com.nettdet.backend.validation;

import org.springframework.stereotype.Component;

import com.nettdet.backend.dto.ImageVerificationRequest;
import com.nettdet.backend.dto.ImageVerificationTask;

@Component
public class ImageVerificationTaskValidator
        implements TaskValidator<ImageVerificationRequest, ImageVerificationTask> {

    @Override
    public boolean validate(ImageVerificationRequest request, ImageVerificationTask task) {
        if (request.selectedCategory() != task.correctCategory()) return false;
        if (!task.correctJustification().equals(request.selectedJustification())) return false;
        return true;
    }

    @Override
    public String getFeedback(ImageVerificationTask task, boolean correct) {
        return correct ? task.correctDescription() : task.falseDescription();
    }

    @Override
    public String getChapterType() {
        return "FOTOGRAFEN";
    }

    @Override
    public Class<ImageVerificationTask> taskType() {
        return ImageVerificationTask.class;
    }
}
