package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "JWT access-token response returned after login, signup, and refresh.")
public record AuthTokenResponse(
    @Schema(
            description = "Short-lived JWT access token to send as `Authorization: Bearer <token>`.",
            example = "eyJhbGciOiJIUzI1NiJ9.eyJyb2xlIjoiVEVBQ0hFUiIsImVtYWlsIjoidGVhY2hlckBleGFtcGxlLmNvbSJ9.signature")
    String accessToken
) {
}
