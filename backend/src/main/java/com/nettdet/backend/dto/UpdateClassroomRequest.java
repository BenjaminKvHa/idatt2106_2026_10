package com.nettdet.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Payload for updating classroom name and description.")
public record UpdateClassroomRequest(
    @Schema(description = "Human-readable classroom name.", example = "8A Oppdatert")
    @NotBlank(message = "name is required")
    @Size(max = 255, message = "name must be less than 256 characters")
    String name,

    @Schema(description = "Optional classroom description.", example = "Ny beskrivelse")
    String description
) {
}
