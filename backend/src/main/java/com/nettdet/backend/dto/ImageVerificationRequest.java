package com.nettdet.backend.dto;

public record ImageVerificationRequest(
    int selectedCategory,
    String selectedJustification
) {}
