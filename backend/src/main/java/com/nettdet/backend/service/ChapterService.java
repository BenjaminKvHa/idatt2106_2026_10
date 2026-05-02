package com.nettdet.backend.service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nettdet.backend.domain.Chapter;
import com.nettdet.backend.domain.PupilChapterBridgeProgression;
import com.nettdet.backend.domain.PupilClassroomMembership;
import com.nettdet.backend.domain.Task;
import com.nettdet.backend.domain.enums.DatacenterTaskFormat;
import com.nettdet.backend.domain.enums.TaskDifficulty;
import com.nettdet.backend.domain.ids.PupilChapterBridgeProgressionId;
import com.nettdet.backend.dto.ChapterProgressionResponse;
import com.nettdet.backend.dto.ChapterResponse;
import com.nettdet.backend.dto.DatacenterAnswerRequest;
import com.nettdet.backend.dto.DatacenterRunResponse;
import com.nettdet.backend.dto.DatacenterTask;
import com.nettdet.backend.dto.DatacenterTaskResponse;
import com.nettdet.backend.dto.ImageVerificationRequest;
import com.nettdet.backend.dto.ImageVerificationResponse;
import com.nettdet.backend.dto.ImageVerificationTask;
import com.nettdet.backend.dto.NewsVerificationRequest;
import com.nettdet.backend.dto.NewsVerificationResponse;
import com.nettdet.backend.dto.NewsVerificationTask;
import com.nettdet.backend.dto.PupilMedalResponse;
import com.nettdet.backend.dto.ValidationResult;
import com.nettdet.backend.repository.ChapterRepository;
import com.nettdet.backend.repository.PupilChapterBridgeProgressionRepository;
import com.nettdet.backend.repository.PupilClassroomMembershipRepository;
import com.nettdet.backend.repository.TaskRepository;
import com.nettdet.backend.validation.TaskValidator;

/**
 * Service class responsible for managing chapter progression, task delivery, and validation.
 * 
 * <p>This service orchestrates the core gameplay loop for pupils, including:
 * <ul>
 *   <li>Retrieving eligible tasks based on chapter, difficulty, and classroom context.</li>
 *   <li>Handling specialized "Datasenteret" runs (Chapter 3) with specific task sequences.</li>
 *   <li>Validating pupil answers and updating their progression state.</li>
 *   <li>Tracking highscores, successful runs, and unsuccessful streaks.</li>
 *   <li>Triggering notifications for teachers based on pupil progress or struggles.</li>
 *   <li>Managing notebook tips through the {@link TipService}.</li>
 * </ul>
 * 
 * <p>Task validation is delegated to specific {@link TaskValidator} implementations
 * mapped by chapter type.
 */
@Service
public class ChapterService {

    private static final int DEFAULT_TASKS_PER_RUN = 3;
    private static final long DATACENTER_CHAPTER_ID = 3L;
    private static final int DATACENTER_TASKS_PER_RUN = 5;
    private static final Map<Long, Integer> TASKS_PER_RUN_BY_CHAPTER = Map.of(
        DATACENTER_CHAPTER_ID, DATACENTER_TASKS_PER_RUN);
    private static final List<DatacenterTaskFormat> DATACENTER_RUN_SEQUENCE = List.of(
        DatacenterTaskFormat.NEWS_VERIFICATION,
        DatacenterTaskFormat.NEWS_VERIFICATION,
        DatacenterTaskFormat.IMAGE_VERIFICATION,
        DatacenterTaskFormat.IMAGE_VERIFICATION,
        DatacenterTaskFormat.IMAGE_VERIFICATION);

    private static final Logger logger = LoggerFactory.getLogger(ChapterService.class);

    private final TaskRepository taskRepository;
    private final PupilClassroomMembershipRepository membershipRepository;
    private final PupilChapterBridgeProgressionRepository progressionRepository;
    private final ChapterRepository chapterRepository;
    private final ObjectMapper objectMapper;
    private final Map<String, TaskValidator<?, ?>> validators;
    private final NotificationService notificationService;
    private final TipService tipService;
    private final MedalService medalService;

