package com.nettdet.backend.dto;

import com.nettdet.backend.domain.PupilClassroomMembership;

public record PupilClassroomMembershipResponse(
    String username,
    Long avatarFileObjectId,
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
