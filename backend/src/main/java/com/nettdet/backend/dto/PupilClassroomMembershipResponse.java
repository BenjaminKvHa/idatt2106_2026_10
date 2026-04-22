package com.nettdet.backend.dto;

import com.nettdet.backend.domain.PupilClassroomMembership;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Pupil classroom membership settings and confirmation state.")
public record PupilClassroomMembershipResponse(
    @Schema(description = "Pupil username for the classroom.", example = "detektiv1")
    String username,
    @Schema(description = "Numeric file object id for the generated saved avatar image.", example = "42", nullable = true)
    Long avatarFileObjectId,
    @Schema(description = "Whether the classroom membership has been accepted by a teacher.", example = "false")
    boolean membershipConfirmedByTeacher
) {
    public static PupilClassroomMembershipResponse from(PupilClassroomMembership membership) {
        Long avatarFileObjectId = membership.getAvatarFileObject() != null
                ? membership.getAvatarFileObject().getId()
                : null;

        return new PupilClassroomMembershipResponse(
                membership.getUsername(),
                avatarFileObjectId,
                membership.isMembershipConfirmedByTeacher());
    }
}
