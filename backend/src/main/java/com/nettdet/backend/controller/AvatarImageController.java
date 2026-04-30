package com.nettdet.backend.controller;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nettdet.backend.service.AvatarImageService;
import com.nettdet.backend.service.FileObjectService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for retrieving generated avatar images.
 * Provides a public endpoint to access stored avatar images that have been generated for users.
 * This controller interacts with {@link AvatarImageService} to fetch the image content from the storage system.
 */
@RestController
@RequestMapping("/avatar-images")
@Tag(name = "Avatar Images", description = "Public endpoint for generated saved avatar images.")
public class AvatarImageController {

    private final AvatarImageService avatarImageService;

    /**
     * Constructs a new AvatarImageController with the required service.
     *
     * @param avatarImageService the service for managing avatar image retrieval logic
     */
    public AvatarImageController(AvatarImageService avatarImageService) {
        this.avatarImageService = avatarImageService;
    }

    /**
     * Retrieves the raw content of a generated avatar image file.
     * The file is served with appropriate content headers (e.g., image/png) for inline viewing.
     * This endpoint only serves files located under the public 'avatar-images' object-key prefix.
     *
     * @param fileName the exact name of the generated avatar image file to retrieve
     * @return a {@link ResponseEntity} containing the image bytes, content type, and content disposition headers
     * @throws com.nettdet.backend.exception.ResourceNotFoundException if the image file does not exist (handled globally)
     */
    @GetMapping("/{fileName:.+}")
    @Operation(summary = "Get generated avatar image", description = "Returns a saved generated avatar PNG by file name. Only public files under the avatar-images object-key prefix are served.")
    public ResponseEntity<byte[]> getAvatarImage(@PathVariable String fileName) {
        FileObjectService.StoredFile storedFile = avatarImageService.getAvatarImage(fileName);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(storedFile.contentType()));
        headers.setContentDisposition(ContentDisposition.inline().filename(fileName).build());

        return new ResponseEntity<>(storedFile.bytes(), headers, HttpStatus.OK);
    }
}
