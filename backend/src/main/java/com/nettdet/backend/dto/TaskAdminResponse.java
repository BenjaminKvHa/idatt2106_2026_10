package com.nettdet.backend.dto;

import java.time.LocalDateTime;

import com.nettdet.backend.domain.Task;
import com.nettdet.backend.domain.enums.TaskDifficulty;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Flat task representation returned by admin endpoints. Excludes nested entity relations.")
public record TaskAdminResponse(
    Long id,
    TaskDifficulty difficulty,
    Long chapterId,
    String classroomCode,
    String taskJson,
    LocalDateTime createdAt
) {
    public static TaskAdminResponse from(Task task) {
        return new TaskAdminResponse(
            task.getId(),
            task.getDifficulty(),
            task.getChapter() != null ? task.getChapter().getId() : null,
            task.getClassroom() != null ? task.getClassroom().getClassroomCode() : null,
            task.getTaskJson(),
            task.getCreatedAt()
        );
    }
}
