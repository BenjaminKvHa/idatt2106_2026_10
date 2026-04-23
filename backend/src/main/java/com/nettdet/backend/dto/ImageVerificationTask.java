package com.nettdet.backend.dto;

import java.util.List;

public record ImageVerificationTask(
    String taskDescription,
    Long fileId,
    int correctCategory,
    String correctJustification,
    List<String> justifications,
    String correctDescription,
    String falseDescription
) {}
