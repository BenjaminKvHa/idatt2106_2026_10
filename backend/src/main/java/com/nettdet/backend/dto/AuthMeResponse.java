package com.nettdet.backend.dto;

import com.nettdet.backend.domain.Account;
import com.nettdet.backend.domain.enums.AccountRole;

public record AuthMeResponse(
    String email,
    String legalName,
    AccountRole role,
    String username,
    String classroomCode,
    Long avatarFileObjectId,
    String avatarSelection,
    boolean membershipConfirmed
) {
    public static AuthMeResponse from(
            Account account,
            String username,
            String classroomCode,
            Long avatarFileObjectId,
            String avatarSelection,
            boolean membershipConfirmed) {
        return new AuthMeResponse(
            account.getEmail(),
            account.getLegalName(),
            account.getAccountRole(),
            username,
            classroomCode,
            avatarFileObjectId,
            avatarSelection,
            membershipConfirmed
        );
    }
}
