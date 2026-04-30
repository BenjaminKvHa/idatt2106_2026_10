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

import com.nettdet.backend.domain.Chapter;
import com.nettdet.backend.domain.enums.ClassroomAccess;
import com.nettdet.backend.domain.enums.DatacenterTaskFormat;
import com.nettdet.backend.domain.enums.ImageCategory;
import com.nettdet.backend.domain.enums.TaskDifficulty;
import com.nettdet.backend.dto.DatacenterTask;
import com.nettdet.backend.dto.ImageVerificationTask;
import com.nettdet.backend.dto.NewsVerificationTask;
import com.nettdet.backend.repository.ChapterRepository;
import com.nettdet.backend.repository.FileObjectRepository;
import com.nettdet.backend.service.FileObjectService;
import com.nettdet.backend.service.TaskAdminService;

import jakarta.persistence.EntityNotFoundException;

/**
 * {@code TaskSeeder} is the primary content ingestion component for the application.
 * <p>
 * This {@link ApplicationRunner} scans a structured directory of task assets to populate
 * the database with educational content. It handles multiple task types, including:
 * <ul>
 *     <li><b>News Verification:</b> Comparing real vs. fake news articles.</li>
 *     <li><b>Image Verification:</b> Identifying AI-generated, manipulated, or real photos.</li>
 *     <li><b>Datacenter Tasks:</b> Complex, multi-format "final boss" challenges.</li>
 * </ul>
 * </p>
 * <p>
 * The seeder follows a hierarchical directory convention:
 * {@code {chapter}/{difficulty}/{task_name}/{assets}}. Asset metadata (like whether a photo
 * is real or AI-generated) is derived from filename suffixes (e.g., {@code _ekte}, {@code _ki}).
 * </p>
 * <p>
 * This component runs with {@code @Order(4)}, ensuring that chapters and core S3
 * infrastructure are already initialized.
 * </p>
 */
