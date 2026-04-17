package com.nettdet.backend.dto;

public record PendingPupilRequestResponse(
    String accountEmail,
    String legalName
) {
}
