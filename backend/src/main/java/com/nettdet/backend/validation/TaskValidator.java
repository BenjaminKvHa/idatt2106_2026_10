package com.nettdet.backend.validation;

import org.springframework.stereotype.Component;

@Component
public interface TaskValidator<R, T> {
    boolean validate(R request, T task);
    String getFeedback(T task, boolean correct);
    String getChapterType();
    Class<T> taskType();
}