    public ChapterService(TaskRepository taskRepository,
                          PupilClassroomMembershipRepository membershipRepository,
                          PupilChapterBridgeProgressionRepository progressionRepository,
                          ChapterRepository chapterRepository,
                          ObjectMapper objectMapper,
                          List<TaskValidator<?, ?>> validatorList,
                          NotificationService notificationService,
                          TipService tipService,
                          MedalService medalService) {
        this.taskRepository = taskRepository;
        this.membershipRepository = membershipRepository;
        this.progressionRepository = progressionRepository;
        this.chapterRepository = chapterRepository;
        this.objectMapper = objectMapper;
        this.validators = validatorList.stream()
            .collect(Collectors.toMap(TaskValidator::getChapterType, v -> v));
        this.notificationService = notificationService;
        this.tipService = tipService;
        this.medalService = medalService;
    }

    // -------------------------------------------
    // CHAPTER: FOTOGRAFEN
    // -------------------------------------------

    /**
     * Fetches a random image verification task for a pupil.
     * 
     * <p>If this is the start of a new run (EASY difficulty), any existing abandoned 
     * run state is reset.
     *
     * @param pupilEmail the email of the pupil
     * @param chapterId  the ID of the chapter (typically 1)
     * @param difficulty the difficulty level requested
     * @return an {@link ImageVerificationResponse} containing the task data
     * @throws NoSuchElementException if no eligible tasks are found
     */
    @Transactional
    public ImageVerificationResponse fetchImageVerificationTask(String pupilEmail, Long chapterId, TaskDifficulty difficulty) {
        logger.info("Fetching image verification task for pupil {} chapter {} at difficulty {}",
            pupilEmail, chapterId, difficulty);

        resetRunIfNewRun(pupilEmail, chapterId, difficulty);
        Task task = pickRandomEligibleTask(pupilEmail, chapterId, difficulty);
        ImageVerificationTask stored = readTaskJson(task, ImageVerificationTask.class);

        logger.info("Returning image verification task id {}", task.getId());
        return new ImageVerificationResponse(
            task.getId(),
            stored.taskDescription(),
            stored.fileId(),
            stored.justifications()
        );
    }

    // -------------------------------------------
    // CHAPTER: NYHETSKVARTALET
    // -------------------------------------------

    /**
     * Fetches a random news verification task for a pupil.
     * 
     * <p>If this is the start of a new run (EASY difficulty), any existing abandoned 
     * run state is reset. The order of file IDs in the response is randomized.
     *
     * @param pupilEmail the email of the pupil
     * @param chapterId  the ID of the chapter (typically 2)
     * @param difficulty the difficulty level requested
     * @return a {@link NewsVerificationResponse} containing the task data
     * @throws NoSuchElementException if no eligible tasks are found
     */
    @Transactional
    public NewsVerificationResponse fetchNewsVerificationTask(String pupilEmail, Long chapterId, TaskDifficulty difficulty) {
        logger.info("Fetching news verification task for pupil {} chapter {} at difficulty {}",
            pupilEmail, chapterId, difficulty);

        resetRunIfNewRun(pupilEmail, chapterId, difficulty);
        Task task = pickRandomEligibleTask(pupilEmail, chapterId, difficulty);
        NewsVerificationTask stored = readTaskJson(task, NewsVerificationTask.class);

        logger.info("Returning news verification task id {}", task.getId());
        List<Long> fileIds = new ArrayList<>(List.of(stored.fileId_1(), stored.fileId_2()));
        Collections.shuffle(fileIds);
        List<String> altTexts = fileIds.stream()
            .map(id -> id.equals(stored.fileId_1()) ? stored.altText_1() : stored.altText_2())
            .toList();
        return new NewsVerificationResponse(
            task.getId(),
            stored.taskDescription(),
            fileIds,
            altTexts
        );
    }

    // -------------------------------------------
    // CHAPTER: DATASENTERET (final boss)
    // -------------------------------------------

