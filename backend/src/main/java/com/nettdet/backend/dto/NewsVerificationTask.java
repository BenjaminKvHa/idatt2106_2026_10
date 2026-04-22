package com.nettdet.backend.dto;

public record NewsVerificationTask(
    String taskDescription,
    Long fileId_1,
    Long fileId_2,
    Long correctFileId,
    String correctDescription,
    String incorrectDescription
) {}
