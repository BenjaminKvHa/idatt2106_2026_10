package com.nettdet.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nettdet.backend.domain.Chapter;
import com.nettdet.backend.domain.FileObject;
import com.nettdet.backend.domain.Task;
import com.nettdet.backend.domain.enums.ClassroomAccess;
import com.nettdet.backend.domain.enums.DatacenterTaskFormat;
import com.nettdet.backend.domain.enums.TaskDifficulty;
import com.nettdet.backend.dto.CreateDatacenterImageTaskRequest;
import com.nettdet.backend.dto.CreateDatacenterNewsTaskRequest;
import com.nettdet.backend.dto.CreateImageVerificationTaskRequest;
import com.nettdet.backend.dto.CreateNewsVerificationTaskRequest;
import com.nettdet.backend.dto.DatacenterTask;
import com.nettdet.backend.dto.ImageVerificationTask;
import com.nettdet.backend.dto.NewsVerificationTask;
import com.nettdet.backend.dto.FileReferenceResponse;
import com.nettdet.backend.repository.AccountClassroomBridgeTeacherRepository;
import com.nettdet.backend.repository.ChapterRepository;
import com.nettdet.backend.repository.FileObjectRepository;
import com.nettdet.backend.repository.TaskRepository;

import jakarta.persistence.EntityNotFoundException;
import org.springframework.security.access.AccessDeniedException;
import java.util.List;

@Service
public class TaskAdminService {

    private static final Logger logger = LoggerFactory.getLogger(TaskAdminService.class);
    private static final Long NYHETSKVARTALET_CHAPTER_ID = 1L;
    private static final Long FOTOGRAFEN_CHAPTER_ID = 2L;
    private static final Long DATACENTER_CHAPTER_ID = 3L;

    private final TaskRepository taskRepository;
    private final ChapterRepository chapterRepository;
    private final FileObjectRepository fileObjectRepository;
    private final FileObjectService fileObjectService;
    private final AccountClassroomBridgeTeacherRepository teacherBridgeRepository;
    private final ObjectMapper objectMapper;

    public TaskAdminService(TaskRepository taskRepository,
                            ChapterRepository chapterRepository,
                            FileObjectRepository fileObjectRepository,
                            FileObjectService fileObjectService,
                            AccountClassroomBridgeTeacherRepository teacherBridgeRepository,
                            ObjectMapper objectMapper) {
        this.taskRepository = taskRepository;
        this.chapterRepository = chapterRepository;
        this.fileObjectRepository = fileObjectRepository;
        this.fileObjectService = fileObjectService;
        this.teacherBridgeRepository = teacherBridgeRepository;
        this.objectMapper = objectMapper;
    }

    // --------------------------------------------------
    // TASK LISTING (per chapter, scoped to a classroom)
    // --------------------------------------------------

    public List<Task> listFotografenTasksForClassroom(String teacherEmail, String classroomCode) {
        return listTasksForClassroom(FOTOGRAFEN_CHAPTER_ID, teacherEmail, classroomCode);
    }

    public List<Task> listNyhetskvartaletTasksForClassroom(String teacherEmail, String classroomCode) {
        return listTasksForClassroom(NYHETSKVARTALET_CHAPTER_ID, teacherEmail, classroomCode);
    }

    public List<Task> listDatasenteretTasksForClassroom(String teacherEmail, String classroomCode) {
        return listTasksForClassroom(DATACENTER_CHAPTER_ID, teacherEmail, classroomCode);
    }

    private List<Task> listTasksForClassroom(Long chapterId, String teacherEmail, String classroomCode) {
        if (!teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode(teacherEmail, classroomCode)) {
            throw new AccessDeniedException("Teacher does not belong to classroom: " + classroomCode);
        }
        return taskRepository.findByChapterIdAndClassroomClassroomCode(chapterId, classroomCode);
    }

    // --------------------------------------------------
    // FILE REFERENCE FROM MINIO
    // --------------------------------------------------

    /**
     * Gets or creates a file object reference from MinIO by object key.
     * This allows referencing files already stored in MinIO without needing
     * to know their database IDs beforehand.
     *
     * @param objectKey the S3/MinIO object key (e.g., "task-assets/fotografen/easy/task1/image.png")
     * @param contentType the MIME type of the file
     * @param fileName the original file name
     * @return response containing the file ID and creation status
     */
    @Transactional
    public FileReferenceResponse getOrCreateFileReference(String objectKey, String contentType, String fileName) {
        FileObjectService.FileObjectResult result = fileObjectService.getOrCreateFileObject(
                objectKey,
                contentType,
                null, // uploadedBy - system reference
                null, // classroom - global task asset
                ClassroomAccess.PUBLIC,
                fileName);

        FileObject fileObject = result.fileObject();
        logger.info("File reference for {}: id={}, newlyCreated={}",
                objectKey, fileObject.getId(), result.newlyCreated());

        return new FileReferenceResponse(fileObject.getId(), objectKey, result.newlyCreated());
    }

    // --------------------------------------------------
    // TASK CREATION: FOTOGRAFEN
    // --------------------------------------------------