    /**
     * Fetches a full "Datasenteret" (Chapter 3) run for a pupil.
     * 
     * <p>A Datasenteret run consists of a fixed sequence of 5 tasks (2 news, 3 image).
     * The tasks are picked randomly from the HARD difficulty pool for the pupil's classroom.
     *
     * @param pupilEmail the email of the pupil
     * @param chapterId  the ID of the chapter (must be 3)
     * @return a {@link DatacenterRunResponse} containing the ordered sequence of tasks
     * @throws IllegalArgumentException if chapterId is not the Datasenteret chapter
     * @throws NoSuchElementException   if there are not enough tasks of each format in the pool
     */
    @Transactional
    public DatacenterRunResponse fetchDatacenterRun(String pupilEmail, Long chapterId) {
        logger.info("Fetching Datasenteret run for pupil {} chapter {}", pupilEmail, chapterId);

        if (chapterId != DATACENTER_CHAPTER_ID) {
            throw new IllegalArgumentException(
                "Datasenteret run is only available for chapter " + DATACENTER_CHAPTER_ID);
        }

        resetRunIfStarting(pupilEmail, chapterId);

        String classroomCode = requireConfirmedClassroom(pupilEmail);
        List<Task> pool = taskRepository.findEligibleForPupil(
            chapterId, TaskDifficulty.HARD, classroomCode);

        Map<DatacenterTaskFormat, List<Task>> tasksByFormat = new java.util.EnumMap<>(DatacenterTaskFormat.class);
        for (DatacenterTaskFormat format : DatacenterTaskFormat.values()) {
            tasksByFormat.put(format, new ArrayList<>());
        }
        for (Task task : pool) {
            DatacenterTask wrapper = readTaskJson(task, DatacenterTask.class);
            if (wrapper.format() == null) {
                logger.warn("Datasenteret task id {} has no format discriminator; skipping", task.getId());
                continue;
            }
            tasksByFormat.get(wrapper.format()).add(task);
        }

        List<DatacenterTaskResponse> ordered = new ArrayList<>(DATACENTER_RUN_SEQUENCE.size());
        Map<DatacenterTaskFormat, List<Task>> remaining = new java.util.EnumMap<>(DatacenterTaskFormat.class);
        for (DatacenterTaskFormat format : DatacenterTaskFormat.values()) {
            List<Task> shuffled = new ArrayList<>(tasksByFormat.get(format));
            Collections.shuffle(shuffled);
            remaining.put(format, shuffled);
        }

        for (DatacenterTaskFormat slotFormat : DATACENTER_RUN_SEQUENCE) {
            List<Task> bucket = remaining.get(slotFormat);
            if (bucket.isEmpty()) {
                logger.warn("Not enough Datasenteret tasks for format {} (chapter {} classroom {})",
                    slotFormat, chapterId, classroomCode);
                throw new NoSuchElementException(
                    "Not enough Datasenteret tasks of format " + slotFormat);
            }
            Task picked = bucket.remove(0);
            ordered.add(buildDatacenterTaskResponse(picked));
        }

        logger.info("Returning Datasenteret run with {} tasks for pupil {}", ordered.size(), pupilEmail);
        return new DatacenterRunResponse(ordered);
    }

    /**
     * Validates an answer for a specialized Datasenteret task.
     * 
     * <p>Handles the different task formats within Datasenteret (image and news verification)
     * and dispatches to the appropriate validator. Updates the pupil's run state.
     *
     * @param pupilEmail the email of the pupil
     * @param taskId     the ID of the task being answered
     * @param request    the answer payload
     * @return a {@link ValidationResult} indicating if the answer was correct and providing feedback
     * @throws NoSuchElementException   if the task is not found
     * @throws IllegalArgumentException if the task is not a Datasenteret task or the format is invalid
     * @throws AccessDeniedException    if the task does not belong to the pupil's classroom
     */
    @Transactional
    public ValidationResult validateDatacenterAnswer(String pupilEmail, Long taskId, DatacenterAnswerRequest request) {
        logger.info("Validating Datasenteret answer for pupil {} task {}", pupilEmail, taskId);

        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> {
                logger.warn("Datasenteret validation requested for unknown task id {}", taskId);
                return new NoSuchElementException("Task not found: " + taskId);
            });

        if (task.getChapter() == null || task.getChapter().getId() != DATACENTER_CHAPTER_ID) {
            throw new IllegalArgumentException(
                "Task " + taskId + " is not a Datasenteret task");
        }

        verifyTaskBelongsToPupil(pupilEmail, task);

        DatacenterTask stored = readTaskJson(task, DatacenterTask.class);
        if (stored.format() != request.format()) {
            throw new IllegalArgumentException(
                "Answer format " + request.format() + " does not match task format " + stored.format());
        }

