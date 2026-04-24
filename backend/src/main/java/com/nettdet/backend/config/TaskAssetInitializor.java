package com.nettdet.backend.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.nettdet.backend.domain.enums.ClassroomAccess;
import com.nettdet.backend.repository.FileObjectRepository;
import com.nettdet.backend.service.FileObjectService;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Component
@Order(2)
public class TaskAssetInitializor implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(TaskAssetInitializor.class);

    private static final long TASK_ASSET_ID_START = 100L;

    private static final List<String> CANDIDATE_ASSET_DIRECTORIES = List.of(
            "/app/initial_taskImage_assets",
            "../initial_taskImage_assets",
            "initial_taskImage_assets");

    private final FileObjectRepository fileObjectRepository;
    private final FileObjectService fileObjectService;

    @PersistenceContext
    private EntityManager entityManager;

    public TaskAssetInitializor(FileObjectRepository fileObjectRepository, FileObjectService fileObjectService) {
        this.fileObjectRepository = fileObjectRepository;
        this.fileObjectService = fileObjectService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Path assetDirectory = resolveAssetDirectory();
        if (assetDirectory == null) {
            logger.info("Task asset seed skipped (initial_taskImage_assets folder not found)");
            return;
        }

        bumpSequenceToTaskAssetStart();

        try (Stream<Path> chapterDirectories = Files.list(assetDirectory)) {
            chapterDirectories
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .forEach(this::seedChapterDirectory);
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read initial task assets", exception);
        }
    }

    private void bumpSequenceToTaskAssetStart() {
        entityManager.createNativeQuery(
                "SELECT setval('file_object_id_seq', GREATEST(COALESCE((SELECT MAX(id) FROM file_object), 0), "
                        + (TASK_ASSET_ID_START - 1) + "), true)")
                .getSingleResult();
    }

    private void seedChapterDirectory(Path chapterDirectory) {
        String chapter = chapterDirectory.getFileName().toString();
        try (Stream<Path> difficultyDirectories = Files.list(chapterDirectory)) {
            difficultyDirectories
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .forEach(difficultyDir -> seedDifficultyDirectory(difficultyDir, chapter));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read chapter directory: " + chapterDirectory, exception);
        }
    }

    private void seedDifficultyDirectory(Path difficultyDirectory, String chapter) {
        String difficulty = difficultyDirectory.getFileName().toString();
        try (Stream<Path> taskDirectories = Files.list(difficultyDirectory)) {
            taskDirectories
                    .filter(Files::isDirectory)
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .forEach(taskDir -> seedTaskDirectory(taskDir, chapter, difficulty));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read difficulty directory: " + difficultyDirectory, exception);
        }
    }

    private void seedTaskDirectory(Path taskDirectory, String chapter, String difficulty) {
        String task = taskDirectory.getFileName().toString();
        try (Stream<Path> files = Files.list(taskDirectory)) {
            files
                    .filter(Files::isRegularFile)
                    .filter(path -> path.getFileName().toString().toLowerCase().endsWith(".png"))
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .forEach(path -> seedAssetFile(path, chapter, difficulty, task));
        } catch (IOException exception) {
            throw new IllegalStateException("Could not read task directory: " + taskDirectory, exception);
        }
    }

    private void seedAssetFile(Path path, String chapter, String difficulty, String task) {
        String fileName = path.getFileName().toString();
        String objectKey = "task-assets/" + chapter + "/" + difficulty + "/" + task + "/" + fileName;

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
            logger.info("Seeded task asset {}", objectKey);
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
