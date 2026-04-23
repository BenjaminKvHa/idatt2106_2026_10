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

@RestController
@RequestMapping("/avatar-images")
@Tag(name = "Avatar Images", description = "Public endpoint for generated saved avatar images.")
public class AvatarImageController {

    private final AvatarImageService avatarImageService;

    public AvatarImageController(AvatarImageService avatarImageService) {
        this.avatarImageService = avatarImageService;
    }

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
