package com.nettdet.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nettdet.backend.domain.enums.ClassroomAccess;
import com.nettdet.backend.repository.FileObjectRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class AvatarImageService {

    private static final String AVATAR_IMAGE_PREFIX = "avatar-images/";

    private final FileObjectRepository fileObjectRepository;
    private final FileObjectService fileObjectService;

    public AvatarImageService(FileObjectRepository fileObjectRepository, FileObjectService fileObjectService) {
        this.fileObjectRepository = fileObjectRepository;
        this.fileObjectService = fileObjectService;
    }

    @Transactional(readOnly = true)
    public FileObjectService.StoredFile getAvatarImage(String fileName) {
        String normalizedFileName = normalizeFileName(fileName);
        String objectKey = AVATAR_IMAGE_PREFIX + normalizedFileName;

        fileObjectRepository.findByObjectKeyAndAccessLevel(objectKey, ClassroomAccess.PUBLIC)
                .orElseThrow(() -> new EntityNotFoundException("Avatar image not found"));

        return fileObjectService.read(objectKey);
    }

    private String normalizeFileName(String fileName) {
        if (fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("Avatar image file name is required");
        }

        if (fileName.contains("/") || fileName.contains("..")) {
            throw new IllegalArgumentException("Invalid avatar image path");
        }

        if (!fileName.toLowerCase().endsWith(".png")) {
            throw new IllegalArgumentException("Avatar images must be png files");
        }

        return fileName;
    }
}
