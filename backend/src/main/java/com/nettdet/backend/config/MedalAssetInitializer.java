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

/**
 * {@code MedalAssetInitializer} is a Spring Boot {@link ApplicationRunner} responsible for
 * bootstrapping achievement-related assets (medals) into the system.
 * <p>
 * This component scans candidate directories for specific image files required for the
 * gamification aspects of the platform, such as chapter completion medals and mystery
 * task rewards.
 * </p>
 * <p>
 * Unlike {@link AvatarAssetInitializer}, this initializer includes a validation step that
 * logs warnings if a predefined set of "expected" medal files (e.g., {@code locked.png},
 * {@code chapter-1.png}) is missing from the resolved asset directory.
 * </p>
 * <p>
 * Execution is marked with {@code @Order(3)}, ensuring it runs after chapters have been
 * seeded but early enough for the system to be fully functional on first launch.
 * </p>
 */
@Component
@Order(3)
public class MedalAssetInitializer implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(MedalAssetInitializer.class);

    /**
     * Default paths where the initializer looks for the {@code initial_medal_assets} directory.
     */
    private static final List<String> CANDIDATE_ASSET_DIRECTORIES = List.of(
            "/app/initial_medal_assets",
            "../initial_medal_assets",
            "initial_medal_assets");

    /**
     * A set of file names that are considered essential for the application's UI.
     * The absence of these files will trigger a warning log.
     */
    private static final Set<String> EXPECTED_FILE_NAMES = Set.of(
            "locked.png",
            "chapter-1.png",
            "chapter-2.png",
            "chapter-3.png",
            "weekly-mystery.png");

    private final FileObjectRepository fileObjectRepository;
    private final FileObjectService fileObjectService;
    private final List<String> candidateAssetDirectories;

    /**
     * Primary constructor used by Spring for dependency injection.
     *
     * @param fileObjectRepository the repository used to check for existing asset records
     * @param fileObjectService the service used to upload and persist new medal assets
     */
    @Autowired
    public MedalAssetInitializer(FileObjectRepository fileObjectRepository, FileObjectService fileObjectService) {
        this(fileObjectRepository, fileObjectService, CANDIDATE_ASSET_DIRECTORIES);
    }

    /**
     * Internal constructor providing flexibility for testing with custom candidate directories.
     *
     * @param fileObjectRepository the repository used to check for existing asset records
     * @param fileObjectService the service used to upload and persist new medal assets
     * @param candidateAssetDirectories list of directory paths to scan for assets
     */
    MedalAssetInitializer(FileObjectRepository fileObjectRepository,
                          FileObjectService fileObjectService,
                          List<String> candidateAssetDirectories) {
        this.fileObjectRepository = fileObjectRepository;
        this.fileObjectService = fileObjectService;
        this.candidateAssetDirectories = candidateAssetDirectories;
    }

    /**
     * Orchestrates the medal asset seeding process.
     * <p>
     * Resolves the asset directory, validates the presence of expected files, and
     * uploads any new PNG files found in the root of the asset directory.
     * </p>
     *
     * @param args incoming application arguments (unused)
     */
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

    /**
     * Checks the resolved directory against {@link #EXPECTED_FILE_NAMES} and logs
     * a warning for any missing files.
     *
     * @param assetDirectory the path to the resolved medal assets directory
     */
    private void warnAboutMissingExpectedFiles(Path assetDirectory) {
        for (String fileName : EXPECTED_FILE_NAMES) {
            if (!Files.isRegularFile(assetDirectory.resolve(fileName))) {
                logger.warn("Expected medal asset is missing: {}", assetDirectory.resolve(fileName));
            }
        }
    }

    /**
     * Checks for the existence of a medal asset and uploads it if missing.
     * <p>
     * The object key is constructed as {@code medal-assets/{fileName}}.
     * Assets are uploaded with {@link ClassroomAccess#PUBLIC} visibility.
     * </p>
     *
     * @param path the local filesystem path to the medal asset file
     */
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

    /**
     * Heuristically resolves the medal asset directory from the list of candidates.
     *
     * @return the first valid directory found, or {@code null} if none exist
     */
    private Path resolveAssetDirectory() {
        return candidateAssetDirectories.stream()
                .map(Path::of)
                .filter(Files::isDirectory)
                .findFirst()
                .orElse(null);
    }
}
