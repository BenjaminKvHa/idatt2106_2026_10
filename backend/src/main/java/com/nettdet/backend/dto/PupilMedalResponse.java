package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Medal shown to the authenticated pupil.")
public record PupilMedalResponse(
    @Schema(description = "Stable medal key used by the frontend.", example = "chapter-1")
    String medalKey,
    @Schema(description = "Medal display name.", example = "Nyhetskvartalet")
    String name,
    @Schema(description = "Description of what unlocks this medal.", example = "Fullfør Nyhetskvartalet for å låse opp denne medaljen.")
    String description,
    @Schema(description = "Whether this medal is unlocked for the authenticated pupil.", example = "true")
    boolean unlocked,
    @Schema(description = "File object id for the medal image to display. This points to the unlocked image when unlocked, otherwise the shared locked medal image.", example = "12", nullable = true)
    Long imageFileObjectId
) {
}
