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

/**
 * Administrative service backing the teacher-only {@code /admin/tasks} endpoints.
 * <p>
 * Tasks live in a single table, with their per-chapter payload stored as JSON in the
 * {@code task_json} column. This service is responsible for:
 * <ul>
 *     <li><b>Listing</b> classroom-scoped tasks per chapter (Fotografen, Nyhetskvartalet,
 *         Datasenteret).</li>
 *     <li><b>Creating</b> tasks for each chapter, uploading any attached image assets through
 *         {@link FileObjectAccessControlService} and serialising the appropriate task DTO into
 *         the {@code task_json} column.</li>
 *     <li><b>Updating</b> existing tasks, optionally swapping in new image assets (the previous
 *         file is deleted from MinIO best-effort; failures are logged but do not abort the
 *         update).</li>
 *     <li><b>Deleting</b> classroom-scoped tasks. Note: the underlying file objects are
 *         intentionally not removed — see {@link #deleteTask(Long, JwtUserPrincipal)}.</li>
 * </ul>
 * Authorisation is enforced in this layer in addition to {@code @PreAuthorize} on the
 * controller: every mutating call checks that the requesting teacher is linked to the task's
 * classroom via {@link AccountClassroomBridgeTeacherRepository}. Datasenteret tasks always use
 * {@link TaskDifficulty#HARD} regardless of caller input — difficulty for that chapter is fixed
 * by design.
 * <p>
 * Chapter ids are pinned in code rather than looked up by name to keep seeding and runtime
 * routing predictable: 1 = Nyhetskvartalet, 2 = Fotografen, 3 = Datasenteret.
 */
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

    /**
     * Constructs a {@code TaskAdminService} with the repositories and collaborators required
     * to manage tasks end-to-end (including S3 uploads and authorisation checks).
     *
     * @param taskRepository                  repository for the {@code task} table
     * @param chapterRepository               repository for the chapter catalogue
     * @param classroomRepository             repository used to resolve target classrooms
     * @param fileObjectService               low-level service used to delete superseded asset rows
     * @param fileObjectAccessControlService  service used to upload new asset files to S3
     * @param teacherBridgeRepository         repository used to verify teacher↔classroom ownership
     * @param objectMapper                    Jackson mapper used to (de)serialise {@code task_json}
     */
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

    /**
     * Lists Fotografen (image-verification) tasks scoped to the given classroom.
     *
     * @param teacherEmail  the requesting teacher's email
     * @param classroomCode the classroom whose tasks are returned
     * @return the classroom's Fotografen tasks projected as admin DTOs
     * @throws AccessDeniedException if the teacher is not linked to the classroom
     */
    @Transactional(readOnly = true)
    public List<TaskAdminResponse> listFotografenTasksForClassroom(String teacherEmail, String classroomCode) {
        return listTasksForClassroom(FOTOGRAFEN_CHAPTER_ID, teacherEmail, classroomCode);
    }

    /**
     * Lists Nyhetskvartalet (news-verification) tasks scoped to the given classroom.
     *
     * @param teacherEmail  the requesting teacher's email
     * @param classroomCode the classroom whose tasks are returned
     * @return the classroom's Nyhetskvartalet tasks projected as admin DTOs
     * @throws AccessDeniedException if the teacher is not linked to the classroom
     */
    @Transactional(readOnly = true)
    public List<TaskAdminResponse> listNyhetskvartaletTasksForClassroom(String teacherEmail, String classroomCode) {
        return listTasksForClassroom(NYHETSKVARTALET_CHAPTER_ID, teacherEmail, classroomCode);
    }

    /**
     * Lists Datasenteret (mixed-format) tasks scoped to the given classroom.
     *
     * @param teacherEmail  the requesting teacher's email
     * @param classroomCode the classroom whose tasks are returned
     * @return the classroom's Datasenteret tasks projected as admin DTOs
     * @throws AccessDeniedException if the teacher is not linked to the classroom
     */
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

    /**
     * Creates a Fotografen image-verification task for the requesting teacher's classroom.
     * <p>
     * Uploads {@code file} to S3, builds the {@link ImageVerificationTask} payload, and stores
     * the result with the requested difficulty.
     *
     * @param request    task metadata, including target chapter, classroom, and answer keys
     * @param file       the image asset for the task; must not be empty
     * @param principal  the authenticated teacher
     * @return the created task projected as a {@link TaskAdminResponse}
     * @throws EntityNotFoundException  if the chapter or classroom is not found
     * @throws AccessDeniedException    if the teacher is not linked to the target classroom
     * @throws IllegalArgumentException if the file is empty
     */
    @Transactional
    public TaskAdminResponse createImageVerificationTask(CreateImageVerificationTaskRequest request,
                                                         MultipartFile file,
                                                         JwtUserPrincipal principal) {
        Chapter chapter = chapterRepository.findById(request.chapterId())
                .orElseThrow(() -> new EntityNotFoundException("Chapter not found: " + request.chapterId()));

        Classroom classroom = resolveClassroomForTeacher(request.classroomCode(), principal);
        Long fileId = storeAndIndexInClassroom(file, principal, classroom.getClassroomCode());

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

    /**
     * Creates a Nyhetskvartalet news-verification task for the requesting teacher's classroom.
     * <p>
     * Uploads both files, resolves which file id is the correct answer based on
     * {@link CorrectFileSlot}, and stores the {@link NewsVerificationTask} payload.
     *
     * @param request    task metadata including the correct-file slot
     * @param file1      first news asset; must not be empty
     * @param file2      second news asset; must not be empty
     * @param principal  the authenticated teacher
     * @return the created task projected as a {@link TaskAdminResponse}
     * @throws EntityNotFoundException  if the chapter or classroom is not found
     * @throws AccessDeniedException    if the teacher is not linked to the target classroom
     * @throws IllegalArgumentException if either file is empty or {@code correctFileSlot} is null
     */
    @Transactional
    public TaskAdminResponse createNewsVerificationTask(CreateNewsVerificationTaskRequest request,
                                                        MultipartFile file1,
                                                        MultipartFile file2,
                                                        JwtUserPrincipal principal) {
        Chapter chapter = chapterRepository.findById(request.chapterId())
                .orElseThrow(() -> new EntityNotFoundException("Chapter not found: " + request.chapterId()));

        Classroom classroom = resolveClassroomForTeacher(request.classroomCode(), principal);
        Long fileId1 = storeAndIndexInClassroom(file1, principal, classroom.getClassroomCode());
        Long fileId2 = storeAndIndexInClassroom(file2, principal, classroom.getClassroomCode());
        Long correctFileId = resolveCorrectFileId(request.correctFileSlot(), fileId1, fileId2);

        NewsVerificationTask taskData = new NewsVerificationTask(
                request.taskDescription(),
                fileId1,
                fileId2,
                correctFileId,
                request.correctDescription(),
                request.incorrectDescription(),
                request.altText1(),
                request.altText2()
        );

        return TaskAdminResponse.from(createTask(chapter, request.difficulty(), taskData, classroom));
    }

    // --------------------------------------------------
    // TASK CREATION: DATASENTERET
    // --------------------------------------------------

    /**
     * Creates an image-format Datasenteret task for the requesting teacher's classroom.
     * <p>
     * The inner {@link ImageVerificationTask} is wrapped in a {@link DatacenterTask} envelope
     * tagged with {@link DatacenterTaskFormat#IMAGE_VERIFICATION}, and difficulty is forced to
     * {@link TaskDifficulty#HARD}.
     *
     * @param request    task metadata
     * @param file       the image asset; must not be empty
     * @param principal  the authenticated teacher
     * @return the created task projected as a {@link TaskAdminResponse}
     * @throws EntityNotFoundException  if the Datasenteret chapter or classroom is not found
     * @throws AccessDeniedException    if the teacher is not linked to the target classroom
     * @throws IllegalArgumentException if the file is empty
     */
    @Transactional
    public TaskAdminResponse createDatacenterImageTask(CreateDatacenterImageTaskRequest request,
                                                       MultipartFile file,
                                                       JwtUserPrincipal principal) {
        Chapter chapter = chapterRepository.findById(DATACENTER_CHAPTER_ID)
                .orElseThrow(() -> new EntityNotFoundException("Datasenteret chapter not found"));
        Classroom classroom = resolveClassroomForTeacher(request.classroomCode(), principal);
        Long fileId = storeAndIndexInClassroom(file, principal, classroom.getClassroomCode());

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

        return TaskAdminResponse.from(createTask(chapter, TaskDifficulty.HARD, wrapped, classroom));
    }

    /**
     * Creates a news-format Datasenteret task for the requesting teacher's classroom.
     * <p>
     * The inner {@link NewsVerificationTask} is wrapped in a {@link DatacenterTask} envelope
     * tagged with {@link DatacenterTaskFormat#NEWS_VERIFICATION}, and difficulty is forced to
     * {@link TaskDifficulty#HARD}.
     *
     * @param request    task metadata including the correct-file slot
     * @param file1      first news asset; must not be empty
     * @param file2      second news asset; must not be empty
     * @param principal  the authenticated teacher
     * @return the created task projected as a {@link TaskAdminResponse}
     * @throws EntityNotFoundException  if the Datasenteret chapter or classroom is not found
     * @throws AccessDeniedException    if the teacher is not linked to the target classroom
     * @throws IllegalArgumentException if either file is empty or {@code correctFileSlot} is null
     */
    @Transactional
    public TaskAdminResponse createDatacenterNewsTask(CreateDatacenterNewsTaskRequest request,
                                                      MultipartFile file1,
                                                      MultipartFile file2,
                                                      JwtUserPrincipal principal) {
        Chapter chapter = chapterRepository.findById(DATACENTER_CHAPTER_ID)
                .orElseThrow(() -> new EntityNotFoundException("Datasenteret chapter not found"));
        Classroom classroom = resolveClassroomForTeacher(request.classroomCode(), principal);

        Long fileId1 = storeAndIndexInClassroom(file1, principal, classroom.getClassroomCode());
        Long fileId2 = storeAndIndexInClassroom(file2, principal, classroom.getClassroomCode());
        Long correctFileId = resolveCorrectFileId(request.correctFileSlot(), fileId1, fileId2);

        NewsVerificationTask inner = new NewsVerificationTask(
                request.taskDescription(),
                fileId1,
                fileId2,
                correctFileId,
                request.correctDescription(),
                request.incorrectDescription(),
                request.altText1(),
                request.altText2()
        );
        DatacenterTask wrapped = new DatacenterTask(
                DatacenterTaskFormat.NEWS_VERIFICATION, null, inner);

        return TaskAdminResponse.from(createTask(chapter, TaskDifficulty.HARD, wrapped, classroom));
    }

    // --------------------------------------------------
    // TASK UPDATE: FOTOGRAFEN / DATASENTERET (image format)
    // --------------------------------------------------

    /**
     * Updates a Fotografen image-verification task in place.
     * <p>
     * If {@code file} is non-empty the existing asset is replaced (the old MinIO file is
     * deleted on best effort; failures are logged). Otherwise the previous file id is kept
     * verbatim and only the JSON metadata + difficulty are rewritten.
     *
     * @param taskId     the task to update
     * @param request    new metadata including difficulty
     * @param file       optional replacement image; {@code null} or empty keeps the existing one
     * @param principal  the authenticated teacher
     * @return the persisted task projected as a {@link TaskAdminResponse}
     * @throws EntityNotFoundException  if the task does not exist
     * @throws IllegalArgumentException if the task does not belong to the Fotografen chapter
     * @throws AccessDeniedException    if the teacher is not linked to the task's classroom
     */
    @Transactional
    public TaskAdminResponse updateImageVerificationTask(Long taskId,
                                                         UpdateImageVerificationTaskRequest request,
                                                         MultipartFile file,
                                                         JwtUserPrincipal principal) {
        Task task = loadTaskForChapter(taskId, FOTOGRAFEN_CHAPTER_ID);
        authorizeTaskMutation(task, principal);

        ImageVerificationTask current = deserialize(task.getTaskJson(), ImageVerificationTask.class);
        Classroom classroom = resolveClassroomForTeacher(task.getClassroom().getClassroomCode(), principal);
        Long fileId = replaceFileIfPresent(file, current.fileId(), principal, classroom.getClassroomCode());

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

    /**
     * Updates an image-format Datasenteret task in place.
     * <p>
     * Rejects tasks whose stored {@link DatacenterTask} envelope is not in image-verification
     * format, since the request DTO would not match the underlying payload. Difficulty is
     * pinned to HARD for Datasenteret and is not taken from the request.
     *
     * @param taskId     the task to update
     * @param request    new metadata
     * @param file       optional replacement image; {@code null} or empty keeps the existing one
     * @param principal  the authenticated teacher
     * @return the persisted task projected as a {@link TaskAdminResponse}
     * @throws EntityNotFoundException  if the task does not exist
     * @throws IllegalArgumentException if the task is not a Datasenteret image task
     * @throws AccessDeniedException    if the teacher is not linked to the task's classroom
     */
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
        Classroom classroom = resolveClassroomForTeacher(task.getClassroom().getClassroomCode(), principal);

        Long fileId = replaceFileIfPresent(file, currentWrapper.imageTask().fileId(), principal, classroom.getClassroomCode());

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

    /**
     * Updates a Nyhetskvartalet news-verification task in place.
     * <p>
     * Either, both, or neither file slot may be replaced. Each missing/empty file leaves the
     * corresponding stored file id untouched; replaced files trigger a best-effort delete of
     * the previous MinIO object. The {@code correctFileId} is recomputed from
     * {@link CorrectFileSlot} against the resulting file id pair.
     *
     * @param taskId     the task to update
     * @param request    new metadata including the correct-file slot
     * @param file1      optional replacement for slot 1
     * @param file2      optional replacement for slot 2
     * @param principal  the authenticated teacher
     * @return the persisted task projected as a {@link TaskAdminResponse}
     * @throws EntityNotFoundException  if the task does not exist
     * @throws IllegalArgumentException if the task does not belong to the Nyhetskvartalet chapter
     *                                  or if {@code correctFileSlot} is null
     * @throws AccessDeniedException    if the teacher is not linked to the task's classroom
     */
    @Transactional
    public TaskAdminResponse updateNewsVerificationTask(Long taskId,
                                                        UpdateNewsVerificationTaskRequest request,
                                                        MultipartFile file1,
                                                        MultipartFile file2,
                                                        JwtUserPrincipal principal) {
        Task task = loadTaskForChapter(taskId, NYHETSKVARTALET_CHAPTER_ID);
        authorizeTaskMutation(task, principal);

        NewsVerificationTask current = deserialize(task.getTaskJson(), NewsVerificationTask.class);
        Classroom classroom = resolveClassroomForTeacher(task.getClassroom().getClassroomCode(), principal);
        Long fileId1 = replaceFileIfPresent(file1, current.fileId_1(), principal, classroom.getClassroomCode());
        Long fileId2 = replaceFileIfPresent(file2, current.fileId_2(), principal, classroom.getClassroomCode());
        Long correctFileId = resolveCorrectFileId(request.correctFileSlot(), fileId1, fileId2);

        NewsVerificationTask updated = new NewsVerificationTask(
                request.taskDescription(),
                fileId1,
                fileId2,
                correctFileId,
                request.correctDescription(),
                request.incorrectDescription(),
                request.altText1(),
                request.altText2()
        );

        task.setDifficulty(request.difficulty());
        task.setTaskJson(serialize(updated));
        return TaskAdminResponse.from(taskRepository.save(task));
    }

    /**
     * Updates a news-format Datasenteret task in place.
     * <p>
     * Rejects tasks whose stored envelope is not in news-verification format. File slot
     * replacement and {@code correctFileId} resolution behave the same as
     * {@link #updateNewsVerificationTask}; difficulty stays HARD.
     *
     * @param taskId     the task to update
     * @param request    new metadata including the correct-file slot
     * @param file1      optional replacement for slot 1
     * @param file2      optional replacement for slot 2
     * @param principal  the authenticated teacher
     * @return the persisted task projected as a {@link TaskAdminResponse}
     * @throws EntityNotFoundException  if the task does not exist
     * @throws IllegalArgumentException if the task is not a Datasenteret news task
     * @throws AccessDeniedException    if the teacher is not linked to the task's classroom
     */
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
        Classroom classroom = resolveClassroomForTeacher(task.getClassroom().getClassroomCode(), principal);
        Long fileId1 = replaceFileIfPresent(file1, current.fileId_1(), principal, classroom.getClassroomCode());
        Long fileId2 = replaceFileIfPresent(file2, current.fileId_2(), principal, classroom.getClassroomCode());
        Long correctFileId = resolveCorrectFileId(request.correctFileSlot(), fileId1, fileId2);

        NewsVerificationTask updated = new NewsVerificationTask(
                request.taskDescription(),
                fileId1,
                fileId2,
                correctFileId,
                request.correctDescription(),
                request.incorrectDescription(),
                request.altText1(),
                request.altText2()
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

    /**
     * Creates a global task (no classroom scope) by serialising the given payload to
     * {@code task_json}. Intended for use by seeders and other internal callers.
     *
     * @param chapter    the chapter the task belongs to
     * @param difficulty the assigned difficulty
     * @param taskData   the chapter-specific DTO that will be serialised into {@code task_json}
     * @return the persisted {@link Task}
     */
    @Transactional
    public Task createTask(Chapter chapter, TaskDifficulty difficulty, Object taskData) {
        return createTask(chapter, difficulty, taskData, null);
    }

    /**
     * Creates a task scoped to {@code classroom} (or global if {@code classroom} is null) by
     * serialising the given payload to {@code task_json}. Used both by classroom-scoped admin
     * endpoints and by seeders that pass {@code classroom = null}.
     *
     * @param chapter    the chapter the task belongs to
     * @param difficulty the assigned difficulty
     * @param taskData   the chapter-specific DTO that will be serialised into {@code task_json}
     * @param classroom  the owning classroom, or {@code null} for a global task
     * @return the persisted {@link Task}
     */
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

    private Long storeAndIndexInClassroom(MultipartFile file, JwtUserPrincipal principal, String classroomCode) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }
        FileObject stored = fileObjectAccessControlService.upload(file, classroomCode, ClassroomAccess.ONLY_CLASS, principal);
        return stored.getId();
    }

    private Long replaceFileIfPresent(MultipartFile file, Long currentFileId, JwtUserPrincipal principal, String classroomCode) {
        if (file == null || file.isEmpty()) {
            return currentFileId;
        }
        Long newId = storeAndIndexInClassroom(file, principal, classroomCode);
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