@Component
@Order(4)
public class TaskSeeder implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(TaskSeeder.class);

    private static final List<String> CANDIDATE_ASSET_DIRECTORIES = List.of(
            "/app/initial_taskImage_assets",
            "../initial_taskImage_assets",
            "initial_taskImage_assets");

    private static final Long CHAPTER_ID_NYHETSKVARTALET = 1L;
    private static final Long CHAPTER_ID_FOTOGRAFEN = 2L;
    private static final Long CHAPTER_ID_DATASENTERET = 3L;

    private final FileObjectService fileObjectService;
    private final FileObjectRepository fileObjectRepository;
    private final ChapterRepository chapterRepository;
    private final TaskAdminService taskAdminService;

    /**
     * Constructs a {@code TaskSeeder} with its required dependencies.
     *
     * @param fileObjectService service for uploading task assets to S3
     * @param fileObjectRepository repository for verifying asset existence
     * @param chapterRepository repository for loading chapter entities
     * @param taskAdminService service for creating task domain objects
     */
    public TaskSeeder(FileObjectService fileObjectService,
                      FileObjectRepository fileObjectRepository,
                      ChapterRepository chapterRepository,
                      TaskAdminService taskAdminService) {
        this.fileObjectService = fileObjectService;
        this.fileObjectRepository = fileObjectRepository;
        this.chapterRepository = chapterRepository;
        this.taskAdminService = taskAdminService;
    }

    private Chapter loadChapter(Long id) {
        return chapterRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Chapter not found: " + id));
    }

    /**
     * Orchestrates the seeding of tasks for all chapters.
     *
     * @param args incoming application arguments (unused)
     * @throws RuntimeException if the seeding process fails due to I/O or database errors
     */
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

    /**
     * Seeds tasks for Chapter 1: Nyhetskvartalet.
     * <p>
     * This chapter focuses on news verification, comparing pairs of "ekte" (real)
     * and "falsk" (fake) newspaper front pages.
     * </p>
     *
     * @param chapDir the base directory for Chapter 1 assets
     * @throws IOException if the filesystem cannot be scanned
     */
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

    /**
     * Seeds a single news verification task.
     *
     * @param taskDir directory containing the pair of news images
     * @param difficulty the assigned difficulty level
     */
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

            String ekteName = ekteFile.getFileName().toString();
            NewsVerificationTask taskData = new NewsVerificationTask(
                    "To avisforsider ligger foran deg. Én er ekte og én er falsk. Hvilken er falsk?",
                    ekteFileId,
                    falskFileId,
                    falskFileId, // correct = the fake one
                    getNewsCorrectDescription(ekteName),
                    getNewsFalseDescription(ekteName)
            );

            taskAdminService.createTask(loadChapter(CHAPTER_ID_NYHETSKVARTALET), difficulty, taskData);
            logger.info("Created Nyhetskvartalet task for {} {} {}", difficulty, taskName, ekteFileId);

        } catch (IOException e) {
            throw new RuntimeException("Failed to seed Nyhetskvartalet task: " + taskDir, e);
        }
    }

    // --------------------------------------------------
    // FOTOGRAFEN (chap2)
    // --------------------------------------------------

    /**
     * Seeds tasks for Chapter 2: Fotografen.
     * <p>
     * This chapter focuses on image source identification, categorizing images as
     * real, AI-generated, or manipulated.
     * </p>
     *
     * @param chapDir the base directory for Chapter 2 assets
     * @throws IOException if the filesystem cannot be scanned
     */
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

    /**
     * Seeds tasks derived from images in a specific directory.
     *
     * @param taskDir directory containing image files for the task
     * @param difficulty the assigned difficulty level
     */
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

                ImageVerificationTask taskData = new ImageVerificationTask(
                        "Er dette bildet ekte, KI-generert eller manipulert?",
                        fileId,
                        correctCategory,
                        correctJustification,
                        justifications,
                        getImageCorrectDescription(fileName, correctCategory),
                        getImageFalseDescription(fileName, correctCategory)
                );

                taskAdminService.createTask(loadChapter(CHAPTER_ID_FOTOGRAFEN), difficulty, taskData);
                logger.info("Created Fotografen task for {} {} {} (category={})", difficulty, taskName, fileId, correctCategory);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to seed Fotografen task: " + taskDir, e);
        }
    }

    // --------------------------------------------------
    // DATASENTERET (chap3 — final boss)
    // --------------------------------------------------

    /**
     * Seeds tasks for Chapter 3: Datasenteret.
     * <p>
     * This chapter combines both news and image verification formats into a high-stakes
     * "final boss" sequence.
     * </p>
     *
     * @param chapDir the base directory for Chapter 3 assets
     * @throws IOException if the filesystem cannot be scanned
     */
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

    /**
     * Seeds a news verification task for the Datasenteret chapter.
     *
     * @param taskDir directory containing the pair of news images
     */
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

            String ekteName = ekteFile.getFileName().toString();
            NewsVerificationTask inner = new NewsVerificationTask(
                    "To avisforsider ligger foran deg. Én er ekte og én er falsk. Hvilken er falsk?",
                    ekteFileId,
                    falskFileId,
                    falskFileId,
                    getNewsCorrectDescription(ekteName),
                    getNewsFalseDescription(ekteName)
            );
            DatacenterTask wrapped = new DatacenterTask(DatacenterTaskFormat.NEWS_VERIFICATION, null, inner);

            taskAdminService.createTask(loadChapter(CHAPTER_ID_DATASENTERET), TaskDifficulty.HARD, wrapped);
            logger.info("Created Datasenteret news task for {} (ekte={}, falsk={})", taskName, ekteFileId, falskFileId);

        } catch (IOException e) {
            throw new RuntimeException("Failed to seed Datasenteret news task: " + taskDir, e);
        }
    }

    /**
     * Seeds an image verification task for the Datasenteret chapter.
     *
     * @param taskDir directory containing image files for the task
     */
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

                ImageVerificationTask inner = new ImageVerificationTask(
                        "Er dette bildet ekte, KI-generert eller manipulert?",
                        fileId,
                        correctCategory,
                        correctJustification,
                        justifications,
                        getImageCorrectDescription(fileName, correctCategory),
                        getImageFalseDescription(fileName, correctCategory)
                );
                DatacenterTask wrapped = new DatacenterTask(DatacenterTaskFormat.IMAGE_VERIFICATION, inner, null);

                taskAdminService.createTask(loadChapter(CHAPTER_ID_DATASENTERET), TaskDifficulty.HARD, wrapped);
                logger.info("Created Datasenteret image task for {} {} (category={})", taskName, fileId, correctCategory);
            }

        } catch (IOException e) {
            throw new RuntimeException("Failed to seed Datasenteret image task: " + taskDir, e);
        }
    }

    // --------------------------------------------------
    // HELPERS
    // --------------------------------------------------

    /**
     * Checks if a file exists in the database and uploads it to S3 if missing.
     *
     * @param objectKey the unique S3 key for the asset
     * @param filePath the local filesystem path to the asset
     * @return the unique ID of the {@code FileObject} record
     * @throws IOException if the file cannot be read
     */
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

    /**
     * Determines the {@link ImageCategory} based on filename suffixes.
     *
     * @param fileName name of the file to inspect
     * @return the identified category
     * @throws IllegalArgumentException if no known suffix is found
     */
    private ImageCategory determineCategory(String fileName) {
        String lower = fileName.toLowerCase();
        if (lower.contains("_ekte")) return ImageCategory.REAL;
        if (lower.contains("_falsk") || lower.contains("_ki")) return ImageCategory.AI_GENERATED;
        if (lower.contains("_manipulert")) return ImageCategory.MANIPULATED;
        throw new IllegalArgumentException(
                "Could not determine ImageCategory for file: " + fileName
                        + " (expected suffix _ekte, _falsk, _ki, or _manipulert before extension)");
    }

    private record DescriptionPair(String correct, String wrong) {}

    // Per-image unique correct/wrong descriptions (Fotografen + Datasenteret image tasks).
    // Keyed by lowercased filename. Falls back to a category-based generic if no entry exists.
    private static final java.util.Map<String, DescriptionPair> IMAGE_DESCRIPTIONS = java.util.Map.ofEntries(
            // Fotografen — easy
            java.util.Map.entry("fotografen_dame_ekte.png", new DescriptionPair(
                    "Riktig! Skygger og hudtekstur i ansiktet er konsistente – dette bildet er ekte.",
                    "Ikke helt. Selv om damen ser litt rar ut, er bildet faktisk ekte.")),
            java.util.Map.entry("fotografen_dame_manipulert.png", new DescriptionPair(
                    "Riktig! Ansiktstrekkene er tydelig redigert – dette bildet er manipulert.",
                    "Ikke helt. Se nøye på ansiktet – det er klart manipulert.")),
            java.util.Map.entry("fotografen_folkemengde_ekte.jpg", new DescriptionPair(
                    "Riktig! Folkemengden ser variert og naturlig ut – bildet er ekte.",
                    "Ikke helt. Selv om scenen er travel, er bildet faktisk ekte.")),
            java.util.Map.entry("fotografen_folkemengde_falsk.png", new DescriptionPair(
                    "Riktig! Folkene smiler unaturlig likt og perspektivet er rart – bildet er KI-generert.",
                    "Ikke helt. Se hvor like ansiktene er – bildet er KI-generert.")),
            java.util.Map.entry("fotografen_jorda_ekte.jpg", new DescriptionPair(
                    "Riktig! Skydekket og fargene er typiske for satellittbilder – bildet er ekte.",
                    "Ikke helt. Bildet er faktisk et ekte satellittbilde av jorda.")),
            java.util.Map.entry("fotografen_jorda_falsk.png", new DescriptionPair(
                    "Riktig! Skarpheten er for perfekt for et satellittbilde – dette er KI-generert.",
                    "Ikke helt. Det er for skarpt og rent til å være et ekte satellittbilde.")),

            // Fotografen — medium
            java.util.Map.entry("fotografen_asiatisk_dame_ekte.jpg", new DescriptionPair(
                    "Riktig! Hudtekstur og lys passer naturlig sammen – bildet er ekte.",
                    "Ikke helt. Bildet er faktisk et ekte portrett.")),
            java.util.Map.entry("fotografen_asiatisk_dame_manipulert.jpg", new DescriptionPair(
                    "Riktig! Hudteksturen passer ikke med resten av ansiktet – bildet er manipulert.",
                    "Ikke helt. Hudteksturen avslører at bildet er manipulert.")),
            java.util.Map.entry("fotografen_blomster_ekte.jpg", new DescriptionPair(
                    "Riktig! Blader og kronblader har naturlige variasjoner – bildet er ekte.",
                    "Ikke helt. Variasjonene i blomstene viser at bildet er ekte.")),
            java.util.Map.entry("fotografen_blomster_falsk.png", new DescriptionPair(
                    "Riktig! Teksturene er for glatte og symmetriske – dette er KI-generert.",
                    "Ikke helt. Den unaturlige glattheten røper at bildet er KI-generert.")),
            java.util.Map.entry("fotografen_naturen_falsk.png", new DescriptionPair(
                    "Riktig! Trærne er duplisert og himmelen virker kunstig – bildet er KI-generert.",
                    "Ikke helt. Dupliserte detaljer viser at bildet er KI-generert.")),

            // Fotografen — hard
            java.util.Map.entry("fotografen_katt_ekte.jpg", new DescriptionPair(
                    "Riktig! Pels og øyne har naturlige uregelmessigheter – bildet er ekte.",
                    "Ikke helt. De små feilene i pelsen viser at bildet er ekte.")),
            java.util.Map.entry("fotografen_katt_falsk.png", new DescriptionPair(
                    "Riktig! Pelsen og øynene er unaturlig perfekte – bildet er KI-generert.",
                    "Ikke helt. Den glatte pelsen avslører at bildet er KI-generert.")),
            java.util.Map.entry("fotografen_naturbilde_ekte.jpg", new DescriptionPair(
                    "Riktig! Lys, dybde og detaljer ser naturlige ut – bildet er ekte.",
                    "Ikke helt. Bildet er faktisk et ekte naturfoto.")),
            java.util.Map.entry("fotografen_naturbilde_falsk.png", new DescriptionPair(
                    "Riktig! Den uvirkelige skarpheten røper at dette er KI-generert.",
                    "Ikke helt. Den unaturlige skarpheten viser at bildet er KI-generert.")),
            java.util.Map.entry("fotografen_ulykke_ekte.jpg", new DescriptionPair(
                    "Riktig! Skygger, røyk og sløring stemmer med en ekte hendelse – bildet er ekte.",
                    "Ikke helt. Detaljene i scenen viser at hendelsen er ekte.")),

            // Datasenteret (chap3) image tasks
            java.util.Map.entry("datasenteret_foto_maleri_ekte.jpg", new DescriptionPair(
                    "Riktig! Penselstrøk og lerretstekstur er autentiske – maleriet er ekte.",
                    "Ikke helt. Penselstrøkene avslører at maleriet er ekte.")),
            java.util.Map.entry("datasenteret_foto_maleri_ki.png", new DescriptionPair(
                    "Riktig! Detaljene er for symmetriske til å være malt for hånd – KI-generert.",
                    "Ikke helt. Den jevne symmetrien røper at maleriet er KI-generert.")),
            java.util.Map.entry("datasenteret_foto_bygning_ekte.jpg", new DescriptionPair(
                    "Riktig! Materialer og perspektiv ser realistiske ut – bygningen er ekte.",
                    "Ikke helt. Detaljene i fasaden viser at bygningen er ekte.")),
            java.util.Map.entry("datasenteret_foto_bygning_ki.png", new DescriptionPair(
                    "Riktig! Vinduer og geometri er for perfekte – bygningen er KI-generert.",
                    "Ikke helt. Den uvirkelig perfekte geometrien røper at bygningen er KI-generert.")),
            java.util.Map.entry("datasenteret_foto_dame_manipulert.png", new DescriptionPair(
                    "Riktig! Ansiktstrekk og bakgrunn er tydelig redigert – bildet er manipulert.",
                    "Ikke helt. Den synlige redigeringen viser at bildet er manipulert.")),
            java.util.Map.entry("datasenteret_foto_mcdonalds_ki.png", new DescriptionPair(
                    "Riktig! Logo og typografi er forvrengt på en typisk KI-måte – KI-generert.",
                    "Ikke helt. Den forvrengte logoen røper at bildet er KI-generert."))
    );

    // Per-news-pair unique correct/wrong descriptions, keyed by the ekte file's name.
    private static final java.util.Map<String, DescriptionPair> NEWS_DESCRIPTIONS = java.util.Map.ofEntries(
            // Nyhetskvartalet (chap1)
            java.util.Map.entry("nyhetsartikkel_ekte_1.png", new DescriptionPair(
                    "Riktig! Den falske forsiden hadde overdrevne overskrifter og en mistenkelig kilde.",
                    "Ikke helt. Den andre forsiden hadde tydelige tegn på å være falsk.")),
            java.util.Map.entry("nyhetsartikkel_ekte_2.png", new DescriptionPair(
                    "Riktig! Den falske forsiden brukte sensasjonelt språk og uklare bilder.",
                    "Ikke helt. Den andre forsiden var den falske – legg merke til språkbruken.")),
            java.util.Map.entry("nyhetsartikkel_ekte_3.png", new DescriptionPair(
                    "Riktig! Datoen og bylinje på den falske forsiden stemte ikke.",
                    "Ikke helt. Den andre forsiden hadde inkonsistente datoer.")),
            java.util.Map.entry("nyhetsartikkel_ekte_4.png", new DescriptionPair(
                    "Riktig! Den falske forsiden manglet kildehenvisning og hadde rare bildevalg.",
                    "Ikke helt. Manglende kilder avslørte den andre forsiden som falsk.")),
            java.util.Map.entry("nyhetsartikkel_ekte_5.png", new DescriptionPair(
                    "Riktig! Logoen var forvrengt og typografien feil på den falske forsiden.",
                    "Ikke helt. Logoen på den andre forsiden var forvrengt.")),
            java.util.Map.entry("nyhetsartikkel_ekte_6.png", new DescriptionPair(
                    "Riktig! Den falske forsiden hadde påfallende skrivefeil og uvanlig layout.",
                    "Ikke helt. Skrivefeilene avslørte den andre forsiden som falsk.")),
            java.util.Map.entry("nyhetsartikkel_ekte_7.png", new DescriptionPair(
                    "Riktig! Den falske forsiden brukte et bilde som ikke passet med saken.",
                    "Ikke helt. Bildevalget på den andre forsiden røpet at den var falsk.")),
            java.util.Map.entry("nyhetsartikkel_ekte_8.png", new DescriptionPair(
                    "Riktig! Den falske forsiden hadde påstander uten kilder.",
                    "Ikke helt. Påstandene på den andre forsiden manglet kildebelegg.")),
            java.util.Map.entry("nyhetsartikkel_ekte_9.png", new DescriptionPair(
                    "Riktig! Layout og fontvalg på den falske forsiden var inkonsistent.",
                    "Ikke helt. Inkonsistent layout avslørte den andre forsiden som falsk.")),

            // Datasenteret (chap3) news
            java.util.Map.entry("datasenteret_nyhet_ekte_1.png", new DescriptionPair(
                    "Riktig! Den falske forsiden hadde sensasjonelle påstander uten kilder.",
                    "Ikke helt. Manglende kilder på den andre forsiden røpet at den var falsk.")),
            java.util.Map.entry("datasenteret_nyhet_ekte_2.png", new DescriptionPair(
                    "Riktig! Datoer og navn på den falske forsiden stemte ikke overens.",
                    "Ikke helt. Inkonsistensene på den andre forsiden viste at den var falsk.")),
            java.util.Map.entry("datasenteret_nyhet_ekte_3.png", new DescriptionPair(
                    "Riktig! Bildet på den falske forsiden var manipulert og passet ikke saken.",
                    "Ikke helt. Det manipulerte bildet avslørte at den andre forsiden var falsk."))
    );

    private String getImageCorrectDescription(String fileName, ImageCategory category) {
        DescriptionPair pair = IMAGE_DESCRIPTIONS.get(fileName.toLowerCase());
        if (pair != null) return pair.correct();
        return "Riktig! " + switch (category) {
            case REAL -> "Dette bildet er ekte.";
            case AI_GENERATED -> "Dette bildet er KI-generert.";
            case MANIPULATED -> "Dette bildet er manipulert.";
        };
    }

    private String getImageFalseDescription(String fileName, ImageCategory category) {
        DescriptionPair pair = IMAGE_DESCRIPTIONS.get(fileName.toLowerCase());
        if (pair != null) return pair.wrong();
        return "Ikke helt. " + switch (category) {
            case REAL -> "Dette bildet er faktisk ekte.";
            case AI_GENERATED -> "Dette bildet er KI-generert.";
            case MANIPULATED -> "Dette bildet er manipulert.";
        };
    }

    private String getNewsCorrectDescription(String ekteFileName) {
        DescriptionPair pair = NEWS_DESCRIPTIONS.get(ekteFileName.toLowerCase());
        return pair != null ? pair.correct() : "Riktig! Avisforsiden du valgte var den falske.";
    }

    private String getNewsFalseDescription(String ekteFileName) {
        DescriptionPair pair = NEWS_DESCRIPTIONS.get(ekteFileName.toLowerCase());
        return pair != null ? pair.wrong() : "Ikke helt. Den andre forsiden var faktisk den falske.";
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
