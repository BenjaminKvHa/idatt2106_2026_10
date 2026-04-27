package com.nettdet.backend.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import com.nettdet.backend.domain.enums.ClassroomAccess;
import com.nettdet.backend.repository.FileObjectRepository;
import com.nettdet.backend.service.FileObjectService;

@Component
@Order(2)
public class MedalAssetInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(MedalAssetInitializer.class);

    private static final List<String> CANDIDATE_ASSET_DIRECTORIES = List.of(
            "/app/initial_medal_assets",
            "../initial_medal_assets",
            "initial_medal_assets");

    private static final Set<String> EXPECTED_FILE_NAMES = Set.of(
            "locked.png",
            "chapter-1.png",
            "chapter-2.png",
            "chapter-3.png",
            "weekly-mystery.png");

    private final FileObjectRepository fileObjectRepository;
    private final FileObjectService fileObjectService;
    private final List<String> candidateAssetDirectories;

    @Autowired
    public MedalAssetInitializer(FileObjectRepository fileObjectRepository, FileObjectService fileObjectService) {
        this(fileObjectRepository, fileObjectService, CANDIDATE_ASSET_DIRECTORIES);
    }

    MedalAssetInitializer(FileObjectRepository fileObjectRepository,
                          FileObjectService fileObjectService,
                          List<String> candidateAssetDirectories) {
        this.fileObjectRepository = fileObjectRepository;
        this.fileObjectService = fileObjectService;
        this.candidateAssetDirectories = candidateAssetDirectories;
    }

    @Override
    public void run(ApplicationArguments args) {
        Path assetDirectory = resolveAssetDirectory();
        if (assetDirectory == null) {
            logger.warn("Medal asset seed skipped (initial_medal_assets folder not found)");
            return;
        }

        warnAboutMissingExpectedFiles(assetDirectory);

        try (Stream<Path> files = Files.walk(assetDirectory, 1)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".png"))
                    .forEach(this::seedMedalAsset);
        } catch (IOException exception) {
            logger.warn("Could not read initial medal assets from {}", assetDirectory, exception);
        }
    }

    private void warnAboutMissingExpectedFiles(Path assetDirectory) {
        for (String fileName : EXPECTED_FILE_NAMES) {
            if (!Files.isRegularFile(assetDirectory.resolve(fileName))) {
                logger.warn("Expected medal asset is missing: {}", assetDirectory.resolve(fileName));
            }
        }
    }

    private void seedMedalAsset(Path path) {
        String fileName = path.getFileName().toString();
        String objectKey = "medal-assets/" + fileName;

        if (fileObjectRepository.findByObjectKey(objectKey).isPresent()) {
            return;
        }

        try {
            byte[] bytes = Files.readAllBytes(path);
            fileObjectService.upload(
                    objectKey,
                    bytes,
                    "image/png",
                    null,
                    null,
                    ClassroomAccess.PUBLIC,
                    fileName);
            logger.info("Seeded medal asset {}", objectKey);
        } catch (IOException exception) {
            logger.warn("Could not read medal asset {}", path, exception);
        }
    }

    private Path resolveAssetDirectory() {
        return candidateAssetDirectories.stream()
                .map(Path::of)
                .filter(Files::isDirectory)
                .findFirst()
                .orElse(null);
    }
}
