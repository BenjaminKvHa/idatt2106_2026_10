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

@RestController
@RequestMapping("/avatar-assets")
public class AvatarAssetController {

    private final AvatarAssetService avatarAssetService;

    public AvatarAssetController(AvatarAssetService avatarAssetService) {
        this.avatarAssetService = avatarAssetService;
    }

    @GetMapping({"", "/"})
    public ResponseEntity<List<AvatarAssetResponse>> getAssets() {
        return ResponseEntity.ok(avatarAssetService.getAllAssets());
    }

    @GetMapping("/{assetType}/{fileName:.+}")
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
