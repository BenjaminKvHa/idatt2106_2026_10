package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Metadata describing a public avatar base asset.")
public record AvatarAssetResponse(
        @Schema(description = "Asset category.", example = "hair")
        String assetType,
        @Schema(description = "Logical asset name without extension.", example = "hair1")
        String name,
        @Schema(description = "File extension.", example = "png")
        String filetype) {
}
