package com.nettdet.backend.dto;

import java.util.List;

import com.nettdet.backend.domain.FileObject;
import com.nettdet.backend.domain.PupilClassroomMembership;

public record PupilResponse(
    String email,
    String username,
    String legalName,
    Long avatarFileObjectId,
    String classroomCode,
    boolean membershipConfirmedByTeacher,
    List<ChapterProgression> chapterProgressions
) {
    public record ChapterProgression(
        Long chapterId,
        Integer successfulRuns,
        Integer highscore,
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
