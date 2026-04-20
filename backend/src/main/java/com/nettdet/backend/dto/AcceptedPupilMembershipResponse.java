package com.nettdet.backend.dto;

public record AcceptedPupilMembershipResponse(
    String accountEmail,
    String legalName,
    boolean membershipConfirmedByTeacher
) {
}
