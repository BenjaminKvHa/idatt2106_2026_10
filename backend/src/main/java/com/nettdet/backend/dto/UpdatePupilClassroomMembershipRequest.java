package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payload for setting a pupil username and avatar selection in a classroom.")
public record UpdatePupilClassroomMembershipRequest(
        @Schema(description = "Display username shown in leaderboards and classroom views.", example = "detektiv1")
        String username,
        @Schema(
                description = "Avatar selection in the format `<person>_<hair>_<costume>`. This is used to generate the saved avatar image.",
                example = "boy_hair1_costume2")
        String avatar) {
}
