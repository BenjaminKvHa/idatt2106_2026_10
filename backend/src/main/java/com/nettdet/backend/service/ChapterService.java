package com.nettdet.backend.service;

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
import com.nettdet.backend.domain.enums.TaskDifficulty;
import com.nettdet.backend.domain.ids.PupilChapterBridgeProgressionId;
import com.nettdet.backend.dto.ChapterProgressionResponse;
import com.nettdet.backend.dto.ChapterResponse;
import com.nettdet.backend.dto.ImageVerificationRequest;
import com.nettdet.backend.dto.ImageVerificationResponse;
import com.nettdet.backend.dto.ImageVerificationTask;
import com.nettdet.backend.dto.NewsVerificationResponse;
import com.nettdet.backend.dto.NewsVerificationTask;
import com.nettdet.backend.dto.ValidationResult;
import com.nettdet.backend.repository.ChapterRepository;
import com.nettdet.backend.repository.PupilChapterBridgeProgressionRepository;
import com.nettdet.backend.repository.PupilClassroomMembershipRepository;
import com.nettdet.backend.repository.TaskRepository;
import com.nettdet.backend.validation.TaskValidator;

@Service
public class ChapterService {

    private static final int TASKS_PER_RUN = 3;

    private static final Logger logger = LoggerFactory.getLogger(ChapterService.class);

    private final TaskRepository taskRepository;
    private final PupilClassroomMembershipRepository membershipRepository;
    private final PupilChapterBridgeProgressionRepository progressionRepository;
    private final ChapterRepository chapterRepository;
    private final ObjectMapper objectMapper;
    private final Map<String, TaskValidator<?, ?>> validators;

    public ChapterService(TaskRepository taskRepository,
                          PupilClassroomMembershipRepository membershipRepository,
                          PupilChapterBridgeProgressionRepository progressionRepository,
                          ChapterRepository chapterRepository,
                          ObjectMapper objectMapper,
                          List<TaskValidator<?, ?>> validatorList) {
        this.taskRepository = taskRepository;
        this.membershipRepository = membershipRepository;
        this.progressionRepository = progressionRepository;
        this.chapterRepository = chapterRepository;
        this.objectMapper = objectMapper;
        this.validators = validatorList.stream()
            .collect(Collectors.toMap(TaskValidator::getChapterType, v -> v));
    }

    // -------------------------------------------
    // CHAPTER: FOTOGRAFEN
    // -------------------------------------------

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

    @Transactional
    public NewsVerificationResponse fetchNewsVerificationTask(String pupilEmail, Long chapterId, TaskDifficulty difficulty) {
        logger.info("Fetching news verification task for pupil {} chapter {} at difficulty {}",
            pupilEmail, chapterId, difficulty);

        resetRunIfNewRun(pupilEmail, chapterId, difficulty);
        Task task = pickRandomEligibleTask(pupilEmail, chapterId, difficulty);
        NewsVerificationTask stored = readTaskJson(task, NewsVerificationTask.class);

        logger.info("Returning news verification task id {}", task.getId());
        return new NewsVerificationResponse(
            task.getId(),
            stored.taskDescription(),
            List.of(stored.fileId_1(), stored.fileId_2())
        );
    }

    // --------------------------------------------------
    // VALIDATION (generic — dispatches by chapter type)
    // --------------------------------------------------

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
            justificationCorrect
        );
    }

    private record RunStateAfterAttempt(int currentRunAttempts, int currentRunCorrect, boolean runCompleted) {}

    // --------------------------------------------------
    // CHAPTER LISTING
    // --------------------------------------------------

    @Transactional(readOnly = true)
    public List<ChapterResponse> getAllChapters() {
        return chapterRepository.findAllByOrderByIdAsc().stream()
            .map(c -> new ChapterResponse(c.getId(), c.getChapterName()))
            .toList();
    }

    // --------------------------------------------------
    // PROGRESSION LOOKUP (for frontend run-state recovery)
    // --------------------------------------------------

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

        int attempts = progression.getCurrentRunAttempts() + 1;
        int correctCount = progression.getCurrentRunCorrect() + (correct ? 1 : 0);
        boolean runCompleted = attempts >= TASKS_PER_RUN;

        if (runCompleted) {
            if (correctCount == TASKS_PER_RUN) {
                progression.setSuccessfulRuns(progression.getSuccessfulRuns() + 1);
                progression.setUnsuccessfulStreak(0);
            } else {
                progression.setUnsuccessfulStreak(progression.getUnsuccessfulStreak() + 1);
            }

            if (correctCount > progression.getHighscore()) {
                progression.setHighscore(correctCount);
            }

            progression.setCurrentRunAttempts(0);
            progression.setCurrentRunCorrect(0);
            logger.info("Run complete for pupil {} chapter {}: {}/{} correct",
                pupilEmail, chapterId, correctCount, TASKS_PER_RUN);
        } else {
            progression.setCurrentRunAttempts(attempts);
            progression.setCurrentRunCorrect(correctCount);
        }

        progressionRepository.save(progression);

        return new RunStateAfterAttempt(
            progression.getCurrentRunAttempts(),
            progression.getCurrentRunCorrect(),
            runCompleted
        );
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
}
