package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Response containing a file object reference.")
public record FileReferenceResponse(
    @Schema(description = "File object ID.", example = "42")
    Long fileId,

    @Schema(description = "Object key in MinIO/S3 storage.", example = "task-assets/fotografen/easy/task1/image.png")
    String objectKey,

    @Schema(description = "Whether the file was newly created or already existed.", example = "true")
    boolean newlyCreated
) {}
