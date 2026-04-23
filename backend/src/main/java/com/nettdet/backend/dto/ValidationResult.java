package com.nettdet.backend.dto;

public record ValidationResult(
    boolean correct,
    String feedback,
    int currentRunAttempts,
    int currentRunCorrect,
    boolean runCompleted
) {}
