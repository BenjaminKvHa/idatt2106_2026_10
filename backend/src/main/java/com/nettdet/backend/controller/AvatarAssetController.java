package com.nettdet.backend.controller;

import java.util.List;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nettdet.backend.dto.AvatarAssetResponse;
import com.nettdet.backend.service.AvatarAssetService;
import com.nettdet.backend.service.FileObjectService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for managing avatar assets.
 * Provides public endpoints to retrieve metadata for all available avatar assets
 * and to download/view the raw asset files (typically images) used for customizing user avatars.
 * This controller interacts with {@link AvatarAssetService} to fetch asset information and file content.
 */
@RestController
@RequestMapping("/avatar-assets")
@Tag(name = "Avatar Assets", description = "Public endpoints for avatar asset metadata and asset files.")
public class AvatarAssetController {

    private final AvatarAssetService avatarAssetService;

    /**
     * Constructs a new AvatarAssetController with the required service.
     *
     * @param avatarAssetService the service for managing avatar asset logic
     */
    public AvatarAssetController(AvatarAssetService avatarAssetService) {
        this.avatarAssetService = avatarAssetService;
    }

    /**
     * Retrieves a list of all available avatar assets.
     * Each asset in the list contains metadata such as its ID, name, category, and URL pointers.
     *
     * @return a {@link ResponseEntity} containing a list of {@link AvatarAssetResponse} objects
     */
    @GetMapping({"", "/"})
    @Operation(summary = "List avatar assets", description = "Returns all available base avatar assets.")
    public ResponseEntity<List<AvatarAssetResponse>> getAssets() {
        return ResponseEntity.ok(avatarAssetService.getAllAssets());
    }

    /**
     * Retrieves the raw content of a specific avatar asset file.
     * The file is served with appropriate content headers (e.g., image/png) for inline viewing.
     *
     * @param assetType the type/category of the asset (e.g., 'body', 'eyes', 'hair')
     * @param fileName the exact name of the file to retrieve
     * @return a {@link ResponseEntity} containing the file bytes, content type, and content disposition headers
     * @throws com.nettdet.backend.exception.ResourceNotFoundException if the asset or file does not exist (handled globally)
     */
    @GetMapping("/{assetType}/{fileName:.+}")
    @Operation(summary = "Get avatar asset image", description = "Returns a public avatar asset PNG by asset type and file name.")
    public ResponseEntity<byte[]> getAsset(
            @PathVariable String assetType,
            @PathVariable String fileName) {
        FileObjectService.StoredFile storedFile = avatarAssetService.getAsset(assetType, fileName);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(storedFile.contentType()));
        headers.setContentDisposition(ContentDisposition.inline().filename(fileName).build());

        return new ResponseEntity<>(storedFile.bytes(), headers, HttpStatus.OK);
    }
}