    /**
     * Creates a new Fotografen (Image Verification) task.
     *
     * @param request the task creation request containing chapter_id, difficulty, and task data
     * @return the created task with its generated ID
     */
    @Transactional
    public Task createImageVerificationTask(CreateImageVerificationTaskRequest request) {
        Chapter chapter = chapterRepository.findById(request.chapterId())
                .orElseThrow(() -> new EntityNotFoundException("Chapter not found: " + request.chapterId()));

        validateFileExists(request.fileId());

        ImageVerificationTask taskData = new ImageVerificationTask(
                request.taskDescription(),
                request.fileId(),
                request.correctCategory(),
                request.correctJustification(),
                request.justifications(),
                request.correctDescription(),
                request.falseDescription()
        );

        return createTask(chapter, request.difficulty(), taskData);
    }

    // --------------------------------------------------
    // TASK CREATION: NYHETSKVARTALET
    // --------------------------------------------------

    /**
     * Creates a new Nyhetskvartalet (News Verification) task.
     *
     * @param request the task creation request containing chapter_id, difficulty, and task data
     * @return the created task with its generated ID
     */
    @Transactional
    public Task createNewsVerificationTask(CreateNewsVerificationTaskRequest request) {
        Chapter chapter = chapterRepository.findById(request.chapterId())
                .orElseThrow(() -> new EntityNotFoundException("Chapter not found: " + request.chapterId()));

        validateFileExists(request.fileId_1());
        validateFileExists(request.fileId_2());

        if (!request.correctFileId().equals(request.fileId_1()) && !request.correctFileId().equals(request.fileId_2())) {
            throw new IllegalArgumentException("correctFileId must match either fileId_1 or fileId_2");
        }

        NewsVerificationTask taskData = new NewsVerificationTask(
                request.taskDescription(),
                request.fileId_1(),
                request.fileId_2(),
                request.correctFileId(),
                request.correctDescription(),
                request.incorrectDescription()
        );

        return createTask(chapter, request.difficulty(), taskData);
    }

    // --------------------------------------------------
    // TASK CREATION: DATASENTERET (final boss)
    // --------------------------------------------------

    /**
     * Creates a Datasenteret task using the image-verification format.
     * Always associated with the Datasenteret chapter at HARD difficulty.
     */
    @Transactional
    public Task createDatacenterImageTask(CreateDatacenterImageTaskRequest request) {
        Chapter chapter = chapterRepository.findById(DATACENTER_CHAPTER_ID)
                .orElseThrow(() -> new EntityNotFoundException("Datasenteret chapter not found"));

        validateFileExists(request.fileId());

        ImageVerificationTask inner = new ImageVerificationTask(
                request.taskDescription(),
                request.fileId(),
                request.correctCategory(),
                request.correctJustification(),
                request.justifications(),
                request.correctDescription(),
                request.falseDescription()
        );
        DatacenterTask wrapped = new DatacenterTask(
                DatacenterTaskFormat.IMAGE_VERIFICATION, inner, null);

        return createTask(chapter, TaskDifficulty.HARD, wrapped);
    }

    /**
     * Creates a Datasenteret task using the news-verification format.
     * Always associated with the Datasenteret chapter at HARD difficulty.
     */
    @Transactional
    public Task createDatacenterNewsTask(CreateDatacenterNewsTaskRequest request) {
        Chapter chapter = chapterRepository.findById(DATACENTER_CHAPTER_ID)
                .orElseThrow(() -> new EntityNotFoundException("Datasenteret chapter not found"));

        validateFileExists(request.fileId_1());
        validateFileExists(request.fileId_2());

        if (!request.correctFileId().equals(request.fileId_1())
                && !request.correctFileId().equals(request.fileId_2())) {
            throw new IllegalArgumentException("correctFileId must match either fileId_1 or fileId_2");
        }

        NewsVerificationTask inner = new NewsVerificationTask(
                request.taskDescription(),
                request.fileId_1(),
                request.fileId_2(),
                request.correctFileId(),
                request.correctDescription(),
                request.incorrectDescription()
        );
        DatacenterTask wrapped = new DatacenterTask(
                DatacenterTaskFormat.NEWS_VERIFICATION, null, inner);

        return createTask(chapter, TaskDifficulty.HARD, wrapped);
    }

    // --------------------------------------------------
    // TASK CREATION: GENERIC
    // --------------------------------------------------

    /**
     * Creates a task with the given chapter, difficulty, and task data.
     * The task data object will be serialized to JSON and stored in task_json column.
     *
     * @param chapter the chapter to associate the task with
     * @param difficulty the difficulty level
     * @param taskData the task data object (ImageVerificationTask, NewsVerificationTask, etc.)
     * @return the created task
     */
    @Transactional
    public Task createTask(Chapter chapter, TaskDifficulty difficulty, Object taskData) {
        String taskJson;
        try {
            taskJson = objectMapper.writeValueAsString(taskData);
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize task data for chapter {}: {}", chapter.getId(), e.getMessage());
            throw new IllegalArgumentException("Failed to serialize task data", e);
        }

        Task task = new Task();
        task.setChapter(chapter);
        task.setDifficulty(difficulty);
        task.setClassroom(null); // Global task (not classroom-specific)
        task.setTaskJson(taskJson);

        Task saved = taskRepository.save(task);
        logger.info("Created task id={} for chapter id={} with difficulty={}",
                saved.getId(), chapter.getId(), difficulty);

        return saved;
    }

    // --------------------------------------------------
    // HELPERS
    // --------------------------------------------------

    private void validateFileExists(Long fileId) {
        if (!fileObjectRepository.existsById(fileId)) {
            throw new EntityNotFoundException("File object not found: " + fileId);
        }
    }
}
