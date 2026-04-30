package com.nettdet.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nettdet.backend.domain.Chapter;
import com.nettdet.backend.domain.Classroom;
import com.nettdet.backend.domain.FileObject;
import com.nettdet.backend.domain.Task;
import com.nettdet.backend.domain.enums.ClassroomAccess;
import com.nettdet.backend.domain.enums.CorrectFileSlot;
import com.nettdet.backend.domain.enums.DatacenterTaskFormat;
import com.nettdet.backend.domain.enums.TaskDifficulty;
import com.nettdet.backend.dto.CreateDatacenterImageTaskRequest;
import com.nettdet.backend.dto.CreateDatacenterNewsTaskRequest;
import com.nettdet.backend.dto.CreateImageVerificationTaskRequest;
import com.nettdet.backend.dto.CreateNewsVerificationTaskRequest;
import com.nettdet.backend.dto.DatacenterTask;
import com.nettdet.backend.dto.ImageVerificationTask;
import com.nettdet.backend.dto.NewsVerificationTask;
import com.nettdet.backend.dto.TaskAdminResponse;
import com.nettdet.backend.dto.UpdateImageVerificationTaskRequest;
import com.nettdet.backend.dto.UpdateNewsVerificationTaskRequest;
import com.nettdet.backend.repository.AccountClassroomBridgeTeacherRepository;
import com.nettdet.backend.repository.ChapterRepository;
import com.nettdet.backend.repository.ClassroomRepository;
import com.nettdet.backend.repository.TaskRepository;
import com.nettdet.backend.security.JwtUserPrincipal;

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
    private final ClassroomRepository classroomRepository;
    private final FileObjectService fileObjectService;
    private final FileObjectAccessControlService fileObjectAccessControlService;
    private final AccountClassroomBridgeTeacherRepository teacherBridgeRepository;
    private final ObjectMapper objectMapper;

    public TaskAdminService(TaskRepository taskRepository,
                            ChapterRepository chapterRepository,
                            ClassroomRepository classroomRepository,
                            FileObjectService fileObjectService,
                            FileObjectAccessControlService fileObjectAccessControlService,
                            AccountClassroomBridgeTeacherRepository teacherBridgeRepository,
                            ObjectMapper objectMapper) {
        this.taskRepository = taskRepository;
        this.chapterRepository = chapterRepository;
        this.classroomRepository = classroomRepository;
        this.fileObjectService = fileObjectService;
        this.fileObjectAccessControlService = fileObjectAccessControlService;
        this.teacherBridgeRepository = teacherBridgeRepository;
        this.objectMapper = objectMapper;
    }

    // --------------------------------------------------
    // TASK LISTING (per chapter, scoped to a classroom)
    // --------------------------------------------------

    @Transactional(readOnly = true)
    public List<TaskAdminResponse> listFotografenTasksForClassroom(String teacherEmail, String classroomCode) {
        return listTasksForClassroom(FOTOGRAFEN_CHAPTER_ID, teacherEmail, classroomCode);
    }

    @Transactional(readOnly = true)
    public List<TaskAdminResponse> listNyhetskvartaletTasksForClassroom(String teacherEmail, String classroomCode) {
        return listTasksForClassroom(NYHETSKVARTALET_CHAPTER_ID, teacherEmail, classroomCode);
    }

    @Transactional(readOnly = true)
    public List<TaskAdminResponse> listDatasenteretTasksForClassroom(String teacherEmail, String classroomCode) {
        return listTasksForClassroom(DATACENTER_CHAPTER_ID, teacherEmail, classroomCode);
    }

    private List<TaskAdminResponse> listTasksForClassroom(Long chapterId, String teacherEmail, String classroomCode) {
        if (!teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode(teacherEmail, classroomCode)) {
            throw new AccessDeniedException("Teacher does not belong to classroom: " + classroomCode);
        }
        return taskRepository.findByChapterIdAndClassroomClassroomCode(chapterId, classroomCode)
                .stream()
                .map(TaskAdminResponse::from)
                .toList();
    }

    // --------------------------------------------------
    // TASK CREATION: FOTOGRAFEN
    // --------------------------------------------------

    @Transactional
    public TaskAdminResponse createImageVerificationTask(CreateImageVerificationTaskRequest request,
                                                         MultipartFile file,
                                                         JwtUserPrincipal principal) {
        Chapter chapter = chapterRepository.findById(request.chapterId())
                .orElseThrow(() -> new EntityNotFoundException("Chapter not found: " + request.chapterId()));

        Classroom classroom = resolveClassroomForTeacher(request.classroomCode(), principal);
        Long fileId = storeAndIndex(file, principal);

        ImageVerificationTask taskData = new ImageVerificationTask(
                request.taskDescription(),
                fileId,
                request.correctCategory(),
                request.correctJustification(),
                request.justifications(),
                request.correctDescription(),
                request.falseDescription()
        );

        return TaskAdminResponse.from(createTask(chapter, request.difficulty(), taskData, classroom));
    }

    // --------------------------------------------------
    // TASK CREATION: NYHETSKVARTALET
    // --------------------------------------------------

    @Transactional
    public TaskAdminResponse createNewsVerificationTask(CreateNewsVerificationTaskRequest request,
                                                        MultipartFile file1,
                                                        MultipartFile file2,
                                                        JwtUserPrincipal principal) {
        Chapter chapter = chapterRepository.findById(request.chapterId())
                .orElseThrow(() -> new EntityNotFoundException("Chapter not found: " + request.chapterId()));

        Classroom classroom = resolveClassroomForTeacher(request.classroomCode(), principal);
        Long fileId1 = storeAndIndex(file1, principal);
        Long fileId2 = storeAndIndex(file2, principal);
        Long correctFileId = resolveCorrectFileId(request.correctFileSlot(), fileId1, fileId2);

        NewsVerificationTask taskData = new NewsVerificationTask(
                request.taskDescription(),
                fileId1,
                fileId2,
                correctFileId,
                request.correctDescription(),
                request.incorrectDescription()
        );

        return TaskAdminResponse.from(createTask(chapter, request.difficulty(), taskData, classroom));
    }

    // --------------------------------------------------
    // TASK CREATION: DATASENTERET
    // --------------------------------------------------

    @Transactional
    public TaskAdminResponse createDatacenterImageTask(CreateDatacenterImageTaskRequest request,
                                                       MultipartFile file,
                                                       JwtUserPrincipal principal) {
        Chapter chapter = chapterRepository.findById(DATACENTER_CHAPTER_ID)
                .orElseThrow(() -> new EntityNotFoundException("Datasenteret chapter not found"));

        Long fileId = storeAndIndex(file, principal);

        ImageVerificationTask inner = new ImageVerificationTask(
                request.taskDescription(),
                fileId,
                request.correctCategory(),
                request.correctJustification(),
                request.justifications(),
                request.correctDescription(),
                request.falseDescription()
        );
        DatacenterTask wrapped = new DatacenterTask(
                DatacenterTaskFormat.IMAGE_VERIFICATION, inner, null);

        Classroom classroom = resolveClassroomForTeacher(request.classroomCode(), principal);
        return TaskAdminResponse.from(createTask(chapter, TaskDifficulty.HARD, wrapped, classroom));
    }

    @Transactional
    public TaskAdminResponse createDatacenterNewsTask(CreateDatacenterNewsTaskRequest request,
                                                      MultipartFile file1,
                                                      MultipartFile file2,
                                                      JwtUserPrincipal principal) {
        Chapter chapter = chapterRepository.findById(DATACENTER_CHAPTER_ID)
                .orElseThrow(() -> new EntityNotFoundException("Datasenteret chapter not found"));

        Long fileId1 = storeAndIndex(file1, principal);
        Long fileId2 = storeAndIndex(file2, principal);
        Long correctFileId = resolveCorrectFileId(request.correctFileSlot(), fileId1, fileId2);

        NewsVerificationTask inner = new NewsVerificationTask(
                request.taskDescription(),
                fileId1,
                fileId2,
                correctFileId,
                request.correctDescription(),
                request.incorrectDescription()
        );
        DatacenterTask wrapped = new DatacenterTask(
                DatacenterTaskFormat.NEWS_VERIFICATION, null, inner);

        Classroom classroom = resolveClassroomForTeacher(request.classroomCode(), principal);
        return TaskAdminResponse.from(createTask(chapter, TaskDifficulty.HARD, wrapped, classroom));
    }

    // --------------------------------------------------
    // TASK UPDATE: FOTOGRAFEN / DATASENTERET (image format)
    // --------------------------------------------------

    @Transactional
    public TaskAdminResponse updateImageVerificationTask(Long taskId,
                                                         UpdateImageVerificationTaskRequest request,
                                                         MultipartFile file,
                                                         JwtUserPrincipal principal) {
        Task task = loadTaskForChapter(taskId, FOTOGRAFEN_CHAPTER_ID);
        authorizeTaskMutation(task, principal);

        ImageVerificationTask current = deserialize(task.getTaskJson(), ImageVerificationTask.class);
        Long fileId = replaceFileIfPresent(file, current.fileId(), principal);

        ImageVerificationTask updated = new ImageVerificationTask(
                request.taskDescription(),
                fileId,
                request.correctCategory(),
                request.correctJustification(),
                request.justifications(),
                request.correctDescription(),
                request.falseDescription()
        );

        task.setDifficulty(request.difficulty());
        task.setTaskJson(serialize(updated));
        return TaskAdminResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskAdminResponse updateDatacenterImageTask(Long taskId,
                                                       UpdateImageVerificationTaskRequest request,
                                                       MultipartFile file,
                                                       JwtUserPrincipal principal) {
        Task task = loadTaskForChapter(taskId, DATACENTER_CHAPTER_ID);
        authorizeTaskMutation(task, principal);

        DatacenterTask currentWrapper = deserialize(task.getTaskJson(), DatacenterTask.class);
        if (currentWrapper.format() != DatacenterTaskFormat.IMAGE_VERIFICATION || currentWrapper.imageTask() == null) {
            throw new IllegalArgumentException("Task " + taskId + " is not an image-format Datasenteret task");
        }

        Long fileId = replaceFileIfPresent(file, currentWrapper.imageTask().fileId(), principal);

        ImageVerificationTask updated = new ImageVerificationTask(
                request.taskDescription(),
                fileId,
                request.correctCategory(),
                request.correctJustification(),
                request.justifications(),
                request.correctDescription(),
                request.falseDescription()
        );
        DatacenterTask wrapped = new DatacenterTask(DatacenterTaskFormat.IMAGE_VERIFICATION, updated, null);

        task.setTaskJson(serialize(wrapped));
        return TaskAdminResponse.from(taskRepository.save(task));
    }

    // --------------------------------------------------
    // TASK UPDATE: NYHETSKVARTALET / DATASENTERET (news format)
    // --------------------------------------------------

    @Transactional
    public TaskAdminResponse updateNewsVerificationTask(Long taskId,
                                                        UpdateNewsVerificationTaskRequest request,
                                                        MultipartFile file1,
                                                        MultipartFile file2,
                                                        JwtUserPrincipal principal) {
        Task task = loadTaskForChapter(taskId, NYHETSKVARTALET_CHAPTER_ID);
        authorizeTaskMutation(task, principal);

        NewsVerificationTask current = deserialize(task.getTaskJson(), NewsVerificationTask.class);
        Long fileId1 = replaceFileIfPresent(file1, current.fileId_1(), principal);
        Long fileId2 = replaceFileIfPresent(file2, current.fileId_2(), principal);
        Long correctFileId = resolveCorrectFileId(request.correctFileSlot(), fileId1, fileId2);

        NewsVerificationTask updated = new NewsVerificationTask(
                request.taskDescription(),
                fileId1,
                fileId2,
                correctFileId,
                request.correctDescription(),
                request.incorrectDescription()
        );

        task.setDifficulty(request.difficulty());
        task.setTaskJson(serialize(updated));
        return TaskAdminResponse.from(taskRepository.save(task));
    }

    @Transactional
    public TaskAdminResponse updateDatacenterNewsTask(Long taskId,
                                                      UpdateNewsVerificationTaskRequest request,
                                                      MultipartFile file1,
                                                      MultipartFile file2,
                                                      JwtUserPrincipal principal) {
        Task task = loadTaskForChapter(taskId, DATACENTER_CHAPTER_ID);
        authorizeTaskMutation(task, principal);

        DatacenterTask currentWrapper = deserialize(task.getTaskJson(), DatacenterTask.class);
        if (currentWrapper.format() != DatacenterTaskFormat.NEWS_VERIFICATION || currentWrapper.newsTask() == null) {
            throw new IllegalArgumentException("Task " + taskId + " is not a news-format Datasenteret task");
        }

        NewsVerificationTask current = currentWrapper.newsTask();
        Long fileId1 = replaceFileIfPresent(file1, current.fileId_1(), principal);
        Long fileId2 = replaceFileIfPresent(file2, current.fileId_2(), principal);
        Long correctFileId = resolveCorrectFileId(request.correctFileSlot(), fileId1, fileId2);

        NewsVerificationTask updated = new NewsVerificationTask(
                request.taskDescription(),
                fileId1,
                fileId2,
                correctFileId,
                request.correctDescription(),
                request.incorrectDescription()
        );
        DatacenterTask wrapped = new DatacenterTask(DatacenterTaskFormat.NEWS_VERIFICATION, null, updated);

        task.setTaskJson(serialize(wrapped));
        return TaskAdminResponse.from(taskRepository.save(task));
    }

    // --------------------------------------------------
    // TASK DELETION
    // --------------------------------------------------

    /**
     * Deletes a classroom-scoped task. The teacher must belong to the task's classroom.
     * Global (classroom-less) tasks cannot be deleted via this method.
     * <p>
     * Room for improvement: associated MinIO file objects are intentionally not removed, so
     * orphaned blobs will accumulate until a future cleanup pass is implemented.
     */
    @Transactional
    public void deleteTask(Long taskId, JwtUserPrincipal principal) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found: " + taskId));
        if (task.getClassroom() == null) {
            throw new AccessDeniedException("Global tasks cannot be deleted via this endpoint");
        }
        authorizeTaskMutation(task, principal);
        taskRepository.delete(task);
        logger.info("Deleted task id={} from classroom={}", taskId, task.getClassroom().getClassroomCode());
    }

    // --------------------------------------------------
    // TASK CREATION: GENERIC
    // --------------------------------------------------

    @Transactional
    public Task createTask(Chapter chapter, TaskDifficulty difficulty, Object taskData) {
        return createTask(chapter, difficulty, taskData, null);
    }

    @Transactional
    public Task createTask(Chapter chapter, TaskDifficulty difficulty, Object taskData, Classroom classroom) {
        Task task = new Task();
        task.setChapter(chapter);
        task.setDifficulty(difficulty);
        task.setClassroom(classroom);
        task.setTaskJson(serialize(taskData));

        Task saved = taskRepository.save(task);
        logger.info("Created task id={} for chapter id={} with difficulty={}",
                saved.getId(), chapter.getId(), difficulty);
        return saved;
    }

    // --------------------------------------------------
    // HELPERS
    // --------------------------------------------------

    private Classroom resolveClassroomForTeacher(String classroomCode, JwtUserPrincipal principal) {
        if (classroomCode == null || classroomCode.isBlank()) {
            throw new IllegalArgumentException("classroomCode is required");
        }
        if (!teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode(principal.email(), classroomCode)) {
            throw new AccessDeniedException("Teacher does not belong to classroom: " + classroomCode);
        }
        return classroomRepository.findById(classroomCode)
                .orElseThrow(() -> new EntityNotFoundException("Classroom not found: " + classroomCode));
    }

    private Long storeAndIndex(MultipartFile file, JwtUserPrincipal principal) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        FileObject stored = fileObjectAccessControlService.upload(file, null, ClassroomAccess.PUBLIC, principal);
        return stored.getId();
    }

    private Long replaceFileIfPresent(MultipartFile file, Long currentFileId, JwtUserPrincipal principal) {
        if (file == null || file.isEmpty()) {
            return currentFileId;
        }
        Long newId = storeAndIndex(file, principal);
        if (currentFileId != null) {
            try {
                fileObjectService.delete(currentFileId);
            } catch (RuntimeException e) {
                logger.warn("Failed to delete previous file id={}: {}", currentFileId, e.getMessage());
            }
        }
        return newId;
    }

    private Long resolveCorrectFileId(CorrectFileSlot slot, Long fileId1, Long fileId2) {
        if (slot == null) {
            throw new IllegalArgumentException("correctFileSlot is required");
        }
        return slot == CorrectFileSlot.FILE_1 ? fileId1 : fileId2;
    }

    private Task loadTaskForChapter(Long taskId, Long expectedChapterId) {
        Task task = taskRepository.findById(taskId)
                .orElseThrow(() -> new EntityNotFoundException("Task not found: " + taskId));
        if (task.getChapter() == null || !expectedChapterId.equals(task.getChapter().getId())) {
            throw new IllegalArgumentException(
                    "Task " + taskId + " does not belong to chapter " + expectedChapterId);
        }
        return task;
    }

    private void authorizeTaskMutation(Task task, JwtUserPrincipal principal) {
        if (task.getClassroom() == null) {
            return;
        }
        String code = task.getClassroom().getClassroomCode();
        if (!teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode(principal.email(), code)) {
            throw new AccessDeniedException("Teacher does not belong to classroom: " + code);
        }
    }

    private String serialize(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("Failed to serialize task data", e);
        }
    }

    private <T> T deserialize(String json, Class<T> type) {
        try {
            return objectMapper.readValue(json, type);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to deserialize stored task_json", e);
        }
    }
}
