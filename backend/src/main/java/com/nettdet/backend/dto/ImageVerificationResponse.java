package com.nettdet.backend.dto;

import java.util.List;

public record ImageVerificationResponse(
    Long taskId,
    String taskDescription,
    Long fileId,
    List<String> justifications
) {}
