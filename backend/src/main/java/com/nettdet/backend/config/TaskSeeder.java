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
            String falskName = falskFile.getFileName().toString();
            NewsVerificationTask taskData = new NewsVerificationTask(
                    "To avisforsider ligger foran deg. Én er ekte og én er falsk. Hvilken er falsk?",
                    ekteFileId,
                    falskFileId,
                    falskFileId, // correct = the fake one
                    getNewsCorrectDescription(ekteName),
                    getNewsFalseDescription(ekteName),
                    getNewsAltText(ekteName),
                    getNewsAltText(falskName)
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
            String falskName = falskFile.getFileName().toString();
            NewsVerificationTask inner = new NewsVerificationTask(
                    "To avisforsider ligger foran deg. Én er ekte og én er falsk. Hvilken er falsk?",
                    ekteFileId,
                    falskFileId,
                    falskFileId,
                    getNewsCorrectDescription(ekteName),
                    getNewsFalseDescription(ekteName),
                    getNewsAltText(ekteName),
                    getNewsAltText(falskName)
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
                    "Riktig! Bildet er ekte. Hudteksturen røper det, ekte hud har naturlige porer og småvariasjoner, mens KI-bilder pleier å bli for glatte. Hårvolum er ikke et godt tegn å gå etter, det forekommer både i ekte og oppdiktede bilder.",
                    "Ikke helt, bildet er ekte. Det er hudteksturen som er nøkkelen: ekte hud har porer og små variasjoner som KI ofte glatter ut. Hårvolum sier lite, det kan finnes på begge typer bilder.")),
            java.util.Map.entry("fotografen_dame_manipulert.png", new DescriptionPair(
                    "Riktig! Bildet er manipulert. De ekstra rynkene er lagt på digitalt for å forsterke et inntrykk, manipulerte bilder overdriver gjerne enkelte trekk. Hårvolum er en blindgate, det røper ingenting.",
                    "Ikke helt, bildet er manipulert. Rynkene er nøkkelen: de er digitalt forsterket for å endre uttrykket. Hårvolum forekommer like gjerne i ekte bilder, så det er ikke en pålitelig ledetråd.")),
            java.util.Map.entry("fotografen_folkemengde_ekte.jpg", new DescriptionPair(
                    "Riktig! Bildet er ekte. Sjekk hender og føtter, KI roter ofte med antall fingre, armer og ben, men her stemmer alt. At alle har samme humør sier ingenting, folk deler stemning også på ekte bilder.",
                    "Ikke helt, bildet er ekte. Det avgjørende er at ingen har ekstra armer eller ben, et sted KI gjerne avslører seg. Felles humør er ikke et nyttig tegn.")),
            java.util.Map.entry("fotografen_folkemengde_falsk.png", new DescriptionPair(
                    "Riktig! Bildet er KI-generert. At absolutt alle ser like glade ut er tegnet, KI gir ofte folkemengder uniforme uttrykk i stedet for variert mimikk. Antall mennesker betyr ingenting i seg selv.",
                    "Ikke helt, bildet er KI-generert. Legg merke til hvor likt alle smiler, KI lager folkemengder med uniforme uttrykk i stedet for variert mimikk. Antall personer er ikke en god indikator.")),
            java.util.Map.entry("fotografen_jorda_ekte.jpg", new DescriptionPair(
                    "Riktig! Bildet er ekte. Bakgrunnen ser realistisk ut, ekte satellittbilder viser ikke ekstra stjerner eller solsystem rundt jorda. At jorda 'er flat' er bare tull, jorda er rund.",
                    "Ikke helt, bildet er ekte. Det er bakgrunnen som teller: ingen unaturlige stjerner eller himmelobjekter, slik ekte satellittbilder ser ut. 'Flat jord' er ikke et reelt argument.")),
            java.util.Map.entry("fotografen_jorda_falsk.png", new DescriptionPair(
                    "Riktig! Bildet er KI-generert. KI har lagt til urealistiske stjerner og himmelobjekter, ekte satellittbilder viser bare svart rom rundt jorda. 'Flat jord' er bare en avsporing.",
                    "Ikke helt, bildet er KI-generert. Bakgrunnen avslører det: ekstra stjerner og solsystem som ikke hører hjemme i et ekte satellittbilde. 'Flat jord' har ingenting med saken å gjøre.")),

            // Fotografen — medium
            java.util.Map.entry("fotografen_asiatisk_dame_ekte.jpg", new DescriptionPair(
                    "Riktig! Bildet er ekte. Det avslappede, naturlige uttrykket er typisk for ekte foto, manipulerte bilder pleier å overdrive uttrykk for å skape en effekt. Hun er ikke 'unaturlig glad', det stemmer ikke med bildet.",
                    "Ikke helt, bildet er ekte. Det naturlige uttrykket er nøkkelen, manipulerte bilder forvrenger gjerne mimikken. Påstanden om 'unaturlig glad' passer ikke her.")),
            java.util.Map.entry("fotografen_asiatisk_dame_manipulert.jpg", new DescriptionPair(
                    "Riktig! Bildet er manipulert. Smilet er digitalt forsterket og virker unaturlig stort, typisk for redigerte ansiktsuttrykk. Hun ser ikke 'for sint' ut, så det alternativet er feil.",
                    "Ikke helt, bildet er manipulert. Sjekk smilet, det er forsterket digitalt og ser unaturlig ut. Hun virker ikke sint, så det alternativet stemmer ikke med bildet.")),
            java.util.Map.entry("fotografen_blomster_ekte.jpg", new DescriptionPair(
                    "Riktig! Bildet er ekte. De skarpe, tydelige detaljene på blomstene tyder på et ekte foto, KI mister ofte skarphet i fine teksturer. Kantene er helt naturlige, ingenting rart der.",
                    "Ikke helt, bildet er ekte. Den skarpe gjengivelsen av blomstene er det avgjørende, KI sliter med slike fine detaljer. Kantene ser naturlige ut.")),
            java.util.Map.entry("fotografen_blomster_falsk.png", new DescriptionPair(
                    "Riktig! Bildet er KI-generert. Den slørete kvaliteten er sporet, KI klarer ikke alltid å holde skarpe kanter på fine teksturer som kronblader. Antall blomster er ikke en god indikator.",
                    "Ikke helt, bildet er KI-generert. Det uskarpe preget røper det, KI smører ut detaljer i blomster. Hvor mange blomster det er, sier ingenting.")),
            java.util.Map.entry("fotografen_naturen_falsk.png", new DescriptionPair(
                    "Riktig! Bildet er KI-generert. Den slørete kvaliteten avslører det, KI mister gjerne skarphet i naturlige teksturer som blader og bark. Lyset her er faktisk vanlig, så det er ikke et reelt tegn.",
                    "Ikke helt, bildet er KI-generert. Den uklare gjengivelsen er sporet, KI sliter med skarpe naturlige detaljer. Lyset er normalt og er ikke det som røper bildet.")),
            java.util.Map.entry("fotografen_naturen_falsk_1.png", new DescriptionPair(
                    "Riktig! Bildet er KI-generert. De mange små unaturlige prikkene er klassiske KI-artefakter, digital støy som ekte kameraer ikke produserer på den måten. Bildet er ikke 'for skarpt', det er ikke der problemet ligger.",
                    "Ikke helt, bildet er KI-generert. Se nøye etter prikkene over hele bildet, det er typiske KI-artefakter. 'For skarpt' er ikke et tegn å lete etter.")),

            // Fotografen — hard
            java.util.Map.entry("fotografen_katt_ekte.jpg", new DescriptionPair(
                    "Riktig! Bildet er ekte. De tydelige detaljene i pels og øyne er nøkkelen, KI sliter med fine teksturer som dyrehår. Uskarp bakgrunn er bare normal dybdeskarphet i fotografi, ikke et tegn på noe.",
                    "Ikke helt, bildet er ekte. Detaljene i pels og øyne er sporet, ekte kameraer fanger det skarpere enn KI klarer. Uskarp bakgrunn er helt vanlig i ekte foto.")),
            java.util.Map.entry("fotografen_katt_falsk.png", new DescriptionPair(
                    "Riktig! Bildet er KI-generert. Pelsen ser unaturlig ut, for jevn eller med rare overganger, fordi KI strever med komplekse teksturer som dyrehår. Posisjonen til katten er helt vanlig, det er ikke det som røper bildet.",
                    "Ikke helt, bildet er KI-generert. Pelsen er nøkkelen, KI klarer ikke å gjengi de små variasjonene i ekte hår. Kattens posisjon er helt naturlig og er ikke en god ledetråd.")),
            java.util.Map.entry("fotografen_naturbilde_ekte.jpg", new DescriptionPair(
                    "Riktig! Bildet er ekte. Overgangen mellom himmel og sjø går jevnt og naturlig, KI lager ofte rare horisonter eller fargebånd der. Solen står helt naturlig, så det alternativet er en avsporing.",
                    "Ikke helt, bildet er ekte. Den jevne, naturlige horisonten er nøkkelen, KI roter ofte med overganger mellom himmel og hav. Solens plassering er helt vanlig.")),
            java.util.Map.entry("fotografen_naturbilde_falsk.png", new DescriptionPair(
                    "Riktig! Bildet er KI-generert. Lyset oppfører seg ikke fysisk riktig, skygger og lyskilder peker i feil retning, et klassisk KI-problem. Klart vann er ikke i seg selv et tegn, det forekommer på ekte bilder også.",
                    "Ikke helt, bildet er KI-generert. Følg lyset, KI klarer ikke alltid å plassere skygger riktig i forhold til lyskilden. Klart vann er ikke et godt tegn å gå etter.")),
            java.util.Map.entry("fotografen_ulykke_ekte.jpg", new DescriptionPair(
                    "Riktig! Bildet er ekte. Bilenes plassering på veien er fysisk realistisk, slik ekte ulykker ser ut, KI roter ofte med sammenhengen mellom objekter. Uklarhet alene betyr ingenting, ekte bilder kan også være uklare.",
                    "Ikke helt, bildet er ekte. Sjekk hvordan bilene står, plasseringen er fysisk korrekt slik en ekte ulykke ser ut. Et uklart bilde er ikke i seg selv et bevis.")),
            java.util.Map.entry("fotografen_ulykke_ekte_1.jpg", new DescriptionPair(
                    "Riktig! Bildet er ekte. Skiltene på bilene er klare og lesbare, KI klarer sjelden å lage konsistent tekst som skiltnumre. Kræsjposisjonen er helt naturlig for en ulykke, så det er ikke det som avgjør.",
                    "Ikke helt, bildet er ekte. De tydelige skiltene er nøkkelen, KI lager nesten alltid grøtete eller meningsløs tekst. Kræsjposisjonen ser realistisk ut og er ikke en god ledetråd.")),

            // Datasenteret (chap3) image tasks
            java.util.Map.entry("datasenteret_foto_maleri_ekte.jpg", new DescriptionPair(
                    "Riktig! Bildet er ekte, dette er Mona Lisa. Du ser autentiske penselstrøk og en lerretstekstur som hører til et ekte håndmalt verk. KI-bilder pleier å mangle slike tydelige strøk.",
                    "Ikke helt, bildet er ekte. Dette er Mona Lisa, og synlige penselstrøk og lerretstekstur er det som skiller et ekte maleri fra et KI-bilde.")),
            java.util.Map.entry("datasenteret_foto_maleri_ki.png", new DescriptionPair(
                    "Riktig! Bildet er KI-generert. Detaljene er pikselete og prikkete med ujevn tekstur, og det finnes ingen tydelige penselstrøk slik et ekte maleri ville hatt. Manglende strøk er et klassisk KI-tegn.",
                    "Ikke helt, bildet er KI-generert. Pikselete detaljer og fravær av penselstrøk røper det, et ekte maleri ville hatt synlige strøk og lerretstekstur.")),
            java.util.Map.entry("datasenteret_foto_bygning_ekte.jpg", new DescriptionPair(
                    "Riktig! Bildet er ekte. Lyset reflekteres naturlig, flagget og logoen ser troverdige ut, og himmelen er ikke overdrevent cinematisk slik KI ofte gjør den. Alt henger fysisk sammen.",
                    "Ikke helt, bildet er ekte. Naturlige refleksjoner, troverdig flagg og logo, og en vanlig himmel uten KI-dramatikk er det som avslører et ekte bilde.")),
            java.util.Map.entry("datasenteret_foto_bygning_ki.png", new DescriptionPair(
                    "Riktig! Bildet er KI-generert. Geometrien er unaturlig perfekt, fasaden urealistisk ren, og bygget strekker seg uendelig langs horisonten på en måte ekte arkitektur sjelden gjør.",
                    "Ikke helt, bildet er KI-generert. Den ulastelig perfekte geometrien og den uendelige fasaden er typiske KI-trekk, ekte bygg har slitasje og uregelmessigheter.")),
            java.util.Map.entry("datasenteret_foto_dame_manipulert.png", new DescriptionPair(
                    "Riktig! Bildet er manipulert. Hudteksturen er altfor glatt og unaturlig jevn, et tydelig spor etter digital retusjering av ansiktet. Ekte hud har porer og småvariasjoner.",
                    "Ikke helt, bildet er manipulert. Den unaturlig glatte huden røper digital retusjering, ekte hud har naturlige porer og småvariasjoner.")),
            java.util.Map.entry("datasenteret_foto_mcdonalds_ki.png", new DescriptionPair(
                    "Riktig! Bildet er KI-generert. Ser du nærmere på pomfri-eskene virker de unaturlige, og M-en i bakgrunnen står litt skjevt, klassiske spor etter KI som strever med tekst og logoer.",
                    "Ikke helt, bildet er KI-generert. Pomfri-detaljene er rare og M-en i bakgrunnen er litt skjev, KI sliter ofte med tekst og kjente logoer."))
    );

    // Per-news-pair unique correct/wrong descriptions, keyed by the ekte file's name.
    private static final java.util.Map<String, DescriptionPair> NEWS_DESCRIPTIONS = java.util.Map.ofEntries(
            // Nyhetskvartalet (chap1)
            // Easy
            java.util.Map.entry("nyhetsartikkel_ekte_1.png", new DescriptionPair(
                    "Riktig! Den falske forsiden gjentok samme bilde flere ganger og var full av store bokstaver og utropstegn, typiske tegn på sensasjonsoppslag fremfor seriøs journalistikk.",
                    "Ikke helt. Den andre forsiden brukte samme bilde om igjen og ropte med store bokstaver og utropstegn, det er kjennetegn på falske nyheter.")),
            java.util.Map.entry("nyhetsartikkel_ekte_3.png", new DescriptionPair(
                    "Riktig! Den falske forsiden hadde utropstegn i overskriften, en urealistisk påstand om romvesener, og bildet passet ikke saken.",
                    "Ikke helt. Den andre forsiden påsto at romvesener var funnet, og bildet matchet ikke overskriften, det er klare tegn på falske nyheter.")),
            java.util.Map.entry("nyhetsartikkel_ekte_6.png", new DescriptionPair(
                    "Riktig! Den falske forsiden viste romvesener, som ikke finnes, og enkelte bokstaver (som ø) var forvrengt, typisk for KI-genererte bilder.",
                    "Ikke helt. Den andre forsiden viste romvesener og hadde rare bokstaver som avslørte at bildet var KI-generert.")),

            // Medium
            java.util.Map.entry("nyhetsartikkel_ekte_2.png", new DescriptionPair(
                    "Riktig! På den falske forsiden passet ikke overskriften helt med bildet, noen bokstaver så litt feil ut, og bildet var kornete og av lav kvalitet.",
                    "Ikke helt. Den andre forsiden hadde et bilde som ikke passet overskriften og dårlig bildekvalitet, det avslørte at den var falsk.")),
            java.util.Map.entry("nyhetsartikkel_ekte_7.png", new DescriptionPair(
                    "Riktig! Den falske forsiden meldte om et dødsfall som ingen andre kilder bekrefter, personen lever fortsatt. Sjekk alltid mot flere uavhengige kilder.",
                    "Ikke helt. Den andre forsiden meldte om et dødsfall ingen andre kilder bekreftet, personen er fortsatt i live.")),
            java.util.Map.entry("nyhetsartikkel_ekte_8.png", new DescriptionPair(
                    "Riktig! På den falske forsiden var seismologen anonym, og en tilfeldig logo lå over bildet, kilden mangler en tydelig, navngitt fagperson.",
                    "Ikke helt. Den andre forsiden brukte en anonym ekspert og hadde en tilfeldig logo over bildet, det avslørte at den var falsk.")),

            // Hard
            java.util.Map.entry("nyhetsartikkel_ekte_4.png", new DescriptionPair(
                    "Riktig! Selv om overskriftene så realistiske ut, var URL-en manipulert, 'msn' var skrevet om til 'rnsn' for å se ekte ut. Sjekk alltid nettadressen nøye.",
                    "Ikke helt. Den andre forsiden hadde en forfalsket URL der 'msn' var skrevet som 'rnsn' for å lure leseren.")),
            java.util.Map.entry("nyhetsartikkel_ekte_5.png", new DescriptionPair(
                    "Riktig! Den falske forsiden hadde unaturlige journalistnavn som ikke kan verifiseres. Sjekk alltid om bylinen tilhører ekte, kjente personer.",
                    "Ikke helt. Den andre forsiden brukte oppdiktede journalistnavn, det er et tegn på at kildene ikke kan verifiseres.")),
            java.util.Map.entry("nyhetsartikkel_ekte_9.png", new DescriptionPair(
                    "Riktig! Den falske forsiden hadde unaturlig språk og påstander om UFO-er, typiske tegn på oppdiktet innhold framfor en seriøs sak.",
                    "Ikke helt. Den andre forsiden hadde rare formuleringer og handlet om UFO-er, det avslørte at den var falsk.")),

            // Datasenteret (chap3) news
            java.util.Map.entry("datasenteret_nyhet_ekte_1.png", new DescriptionPair(
                    "Riktig! Den falske artikkelen brukte bilder som tydelig ikke hørte til teksten, og bildeteksten verken navngir personen eller knytter bildet til saken. Sjekk alltid at bildetekst og innhold henger sammen.",
                    "Ikke helt. Den andre artikkelen hadde bilder som ikke passet teksten, og bildeteksten manglet identifikasjon av personen og kobling til saken.")),
            java.util.Map.entry("datasenteret_nyhet_ekte_2.png", new DescriptionPair(
                    "Riktig! Den falske artikkelen brukte bilder som ikke hørte til saken, og bildeteksten manglet både fotografkreditering og en beskrivelse av hva bildet faktisk viser.",
                    "Ikke helt. Den andre artikkelen hadde bilder som ikke passet saken, og bildeteksten manglet fotografnavn og forklaring, det er kjennetegn på falske nyheter.")),
            java.util.Map.entry("datasenteret_nyhet_ekte_3.png", new DescriptionPair(
                    "Riktig! Den falske artikkelen viste et tydelig manipulert bilde og brukte overdrevent mange farger uten grunn. Ekte aviser holder seg til en nøktern, troverdig visuell stil.",
                    "Ikke helt. Den andre artikkelen hadde et manipulert bilde og overdreven fargebruk, klare tegn på at den ikke var seriøs journalistikk.")),
            java.util.Map.entry("datasenteret_nyhet_ekte_4.png", new DescriptionPair(
                    "Riktig! Den falske artikkelen så svært naturlig ut, men bildet virket unaturlig, og under journalistens navn sto 'utrriksjournalist', en feilstaving av 'utenriksjournalist'. Skrivefeil i fagtitler er et avslørende spor.",
                    "Ikke helt. Den andre artikkelen var oppdiktet, og under journalistnavnet var tittelen feilstavet som 'utrriksjournalist' i stedet for 'utenriksjournalist'. Slike feil i profesjonelle titler røper falske artikler."))
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

    // Per-image WCAG alt texts. Keys are lowercased filenames.
    // Each entry describes what is visually present on the page so a screen-reader
    // user gets the same observable cues a sighted pupil would use to make a judgement.
    private static final java.util.Map<String, String> NEWS_ALT_TEXTS = java.util.Map.ofEntries(
            // Nyhetskvartalet (chap1) — easy
            java.util.Map.entry("nyhetsartikkel_ekte_1.png",
                    "Avisforside med en nøktern nyhetsartikkel. Overskriften er skrevet i vanlig størrelse, og artikkelen har ett bilde knyttet til saken."),
            java.util.Map.entry("nyhetsartikkel_falsk_1.png",
                    "Avisforside der overskriften er skrevet med store bokstaver og flere utropstegn. Det samme bildet er gjentatt flere steder på siden."),
            java.util.Map.entry("nyhetsartikkel_ekte_3.png",
                    "Avisforside med en troverdig nyhetssak. Bildet på forsiden passer med overskriften."),
            java.util.Map.entry("nyhetsartikkel_falsk_3.png",
                    "Avisforside med utropstegn i overskriften som hevder at romvesener er funnet. Bildet på forsiden passer ikke med overskriften."),
            java.util.Map.entry("nyhetsartikkel_ekte_6.png",
                    "Avisforside med en vanlig nyhetssak og et tydelig, lesbart bilde."),
            java.util.Map.entry("nyhetsartikkel_falsk_6.png",
                    "Avisforside som viser romvesener, og enkelte bokstaver (blant annet ø) er forvrengt slik KI-genererte bilder ofte gjør."),

            // Nyhetskvartalet (chap1) — medium
            java.util.Map.entry("nyhetsartikkel_ekte_2.png",
                    "Avisforside med skarpt bilde og en overskrift som passer med innholdet i bildet."),
            java.util.Map.entry("nyhetsartikkel_falsk_2.png",
                    "Avisforside der overskriften ikke helt matcher bildet. Bildet er kornete og av lav kvalitet, og noen bokstaver ser feil ut."),
            java.util.Map.entry("nyhetsartikkel_ekte_7.png",
                    "Avisforside med en nyhetssak signert med en navngitt journalist og en troverdig kilde."),
            java.util.Map.entry("nyhetsartikkel_falsk_7.png",
                    "Avisforside som melder om et dødsfall. Saken er ikke bekreftet av andre kilder, og personen er fortsatt i live."),
            java.util.Map.entry("nyhetsartikkel_ekte_8.png",
                    "Avisforside med en navngitt fagperson som kilde og uten påklistret logo."),
            java.util.Map.entry("nyhetsartikkel_falsk_8.png",
                    "Avisforside der eksperten er omtalt som anonym seismolog. En tilfeldig logo er plassert oppå bildet."),

            // Nyhetskvartalet (chap1) — hard
            java.util.Map.entry("nyhetsartikkel_ekte_4.png",
                    "Avisforside fra et kjent nettsted. URL-en øverst på siden er skrevet riktig."),
            java.util.Map.entry("nyhetsartikkel_falsk_4.png",
                    "Avisforside som ser nesten lik en kjent nyhetsside ut, men URL-en øverst er endret slik at 'msn' er skrevet som 'rnsn'."),
            java.util.Map.entry("nyhetsartikkel_ekte_5.png",
                    "Avisforside med byline fra en navngitt journalist som kan verifiseres."),
            java.util.Map.entry("nyhetsartikkel_falsk_5.png",
                    "Avisforside med byline fra journalister hvis navn virker oppdiktet og ikke kan verifiseres."),
            java.util.Map.entry("nyhetsartikkel_ekte_9.png",
                    "Avisforside med vanlig journalistisk språk og en nøktern saksframstilling."),
            java.util.Map.entry("nyhetsartikkel_falsk_9.png",
                    "Avisforside som omtaler UFO-er. Språket er unaturlig og preget av rare formuleringer."),

            // Datasenteret (chap3) news
            java.util.Map.entry("datasenteret_nyhet_ekte_1.png",
                    "Nyhetsartikkel der bildet hører til teksten, og bildeteksten både navngir personen og forklarer koblingen til saken."),
            java.util.Map.entry("datasenteret_nyhet_falsk_1.png",
                    "Nyhetsartikkel der bildet ikke ser ut til å høre til teksten. Bildeteksten verken navngir personen eller knytter bildet til saken."),
            java.util.Map.entry("datasenteret_nyhet_ekte_2.png",
                    "Nyhetsartikkel med bilder som passer saken, og bildetekst med både fotografkreditering og forklaring."),
            java.util.Map.entry("datasenteret_nyhet_falsk_2.png",
                    "Nyhetsartikkel med bilder som ikke passer til saken. Bildeteksten mangler fotografnavn og forklaring av hva bildet viser."),
            java.util.Map.entry("datasenteret_nyhet_ekte_3.png",
                    "Nyhetsartikkel med nøktern visuell stil og et troverdig pressebilde."),
            java.util.Map.entry("datasenteret_nyhet_falsk_3.png",
                    "Nyhetsartikkel med tydelig manipulert bilde og overdreven, fargesterk layout."),
            java.util.Map.entry("datasenteret_nyhet_ekte_4.png",
                    "Nyhetsartikkel med journalistens navn og korrekt tittel 'utenriksjournalist' under bylinen."),
            java.util.Map.entry("datasenteret_nyhet_falsk_4.png",
                    "Nyhetsartikkel som ved første øyekast ser troverdig ut, men under journalistens navn står tittelen feilstavet som 'utrriksjournalist'.")
    );

    private String getNewsAltText(String fileName) {
        String key = fileName.toLowerCase();
        String alt = NEWS_ALT_TEXTS.get(key);
        if (alt != null) return alt;
        return key.contains("_falsk")
                ? "Avisforside som er en falsk nyhetssak."
                : "Avisforside med en nyhetssak.";
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
                        "Detaljene er for pikselerte og ujevne for ett maleri",
                        "Maleriet er signert i feil hjørne")),
                Map.entry("datasenteret_foto_maleri_ki.png", List.of(
                        "Penselstrøk og lerretstekstur ser autentiske ut",
                        "Detaljene er for pikselerte og ujevne for ett maleri",
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
