package com.nettdet.backend.dto;

import java.util.List;

public record NewsVerificationResponse(
    Long taskId,
    String taskDescription,
    List<Long> fileIds
) {}