        boolean correct;
        Boolean categoryCorrect = null;
        Boolean justificationCorrect = null;
        String feedback;

        switch (stored.format()) {
            case IMAGE_VERIFICATION -> {
                ImageVerificationTask inner = stored.imageTask();
                ImageVerificationRequest answer = request.imageAnswer();
                if (inner == null || answer == null) {
                    throw new IllegalArgumentException("Missing image-verification payload");
                }
                TaskValidator<ImageVerificationRequest, ImageVerificationTask> v =
                    castValidator("FOTOGRAFEN");
                correct = v.validate(answer, inner);
                feedback = v.getFeedback(inner, correct);
                categoryCorrect = answer.selectedCategory() == inner.correctCategory();
                justificationCorrect = inner.correctJustification().equals(answer.selectedJustification());
            }
            case NEWS_VERIFICATION -> {
                NewsVerificationTask inner = stored.newsTask();
                NewsVerificationRequest answer = request.newsAnswer();
                if (inner == null || answer == null) {
                    throw new IllegalArgumentException("Missing news-verification payload");
                }
                TaskValidator<NewsVerificationRequest, NewsVerificationTask> v =
                    castValidator("NYHETSKVARTALET");
                correct = v.validate(answer, inner);
                feedback = v.getFeedback(inner, correct);
            }
            default -> throw new IllegalArgumentException("Unsupported format: " + stored.format());
        }

        RunStateAfterAttempt runState = recordAttempt(pupilEmail, DATACENTER_CHAPTER_ID, correct);

