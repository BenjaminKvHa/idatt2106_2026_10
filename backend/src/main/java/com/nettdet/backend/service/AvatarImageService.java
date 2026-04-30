package com.nettdet.backend.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nettdet.backend.domain.enums.ClassroomAccess;
import com.nettdet.backend.repository.FileObjectRepository;

import jakarta.persistence.EntityNotFoundException;

/**
 * Service class for serving generated or uploaded avatar images.
 * 
 * <p>Handles the retrieval of avatar image files from storage, ensuring they 
 * have public access levels and valid file extensions (PNG).
 */
@Service
public class AvatarImageService {

    private static final String AVATAR_IMAGE_PREFIX = "avatar-images/";

    private final FileObjectRepository fileObjectRepository;
    private final FileObjectService fileObjectService;

    public AvatarImageService(FileObjectRepository fileObjectRepository, FileObjectService fileObjectService) {
        this.fileObjectRepository = fileObjectRepository;
        this.fileObjectService = fileObjectService;
    }

    /**
     * Retrieves the stored avatar image file.
     * Validates that the file metadata exists in the database with public access
     * before reading from the actual storage.
     *
     * @param fileName the name of the avatar image file (must be .png)
     * @return a {@link FileObjectService.StoredFile} containing the image data
     * @throws EntityNotFoundException  if the image is not found or is not public
     * @throws IllegalArgumentException if the filename is invalid or not a PNG
     */
    @Transactional(readOnly = true)
    public FileObjectService.StoredFile getAvatarImage(String fileName) {
        String normalizedFileName = normalizeFileName(fileName);
        String objectKey = AVATAR_IMAGE_PREFIX + normalizedFileName;

        fileObjectRepository.findByObjectKeyAndAccessLevel(objectKey, ClassroomAccess.PUBLIC)
                .orElseThrow(() -> new EntityNotFoundException("Avatar image not found"));

        return fileObjectService.read(objectKey);
    }

    /**
     * Normalizes and validates the provided filename.
     * Ensures the filename is not blank, doesn't contain path traversal sequences,
     * and has a .png extension.
     *
     * @param fileName the filename to validate
     * @return the validated filename
     * @throws IllegalArgumentException if the filename fails validation
     */
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
