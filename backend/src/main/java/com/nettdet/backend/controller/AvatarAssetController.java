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

@RestController
@RequestMapping("/avatar-assets")
@Tag(name = "Avatar Assets", description = "Public endpoints for avatar asset metadata and asset files.")
public class AvatarAssetController {

    private final AvatarAssetService avatarAssetService;

    public AvatarAssetController(AvatarAssetService avatarAssetService) {
        this.avatarAssetService = avatarAssetService;
    }

    @GetMapping({"", "/"})
    @Operation(summary = "List avatar assets", description = "Returns all available base avatar assets.")
    public ResponseEntity<List<AvatarAssetResponse>> getAssets() {
        return ResponseEntity.ok(avatarAssetService.getAllAssets());
    }

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
