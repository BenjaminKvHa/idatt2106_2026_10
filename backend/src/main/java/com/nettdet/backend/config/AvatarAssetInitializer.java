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

/**
 * {@code AvatarAssetInitializer} is a Spring Boot {@link ApplicationRunner} responsible for
 * bootstrapping the system with default avatar assets.
 * <p>
 * During application startup, this component scans a set of candidate directories for
 * image files (specifically {@code .png}) organized into subdirectories representing
 * asset categories (e.g., "hair", "costume", "person").
 * </p>
 * <p>
 * The initialization process is idempotent; it verifies whether an asset already exists in
 * the {@link FileObjectRepository} using its generated S3 object key before attempting
 * an upload via {@link FileObjectService}.
 * </p>
 * <p>
 * This component is marked with {@code @Order(1)} to ensure that core assets are available
 * before other seeders or application logic that might depend on them are executed.
 * </p>
 */
@Component
@Order(1)
public class AvatarAssetInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(AvatarAssetInitializer.class);

    /**
     * Paths where the initializer looks for the {@code initial_avatar_assets} directory.
     * This supports different environments such as Docker containers ({@code /app/...}),
     * multi-module project structures, and standard execution roots.
     */
    private static final List<String> CANDIDATE_ASSET_DIRECTORIES = List.of(
            "/app/initial_avatar_assets",
            "../initial_avatar_assets",
            "initial_avatar_assets");

    private final FileObjectRepository fileObjectRepository;
    private final FileObjectService fileObjectService;

    /**
     * Constructs a new {@code AvatarAssetInitializer} with the required repository and service.
     *
     * @param fileObjectRepository the repository used to check for existing asset records
     * @param fileObjectService the service used to upload and persist new avatar assets
     */
    public AvatarAssetInitializer(FileObjectRepository fileObjectRepository, FileObjectService fileObjectService) {
        this.fileObjectRepository = fileObjectRepository;
        this.fileObjectService = fileObjectService;
    }

    /**
     * Orchestrates the avatar asset seeding process.
     * <p>
     * Resolves the asset directory and iterates through each subdirectory. Each subdirectory
     * name is treated as a category, and its contents are processed for seeding.
     * </p>
     *
     * @param args incoming application arguments (unused)
     * @throws IllegalStateException if the asset directory is found but cannot be read
     */
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

    /**
     * Seeds all valid PNG files within a specific category directory.
     *
     * @param categoryDirectory the path to the directory containing assets for a single category
     * @throws IllegalStateException if an I/O error occurs while walking the directory
     */
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

    /**
     * Checks for the existence of an asset and uploads it if it is missing.
     * <p>
     * The object key is constructed as {@code avatar-assets/{category}/{fileName}}.
     * Assets are uploaded with {@link ClassroomAccess#PUBLIC} visibility to allow
     * unauthenticated or cross-classroom access for avatar customization.
     * </p>
     *
     * @param path the local filesystem path to the asset file
     * @param category the category name derived from the parent directory
     * @throws IllegalStateException if the file cannot be read for upload
     */
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

    /**
     * Attempts to find the first existing directory from {@link #CANDIDATE_ASSET_DIRECTORIES}.
     *
     * @return the resolved {@link Path}, or {@code null} if no candidate directory exists
     */
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
