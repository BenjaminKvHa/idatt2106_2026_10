package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Per-chapter progression counters for one pupil.")
public record ChapterProgressionResponse(
    @Schema(description = "How many tasks have been attempted in the current run.", example = "2")
    int currentRunAttempts,
    @Schema(description = "How many tasks have been answered correctly in the current run.", example = "1")
    int currentRunCorrect,
    @Schema(description = "Total completed successful runs for the chapter.", example = "3")
    int successfulRuns,
    @Schema(description = "Best recorded score or star count for the chapter.", example = "3")
    int highscore,
    @Schema(description = "Current unsuccessful streak for the chapter.", example = "0")
    int unsuccessfulStreak
) {}
