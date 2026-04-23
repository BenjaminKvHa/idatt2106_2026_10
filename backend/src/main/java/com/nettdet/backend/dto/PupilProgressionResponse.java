package com.nettdet.backend.dto;

import java.util.List;

import com.nettdet.backend.domain.PupilClassroomMembership;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Detailed chapter progression list for a pupil within one classroom.")
public record PupilProgressionResponse(
    @Schema(description = "Per-chapter progression entries for the classroom membership.")
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
