package com.nettdet.backend.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nettdet.backend.domain.FileObject;
import com.nettdet.backend.dto.AvatarAssetResponse;
import com.nettdet.backend.repository.FileObjectRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class AvatarAssetService {

    private static final String AVATAR_ASSET_PREFIX = "avatar-assets/";

    private final FileObjectRepository fileObjectRepository;
    private final FileObjectService fileObjectService;

    public AvatarAssetService(FileObjectRepository fileObjectRepository, FileObjectService fileObjectService) {
        this.fileObjectRepository = fileObjectRepository;
        this.fileObjectService = fileObjectService;
    }

    @Transactional(readOnly = true)
    public List<AvatarAssetResponse> getAllAssets() {
        List<FileObject> allAssets = new ArrayList<>();
        allAssets.addAll(fileObjectRepository.findAllByObjectKeyStartingWith(AVATAR_ASSET_PREFIX));
        allAssets.addAll(fileObjectRepository.findAllByObjectKeyStartingWith("/" + AVATAR_ASSET_PREFIX));

        Map<String, AvatarAssetResponse> uniqueByNormalizedPath = new LinkedHashMap<>();

        for (FileObject fileObject : allAssets) {
            String normalizedKey = normalizeObjectKey(fileObject.getObjectKey());
            AvatarAssetResponse parsed = parseAssetFromKey(normalizedKey);
            if (parsed != null) {
                uniqueByNormalizedPath.putIfAbsent(normalizedKey, parsed);
            }
        }

        return uniqueByNormalizedPath.values().stream()
                .sorted(Comparator
                        .comparing(AvatarAssetResponse::assetType)
                        .thenComparing(AvatarAssetResponse::name)
                        .thenComparing(AvatarAssetResponse::filetype))
                .toList();
    }

    @Transactional(readOnly = true)
    public FileObjectService.StoredFile getAsset(String assetType, String fileName) {
        if (assetType == null || assetType.isBlank() || fileName == null || fileName.isBlank()) {
            throw new IllegalArgumentException("Asset type and file name are required");
        }

        if (assetType.contains("/") || fileName.contains("/") || fileName.contains("..")) {
            throw new IllegalArgumentException("Invalid avatar asset path");
        }

        String objectKey = AVATAR_ASSET_PREFIX + assetType + "/" + fileName;

        try {
            return fileObjectService.read(objectKey);
        } catch (EntityNotFoundException notFound) {
            return fileObjectService.read("/" + objectKey);
        }
    }

    private String normalizeObjectKey(String objectKey) {
        if (objectKey == null) {
            return "";
        }

        if (objectKey.startsWith("/")) {
            return objectKey.substring(1);
        }

        return objectKey;
    }

    private AvatarAssetResponse parseAssetFromKey(String objectKey) {
        if (!objectKey.startsWith(AVATAR_ASSET_PREFIX)) {
            return null;
        }

        String remainder = objectKey.substring(AVATAR_ASSET_PREFIX.length());
        int categorySeparator = remainder.indexOf('/');
        if (categorySeparator <= 0 || categorySeparator >= remainder.length() - 1) {
            return null;
        }

        String assetType = remainder.substring(0, categorySeparator);
        String fileName = remainder.substring(categorySeparator + 1);

        int extensionSeparator = fileName.lastIndexOf('.');
        if (extensionSeparator <= 0 || extensionSeparator >= fileName.length() - 1) {
            return null;
        }

        String name = fileName.substring(0, extensionSeparator);
        String filetype = fileName.substring(extensionSeparator + 1);

        return new AvatarAssetResponse(assetType, name, filetype);
    }
}
