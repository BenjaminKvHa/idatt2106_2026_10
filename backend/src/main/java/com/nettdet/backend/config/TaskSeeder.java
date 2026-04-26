package com.nettdet.backend.config;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.nettdet.backend.domain.enums.ClassroomAccess;
import com.nettdet.backend.domain.enums.ImageCategory;
import com.nettdet.backend.domain.enums.TaskDifficulty;
import com.nettdet.backend.dto.CreateDatacenterImageTaskRequest;
import com.nettdet.backend.dto.CreateDatacenterNewsTaskRequest;
import com.nettdet.backend.dto.CreateImageVerificationTaskRequest;
import com.nettdet.backend.dto.CreateNewsVerificationTaskRequest;
import com.nettdet.backend.repository.FileObjectRepository;
import com.nettdet.backend.service.FileObjectService;
import com.nettdet.backend.service.TaskAdminService;

@Component
@Order(3)
public class TaskSeeder implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(TaskSeeder.class);

    private static final List<String> CANDIDATE_ASSET_DIRECTORIES = List.of(
            "/app/initial_taskImage_assets",
            "../initial_taskImage_assets",
            "initial_taskImage_assets");

    private static final Long CHAPTER_ID_NYHETSKVARTALET = 1L;
    private static final Long CHAPTER_ID_FOTOGRAFEN = 2L;

    private final FileObjectService fileObjectService;
    private final FileObjectRepository fileObjectRepository;
    private final TaskAdminService taskAdminService;

    public TaskSeeder(FileObjectService fileObjectService,
                      FileObjectRepository fileObjectRepository,
                      TaskAdminService taskAdminService) {
        this.fileObjectService = fileObjectService;
        this.fileObjectRepository = fileObjectRepository;
        this.taskAdminService = taskAdminService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Path assetDirectory = resolveAssetDirectory();
        if (assetDirectory == null) {
            logger.info("Task seeding skipped (initial_taskImage_assets folder not found)");
            return;
        }

        logger.info("Starting task seeding from {}", assetDirectory);

        try {
            seedNyhetskvartalet(assetDirectory.resolve("chap1"));
            seedFotografen(assetDirectory.resolve("chap2"));
            seedDatasenteret(assetDirectory.resolve("chap3"));
            logger.info("Task seeding completed successfully");
        } catch (Exception e) {
            logger.error("Task seeding failed", e);
            throw new RuntimeException("Task seeding failed", e);
        }
    }

    // --------------------------------------------------
    // NYHETSKVARTALET (chap1)
    // --------------------------------------------------

    private void seedNyhetskvartalet(Path chapDir) throws IOException {
        if (!Files.exists(chapDir)) {
            logger.warn("Nyhetskvartalet directory not found: {}", chapDir);
            return;
        }

        for (TaskDifficulty difficulty : List.of(TaskDifficulty.EASY, TaskDifficulty.MEDIUM, TaskDifficulty.HARD)) {
            Path diffDir = chapDir.resolve(difficulty.name().toLowerCase());
            if (!Files.exists(diffDir)) continue;

            try (Stream<Path> taskDirs = Files.list(diffDir).filter(Files::isDirectory).sorted()) {
                taskDirs.forEach(taskDir -> seedNyhetskvartaletTask(taskDir, difficulty));
            }
        }
    }

    private void seedNyhetskvartaletTask(Path taskDir, TaskDifficulty difficulty) {
        String taskName = taskDir.getFileName().toString();
        try {
            Path ekteFile = Files.list(taskDir)
                    .filter(p -> p.getFileName().toString().contains("_ekte"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No ekte file in " + taskDir));
            Path falskFile = Files.list(taskDir)
                    .filter(p -> p.getFileName().toString().contains("_falsk"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No falsk file in " + taskDir));

            String ekteObjectKey = buildObjectKey("chap1", difficulty.name().toLowerCase(), taskName, ekteFile.getFileName().toString());
            String falskObjectKey = buildObjectKey("chap1", difficulty.name().toLowerCase(), taskName, falskFile.getFileName().toString());

            Long ekteFileId = getOrUploadFile(ekteObjectKey, ekteFile);
            Long falskFileId = getOrUploadFile(falskObjectKey, falskFile);

            CreateNewsVerificationTaskRequest request = new CreateNewsVerificationTaskRequest(
                    CHAPTER_ID_NYHETSKVARTALET,
                    difficulty,
                    "To avisforsider ligger foran deg. Én er ekte og én er falsk. Hvilken er falsk?",
                    ekteFileId,
                    falskFileId,
                    falskFileId, // correct = the fake one
                    "Riktig! Avisforsiden du valgte var den falske.",
                    "Ikke helt. Den andre forsiden var faktisk den falske."
            );

            taskAdminService.createNewsVerificationTask(request);
            logger.info("Created Nyhetskvartalet task for {} {} {}", difficulty, taskName, ekteFileId);

        } catch (IOException e) {
            throw new RuntimeException("Failed to seed Nyhetskvartalet task: " + taskDir, e);
        }
    }

    // --------------------------------------------------
    // FOTOGRAFEN (chap2)
    // --------------------------------------------------

    private void seedFotografen(Path chapDir) throws IOException {
        if (!Files.exists(chapDir)) {
            logger.warn("Fotografen directory not found: {}", chapDir);
            return;
        }

        for (TaskDifficulty difficulty : List.of(TaskDifficulty.EASY, TaskDifficulty.MEDIUM, TaskDifficulty.HARD)) {
            Path diffDir = chapDir.resolve(difficulty.name().toLowerCase());
            if (!Files.exists(diffDir)) continue;

            try (Stream<Path> taskDirs = Files.list(diffDir).filter(Files::isDirectory).sorted()) {
                taskDirs.forEach(taskDir -> seedFotografenTask(taskDir, difficulty));
            }
        }
    }

    private void seedFotografenTask(Path taskDir, TaskDifficulty difficulty) {
        String taskName = taskDir.getFileName().toString();
        try {
            // Each task dir has one or two images; we create a task per image
            List<Path> imageFiles = Files.list(taskDir)
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
                    })
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();

            for (Path imageFile : imageFiles) {
                String fileName = imageFile.getFileName().toString();
                String objectKey = buildObjectKey("chap2", difficulty.name().toLowerCase(), taskName, fileName);
                Long fileId = getOrUploadFile(objectKey, imageFile);

                ImageCategory correctCategory = determineCategory(fileName);
                List<String> justifications = getJustifications(fileName);
                String correctJustification = getCorrectJustification(fileName, correctCategory);

                CreateImageVerificationTaskRequest request = new CreateImageVerificationTaskRequest(
                        CHAPTER_ID_FOTOGRAFEN,
                        difficulty,
                        "Er dette bildet ekte, KI-generert eller manipulert?",
                        fileId,
                        correctCategory,
                        correctJustification,
                        justifications,
                        "Riktig! " + getCorrectDescription(correctCategory),
                        "Ikke helt. " + getFalseDescription(correctCategory)
                );

                taskAdminService.createImageVerificationTask(request);
                logger.info("Created Fotografen task for {} {} {} (category={})", difficulty, taskName, fileId, correctCategory);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to seed Fotografen task: " + taskDir, e);
        }
    }

    // --------------------------------------------------
    // DATASENTERET (chap3 — final boss)
    // --------------------------------------------------

    private void seedDatasenteret(Path chapDir) throws IOException {
        if (!Files.exists(chapDir)) {
            logger.warn("Datasenteret directory not found: {}", chapDir);
            return;
        }

        Path newsDir = chapDir.resolve("news");
        if (Files.exists(newsDir)) {
            try (Stream<Path> taskDirs = Files.list(newsDir).filter(Files::isDirectory).sorted()) {
                taskDirs.forEach(this::seedDatasenteretNewsTask);
            }
        }

        Path imageDir = chapDir.resolve("image");
        if (Files.exists(imageDir)) {
            try (Stream<Path> taskDirs = Files.list(imageDir).filter(Files::isDirectory).sorted()) {
                taskDirs.forEach(this::seedDatasenteretImageTask);
            }
        }
    }

    private void seedDatasenteretNewsTask(Path taskDir) {
        String taskName = taskDir.getFileName().toString();
        try {
            Path ekteFile = Files.list(taskDir)
                    .filter(p -> p.getFileName().toString().contains("_ekte"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No ekte file in " + taskDir));
            Path falskFile = Files.list(taskDir)
                    .filter(p -> p.getFileName().toString().contains("_falsk"))
                    .findFirst()
                    .orElseThrow(() -> new IllegalStateException("No falsk file in " + taskDir));

            String ekteObjectKey = buildObjectKey("chap3", "news", taskName, ekteFile.getFileName().toString());
            String falskObjectKey = buildObjectKey("chap3", "news", taskName, falskFile.getFileName().toString());

            Long ekteFileId = getOrUploadFile(ekteObjectKey, ekteFile);
            Long falskFileId = getOrUploadFile(falskObjectKey, falskFile);

            CreateDatacenterNewsTaskRequest request = new CreateDatacenterNewsTaskRequest(
                    "To avisforsider ligger foran deg. Én er ekte og én er falsk. Hvilken er falsk?",
                    ekteFileId,
                    falskFileId,
                    falskFileId,
                    "Riktig! Avisforsiden du valgte var den falske.",
                    "Ikke helt. Den andre forsiden var faktisk den falske."
            );

            taskAdminService.createDatacenterNewsTask(request);
            logger.info("Created Datasenteret news task for {} (ekte={}, falsk={})", taskName, ekteFileId, falskFileId);

        } catch (IOException e) {
            throw new RuntimeException("Failed to seed Datasenteret news task: " + taskDir, e);
        }
    }

    private void seedDatasenteretImageTask(Path taskDir) {
        String taskName = taskDir.getFileName().toString();
        try {
            List<Path> imageFiles = Files.list(taskDir)
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        return name.endsWith(".png") || name.endsWith(".jpg") || name.endsWith(".jpeg");
                    })
                    .sorted(Comparator.comparing(p -> p.getFileName().toString()))
                    .toList();

            for (Path imageFile : imageFiles) {
                String fileName = imageFile.getFileName().toString();
                String objectKey = buildObjectKey("chap3", "image", taskName, fileName);
                Long fileId = getOrUploadFile(objectKey, imageFile);

                ImageCategory correctCategory = determineCategory(fileName);
                List<String> justifications = getJustifications(fileName);
                String correctJustification = getCorrectJustification(fileName, correctCategory);

                CreateDatacenterImageTaskRequest request = new CreateDatacenterImageTaskRequest(
                        "Er dette bildet ekte, KI-generert eller manipulert?",
                        fileId,
                        correctCategory,
                        correctJustification,
                        justifications,
                        "Riktig! " + getCorrectDescription(correctCategory),
                        "Ikke helt. " + getFalseDescription(correctCategory)
                );

                taskAdminService.createDatacenterImageTask(request);
                logger.info("Created Datasenteret image task for {} {} (category={})", taskName, fileId, correctCategory);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to seed Datasenteret image task: " + taskDir, e);
        }
    }

    // --------------------------------------------------
    // HELPERS
    // --------------------------------------------------

    private Long getOrUploadFile(String objectKey, Path filePath) throws IOException {
        String contentType = determineContentType(filePath.getFileName().toString());
        String fileName = filePath.getFileName().toString();

        // First check if file already exists in database
        var existing = fileObjectRepository.findByObjectKey(objectKey);
        if (existing.isPresent()) {
            logger.debug("File already exists in database: {} (id={})", objectKey, existing.get().getId());
            return existing.get().getId();
        }

        // Read from filesystem and upload to MinIO
        byte[] bytes = Files.readAllBytes(filePath);
        var result = fileObjectService.upload(
                objectKey,
                bytes,
                contentType,
                null, // uploadedBy - system upload
                null, // classroom - global task asset
                ClassroomAccess.PUBLIC,
                fileName);

        logger.debug("Uploaded file to MinIO: {} (id={})", objectKey, result.getId());
        return result.getId();
    }

    private String buildObjectKey(String chapter, String difficulty, String task, String fileName) {
        return "task-assets/" + chapter + "/" + difficulty + "/" + task + "/" + fileName;
    }

    private String determineContentType(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }

    private ImageCategory determineCategory(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.contains("_ekte")) return ImageCategory.REAL;
        if (lower.contains("_falsk") || lower.contains("_ki")) return ImageCategory.AI_GENERATED;
        if (lower.contains("_manipulert")) return ImageCategory.MANIPULATED;
        throw new IllegalArgumentException(
                "Could not determine ImageCategory for file: " + fileName
                        + " (expected suffix _ekte, _falsk, _ki, or _manipulert before extension)");
    }

    private String getCorrectDescription(ImageCategory category) {
        return switch (category) {
            case REAL -> "Dette bildet er ekte.";
            case AI_GENERATED -> "Dette bildet er KI-generert.";
            case MANIPULATED -> "Dette bildet er manipulert.";
        };
    }

    private String getFalseDescription(ImageCategory category) {
        return switch (category) {
            case REAL -> "Dette bildet er faktisk ekte.";
            case AI_GENERATED -> "Dette bildet er KI-generert.";
            case MANIPULATED -> "Dette bildet er manipulert.";
        };
    }

    private List<String> getJustifications(String fileName) {
        String key = fileName.toLowerCase();

        // Each list follows the pattern [real-reason, fake-reason, distractor].
        // getCorrectJustification picks index 0 for REAL and index 1 for AI_GENERATED/MANIPULATED,
        // so an ekte image and its falsk/manipulert pair share the same list.
        Map<String, List<String>> justifications = Map.ofEntries(
                // Easy tasks
                Map.entry("fotografen_dame_ekte.png", List.of(
                        "Detaljer og skygger i ansiktet ser naturlige ut",
                        "Ansiktet er tydelig photoshoppet med unaturlige redigeringer",
                        "Bildet er tatt i feil vinkel")),
                Map.entry("fotografen_dame_manipulert.png", List.of(
                        "Detaljer og skygger i ansiktet ser naturlige ut",
                        "Ansiktet er tydelig photoshoppet med unaturlige redigeringer",
                        "Bildet er tatt i feil vinkel")),
                Map.entry("fotografen_folkemengde_ekte.jpg", List.of(
                        "Scenen ser naturlig og levende ut",
                        "Perspektivet virker rart og folk smiler unaturlig likt",
                        "Bildet er i svart-hvitt")),
                Map.entry("fotografen_folkemengde_falsk.png", List.of(
                        "Scenen ser naturlig og levende ut",
                        "Perspektivet virker rart og folk smiler unaturlig likt",
                        "Bildet er i svart-hvitt")),
                Map.entry("fotografen_jorda_ekte.jpg", List.of(
                        "Detaljnivået ser realistisk ut, slik et ekte kamera fanger det",
                        "Kvaliteten er unaturlig skarp og perfekt, typisk for KI-bilder",
                        "Fargene er for matte")),
                Map.entry("fotografen_jorda_falsk.png", List.of(
                        "Detaljnivået ser realistisk ut, slik et ekte kamera fanger det",
                        "Kvaliteten er unaturlig skarp og perfekt, typisk for KI-bilder",
                        "Fargene er for matte")),

                // Medium tasks
                Map.entry("fotografen_asiatisk_dame_ekte.jpg", List.of(
                        "Hudtekstur og ansiktstrekk henger naturlig sammen",
                        "Hudteksturen passer ikke med resten av ansiktet og trekkene virker unaturlige",
                        "Bakgrunnen er uskarp")),
                Map.entry("fotografen_asiatisk_dame_manipulert.jpg", List.of(
                        "Hudtekstur og ansiktstrekk henger naturlig sammen",
                        "Hudteksturen passer ikke med resten av ansiktet og trekkene virker unaturlige",
                        "Bakgrunnen er uskarp")),
                Map.entry("fotografen_blomster_ekte.jpg", List.of(
                        "Teksturene på blomstene ser naturlige ut",
                        "Teksturene ser unaturlige og for glatte ut",
                        "Lyset kommer fra feil retning")),
                Map.entry("fotografen_blomster_falsk.png", List.of(
                        "Teksturene på blomstene ser naturlige ut",
                        "Teksturene ser unaturlige og for glatte ut",
                        "Lyset kommer fra feil retning")),
                Map.entry("fotografen_naturen_falsk.png", List.of(
                        "Landskapet ser naturlig og variert ut",
                        "Objekter er duplisert og himmelen virker kunstig",
                        "Bildet er tatt fra en drone")),

                // Hard tasks
                Map.entry("fotografen_katt_ekte.jpg", List.of(
                        "Pelsen og øynene ser naturlige ut",
                        "Pelsen og øynene er unaturlig skarpe og virker kunstige",
                        "Katten har seks poter")),
                Map.entry("fotografen_katt_falsk.png", List.of(
                        "Pelsen og øynene ser naturlige ut",
                        "Pelsen og øynene er unaturlig skarpe og virker kunstige",
                        "Katten har seks poter")),
                Map.entry("fotografen_naturbilde_ekte.jpg", List.of(
                        "Bildet har naturlig kvalitet, lys og detaljer",
                        "Kvaliteten er unaturlig skarp og perfekt, typisk for KI-bilder",
                        "Horisonten er krøket")),
                Map.entry("fotografen_naturbilde_falsk.png", List.of(
                        "Bildet har naturlig kvalitet, lys og detaljer",
                        "Kvaliteten er unaturlig skarp og perfekt, typisk for KI-bilder",
                        "Horisonten er krøket")),
                Map.entry("fotografen_ulykke_ekte.jpg", List.of(
                        "Detaljer, skygger og bevegelsessløring stemmer med hendelsen",
                        "Scenen ser for dramatisk ut til å være ekte",
                        "Fargene er oppjustert i etterkant")),

                // Datasenteret (chap3) image tasks
                Map.entry("datasenteret_foto_maleri_ekte.jpg", List.of(
                        "Penselstrøk og lerretstekstur ser autentiske ut",
                        "Detaljene er for jevne og symmetriske til å være malt for hånd",
                        "Maleriet er signert i feil hjørne")),
                Map.entry("datasenteret_foto_maleri_ki.png", List.of(
                        "Penselstrøk og lerretstekstur ser autentiske ut",
                        "Detaljene er for jevne og symmetriske til å være malt for hånd",
                        "Maleriet er signert i feil hjørne")),
                Map.entry("datasenteret_foto_bygning_ekte.jpg", List.of(
                        "Linjer, perspektiv og materialer ser realistiske ut",
                        "Bygningens geometri og vinduer virker unaturlig perfekte",
                        "Bygningen ligger i feil by")),
                Map.entry("datasenteret_foto_bygning_ki.png", List.of(
                        "Linjer, perspektiv og materialer ser realistiske ut",
                        "Bygningens geometri og vinduer virker unaturlig perfekte",
                        "Bygningen ligger i feil by")),
                Map.entry("datasenteret_foto_dame_manipulert.png", List.of(
                        "Hudtekstur og lys passer naturlig sammen",
                        "Ansiktstrekk og bakgrunn er tydelig redigert i etterkant",
                        "Bildet er tatt med mobilkamera")),
                Map.entry("datasenteret_foto_mcdonalds_ki.png", List.of(
                        "Skiltet og omgivelsene ser ekte og slitt ut",
                        "Logo og typografi er litt forvrengt, typisk for KI-bilder",
                        "Restauranten ligger på feil sted"))
        );

        return justifications.getOrDefault(key, List.of(
                "Detaljer og skygger ser naturlige ut",
                "Bildet har tydelige tegn på KI-generering eller manipulering",
                "Bakgrunnen ser rar ut"));
    }

    private String getCorrectJustification(String fileName, ImageCategory category) {
        List<String> justs = getJustifications(fileName);
        return switch (category) {
            case REAL -> justs.get(0);
            case AI_GENERATED, MANIPULATED -> justs.get(1);
        };
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
