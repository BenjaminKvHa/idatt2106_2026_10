package com.nettdet.backend.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Map/progress summary for one chapter for the authenticated pupil.")
public record PupilChapterProgressResponse(
    @Schema(description = "Chapter id.", example = "1")
    Long chapterId,
    @Schema(description = "Current star count or highscore for the chapter.", example = "3")
    int highscore,
    @Schema(description = "Whether the chapter is currently unlocked for the pupil.", example = "true")
    boolean unlocked
) {
}
