package com.nettdet.backend.dto;

import com.nettdet.backend.domain.Account;
import com.nettdet.backend.domain.enums.AccountRole;

public record AuthMeResponse(
    String email,
    String legalName,
    AccountRole role,
    String username,
    String classroomCode,
    Long avatarFileObjectId
) {
    public static AuthMeResponse from(Account account, String username, String classroomCode, Long avatarFileObjectId) {
        return new AuthMeResponse(
            account.getEmail(),
            account.getLegalName(),
            account.getAccountRole(),
            username,
            classroomCode,
            avatarFileObjectId
        );
    }
}
