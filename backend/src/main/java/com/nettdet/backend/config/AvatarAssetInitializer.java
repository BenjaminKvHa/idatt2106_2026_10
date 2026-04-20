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
@Order(1)
public class AvatarAssetInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(AvatarAssetInitializer.class);

    private static final List<String> CANDIDATE_ASSET_DIRECTORIES = List.of(
            "/app/initial_avatar_assets",
            "../initial_avatar_assets",
            "initial_avatar_assets");

    private final FileObjectRepository fileObjectRepository;
    private final FileObjectService fileObjectService;

    public AvatarAssetInitializer(FileObjectRepository fileObjectRepository, FileObjectService fileObjectService) {
        this.fileObjectRepository = fileObjectRepository;
        this.fileObjectService = fileObjectService;
    }

    @Override
    public void run(ApplicationArguments args) {
        Path assetDirectory = resolveAssetDirectory();
        if (assetDirectory == null) {
            logger.info("Avatar asset seed skipped (initial_avatar_assets folder not found)");
            return;
        }

        try (Stream<Path> categoryDirectories = Files.list(assetDirectory)) {
            categoryDirectories.filter(Files::isDirectory)
                    .forEach(this::seedCategoryDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read initial avatar assets", exception);
        }
    }

    private void seedCategoryDirectory(Path categoryDirectory) {
        String category = categoryDirectory.getFileName().toString();

        try (Stream<Path> files = Files.walk(categoryDirectory, 1)) {
            files.filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".png"))
                    .forEach(path -> seedAssetFile(path, category));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read avatar asset category: " + categoryDirectory, exception);
        }
    }

    private void seedAssetFile(Path path, String category) {
        String fileName = path.getFileName().toString();
        String objectKey = "avatar-assets/" + category + "/" + fileName;

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
            logger.info("Seeded avatar asset {}", objectKey);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read asset file: " + path, exception);
        }
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
