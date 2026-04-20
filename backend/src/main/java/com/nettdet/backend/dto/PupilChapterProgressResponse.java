package com.nettdet.backend.dto;

public record PupilChapterProgressResponse(
    Long chapterId,
    int highscore,
    boolean unlocked
) {
}
