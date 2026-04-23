package com.nettdet.backend.dto;

import java.util.List;

import com.nettdet.backend.domain.FileObject;
import com.nettdet.backend.domain.PupilClassroomMembership;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Full pupil-facing classroom membership view.")
public record PupilResponse(
    @Schema(description = "Pupil account email.", example = "pupil@example.com")
    String email,
    @Schema(description = "Pupil username for the classroom.", example = "detektiv1", nullable = true)
    String username,
    @Schema(description = "Legal name of the account.", example = "Pupil Name")
    String legalName,
    @Schema(description = "Numeric file object id for the saved avatar image.", example = "42", nullable = true)
    Long avatarFileObjectId,
    @Schema(description = "Classroom code. Codes are generated as 6 uppercase letters.", example = "ABCABC")
    String classroomCode,
    @Schema(description = "Whether the classroom membership has been confirmed by a teacher.", example = "true")
    boolean membershipConfirmedByTeacher,
    @Schema(description = "Detailed chapter progression entries for this classroom membership.")
    List<ChapterProgression> chapterProgressions
) {
    @Schema(description = "Per-chapter progression details for a pupil.")
    public record ChapterProgression(
        @Schema(description = "Chapter id.", example = "1")
        Long chapterId,
        @Schema(description = "Successful run count for the chapter.", example = "3", nullable = true)
        Integer successfulRuns,
        @Schema(description = "Highscore or star count for the chapter.", example = "3", nullable = true)
        Integer highscore,
        @Schema(description = "Current unsuccessful streak for the chapter.", example = "0", nullable = true)
        Integer unsuccessfulStreak
    ) {}

    public static PupilResponse from(PupilClassroomMembership membership) {
        FileObject avatar = membership.getAvatarFileObject();
        List<ChapterProgression> progressions = membership.getChapterProgressions().stream()
            .map(p -> new ChapterProgression(
                p.getChapter().getId(),
                p.getSuccessfulRuns(),
                p.getHighscore(),
                p.getUnsuccessfulStreak()
            ))
            .toList();

        return new PupilResponse(
            membership.getAccount().getEmail(),
            membership.getUsername(),
            membership.getAccount().getLegalName(),
            avatar != null ? avatar.getId() : null,
            membership.getClassroom() != null ? membership.getClassroom().getClassroomCode() : null,
            membership.isMembershipConfirmedByTeacher(),
            progressions
        );
    }
}
