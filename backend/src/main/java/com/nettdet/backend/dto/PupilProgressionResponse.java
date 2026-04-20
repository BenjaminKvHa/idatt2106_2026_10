package com.nettdet.backend.dto;

import java.util.List;

import com.nettdet.backend.domain.PupilClassroomMembership;

public record PupilProgressionResponse(
    List<PupilResponse.ChapterProgression> chapterProgressions
) {
    public static PupilProgressionResponse from(PupilClassroomMembership membership) {
        List<PupilResponse.ChapterProgression> list = membership.getChapterProgressions().stream()
            .map(p -> new PupilResponse.ChapterProgression(
                p.getChapter().getId(),
                p.getSuccessfulRuns(),
                p.getHighscore(),
                p.getUnsuccessfulStreak()
            ))
            .toList();
        return new PupilProgressionResponse(list);
    }
}
