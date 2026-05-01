package com.nettdet.backend.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.nettdet.backend.domain.enums.ClassroomAccess;
import com.nettdet.backend.repository.FileObjectRepository;
import com.nettdet.backend.service.FileObjectService;

@Component
@Order(3)
public class DemoAssetInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(DemoAssetInitializer.class);

    private static final List<String> CANDIDATE_ASSET_DIRECTORIES = List.of(
            "/app/initial_demo_assets",
            "../initial_demo_assets",
            "initial_demo_assets");

    private final FileObjectRepository fileObjectRepository;
    private final FileObjectService fileObjectService;

    public DemoAssetInitializer(FileObjectRepository fileObjectRepository, FileObjectService fileObjectService) {
        this.fileObjectRepository = fileObjectRepository;
        this.fileObjectService = fileObjectService;
    }

    @Override
    public void run(ApplicationArguments args) {
        Path assetDirectory = resolveAssetDirectory();
        if (assetDirectory == null) {
            logger.info("Demo asset seed skipped (initial_demo_assets folder not found)");
            return;
        }

        seedDirectory(assetDirectory.resolve("avatars"), "avatar-images");
        seedDirectory(assetDirectory.resolve("weekly_mysteries"), "weekly-mysteries");
    }

    private void seedDirectory(Path directory, String objectKeyPrefix) {
        if (!Files.isDirectory(directory)) {
            logger.info("Demo asset category skipped (folder not found): {}", directory);
            return;
        }

        try (Stream<Path> files = Files.list(directory)) {
            files.filter(Files::isRegularFile)
                    .filter(this::isSupportedImage)
                    .forEach(path -> seedAssetFile(path, objectKeyPrefix));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read demo asset directory: " + directory, exception);
        }
    }

    private void seedAssetFile(Path path, String objectKeyPrefix) {
        String fileName = path.getFileName().toString();
        String objectKey = objectKeyPrefix + "/" + fileName;

        if (fileObjectRepository.findByObjectKey(objectKey).isPresent()) {
            return;
        }

        try {
            fileObjectService.upload(
                    objectKey,
                    Files.readAllBytes(path),
                    determineContentType(fileName),
                    null,
                    null,
                    ClassroomAccess.PUBLIC,
                    fileName);
            logger.info("Seeded demo asset {}", objectKey);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read demo asset file: " + path, exception);
        }
    }

    private boolean isSupportedImage(Path path) {
        String lower = path.getFileName().toString().toLowerCase();
        return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg");
    }

    private String determineContentType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png")) {
            return "image/png";
        }
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        return "application/octet-stream";
    }

    private Path resolveAssetDirectory() {
        for (String candidateDirectory : CANDIDATE_ASSET_DIRECTORIES) {
            Path path = Path.of(candidateDirectory);
            if (Files.isDirectory(path)) {
                return path;
            }
        }
        return null;
    }
}