        logger.info("Datasenteret validation result for task {}: {}", taskId, correct ? "correct" : "incorrect");
        return new ValidationResult(
            correct,
            feedback,
            runState.currentRunAttempts(),
            runState.currentRunCorrect(),
            runState.runCompleted(),
            categoryCorrect,
            justificationCorrect,
            runState.completedChapterMedal(),
            runState.completedChapterMedalNewlyAwarded()
        );
    }

    private DatacenterTaskResponse buildDatacenterTaskResponse(Task task) {
        DatacenterTask stored = readTaskJson(task, DatacenterTask.class);
        return switch (stored.format()) {
            case IMAGE_VERIFICATION -> {
                ImageVerificationTask inner = stored.imageTask();
                yield new DatacenterTaskResponse(
                    task.getId(),
                    DatacenterTaskFormat.IMAGE_VERIFICATION,
                    new ImageVerificationResponse(
                        task.getId(),
                        inner.taskDescription(),
                        inner.fileId(),
                        inner.justifications()
                    ),
                    null
                );
            }
            case NEWS_VERIFICATION -> {
                NewsVerificationTask inner = stored.newsTask();
                List<Long> fileIds = new ArrayList<>(List.of(inner.fileId_1(), inner.fileId_2()));
                Collections.shuffle(fileIds);
                List<String> altTexts = fileIds.stream()
                    .map(id -> id.equals(inner.fileId_1()) ? inner.altText_1() : inner.altText_2())
                    .toList();
                yield new DatacenterTaskResponse(
                    task.getId(),
                    DatacenterTaskFormat.NEWS_VERIFICATION,
                    null,
                    new NewsVerificationResponse(
                        task.getId(),
                        inner.taskDescription(),
                        fileIds,
                        altTexts
                    )
                );
            }
        };
    }

    @SuppressWarnings("unchecked")
    private <R, T> TaskValidator<R, T> castValidator(String chapterType) {
        TaskValidator<?, ?> validator = validators.get(chapterType);
        if (validator == null) {
            throw new IllegalStateException("No validator registered for chapter type: " + chapterType);
        }
        return (TaskValidator<R, T>) validator;
    }

    private void resetRunIfStarting(String pupilEmail, Long chapterId) {
        PupilChapterBridgeProgression progression = progressionRepository
            .findById(new PupilChapterBridgeProgressionId(pupilEmail, chapterId))
            .orElse(null);

        if (progression == null || progression.getCurrentRunAttempts() == 0) {
            return;
        }

        logger.info("Resetting abandoned Datasenteret run for pupil {} chapter {} (was {} attempts, {} correct)",
            pupilEmail, chapterId, progression.getCurrentRunAttempts(), progression.getCurrentRunCorrect());
        progression.setCurrentRunAttempts(0);
        progression.setCurrentRunCorrect(0);
        progressionRepository.save(progression);
    }

    // --------------------------------------------------
    // VALIDATION (generic — dispatches by chapter type)
    // --------------------------------------------------

    /**
     * Validates a pupil's answer for a standard chapter task.
     * 
     * <p>Dispatches validation to the registered {@link TaskValidator} for the given 
     * chapter type. Records the attempt and updates the pupil's progression state.
     *
     * @param pupilEmail  the email of the pupil
     * @param chapterType the type of chapter (e.g., "FOTOGRAFEN", "NYHETSKVARTALET")
     * @param taskId      the ID of the task being answered
     * @param request     the answer payload
     * @return a {@link ValidationResult} containing correctness, feedback, and run status
     * @throws IllegalArgumentException if no validator is registered for the chapter type
     * @throws NoSuchElementException   if the task is not found
     * @throws AccessDeniedException    if the task does not belong to the pupil's classroom
     */
    @Transactional
    public ValidationResult validateAnswer(String pupilEmail, String chapterType, Long taskId, Object request) {
        logger.info("Validating answer for chapter type {} against task id {}", chapterType, taskId);

        TaskValidator<?, ?> validator = validators.get(chapterType);
        if (validator == null) {
            logger.warn("No validator registered for chapter type {}", chapterType);
            throw new IllegalArgumentException("No validator for chapter type: " + chapterType);
        }

        Task task = taskRepository.findById(taskId)
            .orElseThrow(() -> {
                logger.warn("Validation requested for unknown task id {}", taskId);
                return new NoSuchElementException("Task not found: " + taskId);
            });

        verifyTaskBelongsToPupil(pupilEmail, task);

        Object stored = readTaskJson(task, validator.taskType());
        boolean correct = invokeValidator(validator, request, stored);
        String feedback = invokeFeedback(validator, stored, correct);

        Boolean categoryCorrect = null;
        Boolean justificationCorrect = null;
        if (stored instanceof ImageVerificationTask imgTask
                && request instanceof ImageVerificationRequest imgReq) {
            categoryCorrect = imgReq.selectedCategory() == imgTask.correctCategory();
            justificationCorrect = imgTask.correctJustification().equals(imgReq.selectedJustification());
        }

        RunStateAfterAttempt runState = recordAttempt(pupilEmail, task.getChapter().getId(), correct);

        logger.info("Validation result for task id {}: {}", taskId, correct ? "correct" : "incorrect");
        return new ValidationResult(
            correct,
            feedback,
            runState.currentRunAttempts(),
            runState.currentRunCorrect(),
            runState.runCompleted(),
            categoryCorrect,
            justificationCorrect,
            runState.completedChapterMedal(),
            runState.completedChapterMedalNewlyAwarded()
        );
    }

    private record RunStateAfterAttempt(
        int currentRunAttempts,
        int currentRunCorrect,
        boolean runCompleted,
        PupilMedalResponse completedChapterMedal,
        boolean completedChapterMedalNewlyAwarded) {}

    // --------------------------------------------------
    // CHAPTER LISTING
    // --------------------------------------------------

    /**
     * Retrieves all chapters available in the system, ordered by ID.
     * 
     * @return a list of {@link ChapterResponse} objects
     */
    @Transactional(readOnly = true)
    public List<ChapterResponse> getAllChapters() {
        return chapterRepository.findAllByOrderByIdAsc().stream()
            .map(c -> new ChapterResponse(c.getId(), c.getChapterName()))
            .toList();
    }

    // --------------------------------------------------
    // PROGRESSION LOOKUP (for frontend run-state recovery)
    // --------------------------------------------------

    /**
     * Retrieves the current progression state for a pupil in a specific chapter.
     * 
     * <p>Used by the frontend to recover run state (e.g., number of attempts in the 
     * current run) and display highscores.
     *
     * @param pupilEmail the email of the pupil
     * @param chapterId  the ID of the chapter
     * @return a {@link ChapterProgressionResponse} with the pupil's stats
     */
    @Transactional(readOnly = true)
    public ChapterProgressionResponse getProgression(String pupilEmail, Long chapterId) {
        PupilChapterBridgeProgression progression = progressionRepository
            .findById(new PupilChapterBridgeProgressionId(pupilEmail, chapterId))
            .orElse(null);

        if (progression == null) {
            return new ChapterProgressionResponse(0, 0, 0, 0, 0);
        }

        return new ChapterProgressionResponse(
            progression.getCurrentRunAttempts(),
            progression.getCurrentRunCorrect(),
            progression.getSuccessfulRuns(),
            progression.getHighscore(),
            progression.getUnsuccessfulStreak()
        );
    }

    // --------------------------------------------------
    // HELPERS
    // --------------------------------------------------

    private void resetRunIfNewRun(String pupilEmail, Long chapterId, TaskDifficulty difficulty) {
        if (difficulty != TaskDifficulty.EASY) {
            return;
        }

        PupilChapterBridgeProgression progression = progressionRepository
            .findById(new PupilChapterBridgeProgressionId(pupilEmail, chapterId))
            .orElse(null);

        if (progression == null || progression.getCurrentRunAttempts() == 0) {
            return;
        }

        logger.info("Resetting abandoned run for pupil {} chapter {} (was {} attempts, {} correct)",
            pupilEmail, chapterId, progression.getCurrentRunAttempts(), progression.getCurrentRunCorrect());
        progression.setCurrentRunAttempts(0);
        progression.setCurrentRunCorrect(0);
        progressionRepository.save(progression);
    }

    private RunStateAfterAttempt recordAttempt(String pupilEmail, Long chapterId, boolean correct) {
        PupilChapterBridgeProgression progression = getOrCreateProgression(pupilEmail, chapterId);
        int tasksPerRun = tasksPerRun(chapterId);
        int previousSuccessfulRuns = progression.getSuccessfulRuns();
        int previousUnsuccessfulStreak = progression.getUnsuccessfulStreak();

        int attempts = progression.getCurrentRunAttempts() + 1;
        int correctCount = progression.getCurrentRunCorrect() + (correct ? 1 : 0);
        boolean runCompleted = attempts >= tasksPerRun;
        PupilMedalResponse completedChapterMedal = null;
        boolean completedChapterMedalNewlyAwarded = false;

        if (runCompleted) {
            if (correctCount == tasksPerRun) {
                progression.setSuccessfulRuns(progression.getSuccessfulRuns() + 1);
                progression.setUnsuccessfulStreak(0);
                tipService.addAllConfiguredTipsToNotebook(progression);
                completedChapterMedal = medalService.getUnlockedChapterMedal(chapterId);
                completedChapterMedalNewlyAwarded = previousSuccessfulRuns == 0;
                if (previousSuccessfulRuns == 0) {
                    notifyFirstChapterCompletion(progression);
                }
            } else {
                progression.setUnsuccessfulStreak(progression.getUnsuccessfulStreak() + 1);
                if (previousUnsuccessfulStreak == 4 && progression.getUnsuccessfulStreak() == 5) {
                    notifyPupilStuckOnChapter(progression);
                }
            }

            if (correctCount > progression.getHighscore()) {
                progression.setHighscore(correctCount);
            }

            progression.setCurrentRunAttempts(0);
            progression.setCurrentRunCorrect(0);
            logger.info("Run complete for pupil {} chapter {}: {}/{} correct",
                pupilEmail, chapterId, correctCount, tasksPerRun);
        } else {
            progression.setCurrentRunAttempts(attempts);
            progression.setCurrentRunCorrect(correctCount);
        }

        progressionRepository.save(progression);

        return new RunStateAfterAttempt(
            progression.getCurrentRunAttempts(),
            progression.getCurrentRunCorrect(),
            runCompleted,
            completedChapterMedal,
            completedChapterMedalNewlyAwarded
        );
    }

    private int tasksPerRun(Long chapterId) {
        return TASKS_PER_RUN_BY_CHAPTER.getOrDefault(chapterId, DEFAULT_TASKS_PER_RUN);
    }

    private PupilChapterBridgeProgression getOrCreateProgression(String pupilEmail, Long chapterId) {
        PupilChapterBridgeProgressionId id = new PupilChapterBridgeProgressionId(pupilEmail, chapterId);
        return progressionRepository.findById(id).orElseGet(() -> {
            PupilClassroomMembership membership = membershipRepository.findById(pupilEmail)
                .orElseThrow(() -> new NoSuchElementException("Pupil membership not found: " + pupilEmail));
            Chapter chapter = chapterRepository.findById(chapterId)
                .orElseThrow(() -> new NoSuchElementException("Chapter not found: " + chapterId));

            PupilChapterBridgeProgression created = new PupilChapterBridgeProgression();
            created.setId(id);
            created.setPupilClassroomMembership(membership);
            created.setChapter(chapter);
            created.setSuccessfulRuns(0);
            created.setHighscore(0);
            created.setUnsuccessfulStreak(0);
            created.setCurrentRunAttempts(0);
            created.setCurrentRunCorrect(0);
            return created;
        });
    }

    private void verifyTaskBelongsToPupil(String pupilEmail, Task task) {
        if (task.getClassroom() == null) {
            return;
        }

        String pupilClassroom = requireConfirmedClassroom(pupilEmail);
        if (!pupilClassroom.equals(task.getClassroom().getClassroomCode())) {
            logger.warn("Pupil {} attempted to validate task {} from classroom {}",
                pupilEmail, task.getId(), task.getClassroom().getClassroomCode());
            throw new AccessDeniedException("Task does not belong to your classroom");
        }
    }

    private String requireConfirmedClassroom(String pupilEmail) {
        return membershipRepository.findConfirmedClassroomCodeByAccountEmail(pupilEmail)
            .orElseThrow(() -> {
                logger.warn("Pupil {} has no confirmed classroom membership", pupilEmail);
                return new NoSuchElementException("Pupil has no confirmed classroom: " + pupilEmail);
            });
    }

    private Task pickRandomEligibleTask(String pupilEmail, Long chapterId, TaskDifficulty difficulty) {
        String classroomCode = requireConfirmedClassroom(pupilEmail);

        List<Task> pool = taskRepository.findEligibleForPupil(chapterId, difficulty, classroomCode);
        if (pool.isEmpty()) {
            logger.warn("No eligible tasks for chapter {} difficulty {} classroom {}",
                chapterId, difficulty, classroomCode);
            throw new NoSuchElementException(
                "No eligible tasks for chapter " + chapterId + " at difficulty " + difficulty);
        }

        int index = ThreadLocalRandom.current().nextInt(pool.size());
        Task picked = pool.get(index);
        logger.debug("Picked task id {} out of pool size {}", picked.getId(), pool.size());
        return picked;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private boolean invokeValidator(TaskValidator validator, Object request, Object task) {
        return validator.validate(request, task);
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private String invokeFeedback(TaskValidator validator, Object task, boolean correct) {
        return validator.getFeedback(task, correct);
    }

    private <T> T readTaskJson(Task task, Class<T> type) {
        try {
            return objectMapper.readValue(task.getTaskJson(), type);
        } catch (JsonProcessingException e) {
            logger.error("Corrupt task_json for task id {}", task.getId(), e);
            throw new IllegalStateException("Corrupt task_json for task id " + task.getId(), e);
        }
    }

    private void notifyFirstChapterCompletion(PupilChapterBridgeProgression progression) {
        createNotificationIfClassroomContextExists(
            progression,
            "%s har fullført %s for første gang.");
    }

    private void notifyPupilStuckOnChapter(PupilChapterBridgeProgression progression) {
        createNotificationIfClassroomContextExists(
            progression,
            "%s står fast på %s og kan trenge hjelp.");
    }

    private void createNotificationIfClassroomContextExists(PupilChapterBridgeProgression progression,
                                                           String textTemplate) {
        PupilClassroomMembership membership = progression.getPupilClassroomMembership();
        Chapter chapter = progression.getChapter();

        if (membership == null || membership.getClassroom() == null || chapter == null) {
            logger.warn("Skipping chapter notification because progression lacks classroom or chapter context");
            return;
        }

        String legalName = membership.getAccount() != null && membership.getAccount().getLegalName() != null
            ? membership.getAccount().getLegalName()
            : membership.getAccountEmail();
        String text = String.format(textTemplate, legalName, chapter.getChapterName());

        notificationService.createNotification(
            membership.getClassroom().getClassroomCode(),
            membership.getAccountEmail(),
            text);
    }
}
