package com.nettdet.backend.dto;

public record ChapterProgressionResponse(
    int currentRunAttempts,
    int currentRunCorrect,
    int successfulRuns,
    int highscore,
    int unsuccessfulStreak
) {}
