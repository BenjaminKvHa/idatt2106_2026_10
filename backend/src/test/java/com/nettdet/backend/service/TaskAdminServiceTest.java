package com.nettdet.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.multipart.MultipartFile;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nettdet.backend.domain.Chapter;
import com.nettdet.backend.domain.Classroom;
import com.nettdet.backend.domain.FileObject;
import com.nettdet.backend.domain.Task;
import com.nettdet.backend.domain.enums.ClassroomAccess;
import com.nettdet.backend.domain.enums.CorrectFileSlot;
import com.nettdet.backend.domain.enums.DatacenterTaskFormat;
import com.nettdet.backend.domain.enums.ImageCategory;
import com.nettdet.backend.domain.enums.TaskDifficulty;
import com.nettdet.backend.dto.CreateDatacenterImageTaskRequest;
import com.nettdet.backend.dto.CreateDatacenterNewsTaskRequest;
import com.nettdet.backend.dto.CreateImageVerificationTaskRequest;
import com.nettdet.backend.dto.CreateNewsVerificationTaskRequest;
import com.nettdet.backend.dto.TaskAdminResponse;
import com.nettdet.backend.dto.UpdateImageVerificationTaskRequest;
import com.nettdet.backend.dto.UpdateNewsVerificationTaskRequest;
import com.nettdet.backend.repository.AccountClassroomBridgeTeacherRepository;
import com.nettdet.backend.repository.ChapterRepository;
import com.nettdet.backend.repository.ClassroomRepository;
import com.nettdet.backend.repository.TaskRepository;
import com.nettdet.backend.security.JwtUserPrincipal;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class TaskAdminServiceTest {

    private static final Long FOTOGRAFEN_ID = 2L;
    private static final Long NYHETSKVARTALET_ID = 1L;
    private static final Long DATACENTER_ID = 3L;

    @Mock
    private TaskRepository taskRepository;
    @Mock
    private ChapterRepository chapterRepository;
    @Mock
    private FileObjectService fileObjectService;
    @Mock
    private FileObjectAccessControlService fileObjectAccessControlService;
    @Mock
    private AccountClassroomBridgeTeacherRepository teacherBridgeRepository;
    @Mock
    private ClassroomRepository classroomRepository;

    private static final String CLASSROOM_CODE = "ABCABC";

    private ObjectMapper objectMapper;
    private TaskAdminService taskAdminService;
    private JwtUserPrincipal teacher;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        taskAdminService = new TaskAdminService(
                taskRepository, chapterRepository, classroomRepository,
                fileObjectService, fileObjectAccessControlService,
                teacherBridgeRepository, objectMapper);
        teacher = new JwtUserPrincipal("Teacher", "teacher@example.com", "TEACHER");
    }

    private Classroom stubClassroomLookup() {
        Classroom c = new Classroom();
        c.setClassroomCode(CLASSROOM_CODE);
        when(teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode(
                "teacher@example.com", CLASSROOM_CODE)).thenReturn(true);
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.of(c));
        return c;
    }

    private MultipartFile mockFile(String name) {
        return new MockMultipartFile(name, name + ".png", "image/png", new byte[]{1, 2, 3});
    }

    private FileObject fileObjectWithId(Long id) {
        FileObject f = new FileObject();
        f.setId(id);
        return f;
    }

    private void stubUploadReturning(Long... ids) {
        var stub = when(fileObjectAccessControlService.upload(any(MultipartFile.class), isNull(),
                eq(ClassroomAccess.PUBLIC), any(JwtUserPrincipal.class)));
        if (ids.length == 1) {
            stub.thenReturn(fileObjectWithId(ids[0]));
        } else {
            FileObject first = fileObjectWithId(ids[0]);
            FileObject[] rest = new FileObject[ids.length - 1];
            for (int i = 1; i < ids.length; i++) rest[i - 1] = fileObjectWithId(ids[i]);
            stub.thenReturn(first, rest);
        }
    }

    @DisplayName("Image-verification task creation uploads file, persists chapter, difficulty, and serialized payload")
    @Test
    void createImageVerificationTask_whenChapterAndFileExist_persistsTaskWithSerializedPayload() throws Exception {
        when(chapterRepository.findById(FOTOGRAFEN_ID)).thenReturn(Optional.of(chapter(FOTOGRAFEN_ID, "Fotografen")));
        stubClassroomLookup();
        stubUploadReturning(42L);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateImageVerificationTaskRequest request = new CreateImageVerificationTaskRequest(
                FOTOGRAFEN_ID,
                CLASSROOM_CODE,
                TaskDifficulty.MEDIUM,
                "Vurder bildet",
                ImageCategory.AI_GENERATED,
                "right",
                List.of("right", "wrong-1", "wrong-2"),
                "ok",
                "nope");

        TaskAdminResponse saved = taskAdminService.createImageVerificationTask(request, mockFile("img"), teacher);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        Task captured = captor.getValue();
        assertEquals(FOTOGRAFEN_ID, saved.chapterId());
        assertEquals(TaskDifficulty.MEDIUM, saved.difficulty());
        assertEquals(CLASSROOM_CODE, saved.classroomCode());

        JsonNode json = objectMapper.readTree(captured.getTaskJson());
        assertEquals("Vurder bildet", json.get("taskDescription").asText());
        assertEquals(42L, json.get("fileId").asLong());
        assertEquals("AI_GENERATED", json.get("correctCategory").asText());
    }

    @DisplayName("Image-verification task creation rejects unknown chapter id")
    @Test
    void createImageVerificationTask_whenChapterMissing_throwsEntityNotFound() {
        when(chapterRepository.findById(FOTOGRAFEN_ID)).thenReturn(Optional.empty());

        CreateImageVerificationTaskRequest request = new CreateImageVerificationTaskRequest(
                FOTOGRAFEN_ID, CLASSROOM_CODE, TaskDifficulty.EASY, "x",
                ImageCategory.REAL, "j", List.of("j"), "ok", "nope");

        assertThrows(EntityNotFoundException.class,
                () -> taskAdminService.createImageVerificationTask(request, mockFile("img"), teacher));
        verify(taskRepository, never()).save(any());
    }

    @DisplayName("Image-verification task creation rejects empty file")
    @Test
    void createImageVerificationTask_whenFileEmpty_throwsIllegalArgument() {
        when(chapterRepository.findById(FOTOGRAFEN_ID)).thenReturn(Optional.of(chapter(FOTOGRAFEN_ID, "Fotografen")));
        stubClassroomLookup();

        CreateImageVerificationTaskRequest request = new CreateImageVerificationTaskRequest(
                FOTOGRAFEN_ID, CLASSROOM_CODE, TaskDifficulty.EASY, "x",
                ImageCategory.REAL, "j", List.of("j"), "ok", "nope");
        MultipartFile empty = new MockMultipartFile("file", "x.png", "image/png", new byte[0]);

        assertThrows(IllegalArgumentException.class,
                () -> taskAdminService.createImageVerificationTask(request, empty, teacher));
        verify(taskRepository, never()).save(any());
    }

    @DisplayName("News-verification task creation uploads both files and resolves correctFileId from slot")
    @Test
    void createNewsVerificationTask_whenInputsValid_persistsTaskWithSerializedPayload() throws Exception {
        when(chapterRepository.findById(NYHETSKVARTALET_ID)).thenReturn(Optional.of(chapter(NYHETSKVARTALET_ID, "Nyhetskvartalet")));
        stubClassroomLookup();
        stubUploadReturning(101L, 102L);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateNewsVerificationTaskRequest request = new CreateNewsVerificationTaskRequest(
                NYHETSKVARTALET_ID, CLASSROOM_CODE, TaskDifficulty.HARD, "Velg",
                CorrectFileSlot.FILE_2, "good", "bad");

        taskAdminService.createNewsVerificationTask(request, mockFile("a"), mockFile("b"), teacher);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());

        JsonNode json = objectMapper.readTree(captor.getValue().getTaskJson());
        assertEquals(101L, json.get("fileId_1").asLong());
        assertEquals(102L, json.get("fileId_2").asLong());
        assertEquals(102L, json.get("correctFileId").asLong());
    }

    @DisplayName("News-verification task creation rejects null correctFileSlot")
    @Test
    void createNewsVerificationTask_whenSlotNull_throwsIllegalArgument() {
        when(chapterRepository.findById(NYHETSKVARTALET_ID)).thenReturn(Optional.of(chapter(NYHETSKVARTALET_ID, "Nyhetskvartalet")));
        stubClassroomLookup();
        stubUploadReturning(101L, 102L);

        CreateNewsVerificationTaskRequest request = new CreateNewsVerificationTaskRequest(
                NYHETSKVARTALET_ID, CLASSROOM_CODE, TaskDifficulty.HARD, "Velg",
                null, "good", "bad");

        assertThrows(IllegalArgumentException.class,
                () -> taskAdminService.createNewsVerificationTask(request, mockFile("a"), mockFile("b"), teacher));
        verify(taskRepository, never()).save(any());
    }

    @DisplayName("Datasenteret image task wraps the inner DTO with the IMAGE_VERIFICATION discriminator at HARD")
    @Test
    void createDatacenterImageTask_whenInputsValid_persistsWrappedTaskAtHardDifficulty() throws Exception {
        when(chapterRepository.findById(DATACENTER_ID)).thenReturn(Optional.of(chapter(DATACENTER_ID, "Datasenteret")));
        stubClassroomLookup();
        stubUploadReturning(7L);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateDatacenterImageTaskRequest request = new CreateDatacenterImageTaskRequest(
                CLASSROOM_CODE, "desc", ImageCategory.MANIPULATED, "j",
                List.of("j", "x", "y"), "ok", "nope");

        TaskAdminResponse saved = taskAdminService.createDatacenterImageTask(request, mockFile("img"), teacher);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        Task captured = captor.getValue();
        assertEquals(TaskDifficulty.HARD, saved.difficulty());
        assertEquals(TaskDifficulty.HARD, captured.getDifficulty());

        JsonNode json = objectMapper.readTree(captured.getTaskJson());
        assertEquals(DatacenterTaskFormat.IMAGE_VERIFICATION.name(), json.get("format").asText());
        assertNotNull(json.get("imageTask"));
        assertTrue(json.get("newsTask").isNull());
        assertEquals(7L, json.get("imageTask").get("fileId").asLong());
    }

    @DisplayName("Datasenteret news task wraps the inner DTO with the NEWS_VERIFICATION discriminator")
    @Test
    void createDatacenterNewsTask_whenInputsValid_persistsWrappedTaskAtHardDifficulty() throws Exception {
        when(chapterRepository.findById(DATACENTER_ID)).thenReturn(Optional.of(chapter(DATACENTER_ID, "Datasenteret")));
        stubClassroomLookup();
        stubUploadReturning(11L, 12L);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateDatacenterNewsTaskRequest request = new CreateDatacenterNewsTaskRequest(
                CLASSROOM_CODE, "desc", CorrectFileSlot.FILE_1, "good", "bad");

        taskAdminService.createDatacenterNewsTask(request, mockFile("a"), mockFile("b"), teacher);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());

        JsonNode json = objectMapper.readTree(captor.getValue().getTaskJson());
        assertEquals(DatacenterTaskFormat.NEWS_VERIFICATION.name(), json.get("format").asText());
        assertEquals(11L, json.get("newsTask").get("correctFileId").asLong());
    }

    // ----- UPDATE -----

    @DisplayName("Updating a Fotografen task without a file part keeps the existing fileId")
    @Test
    void updateImageVerificationTask_whenNoFile_keepsExistingFileId() throws Exception {
        Task existing = imageTaskRow(50L, FOTOGRAFEN_ID, 42L, null);
        when(taskRepository.findById(50L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateImageVerificationTaskRequest req = new UpdateImageVerificationTaskRequest(
                TaskDifficulty.HARD, "new desc", ImageCategory.REAL, "j",
                List.of("j"), "ok", "nope");

        taskAdminService.updateImageVerificationTask(50L, req, null, teacher);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        JsonNode json = objectMapper.readTree(captor.getValue().getTaskJson());
        assertEquals(42L, json.get("fileId").asLong());
        assertEquals("new desc", json.get("taskDescription").asText());
        assertEquals(TaskDifficulty.HARD, captor.getValue().getDifficulty());
        verify(fileObjectService, never()).delete(any());
    }

    @DisplayName("Updating a Fotografen task with a new file uploads it and deletes the old one")
    @Test
    void updateImageVerificationTask_whenNewFile_replacesOldFile() throws Exception {
        Task existing = imageTaskRow(50L, FOTOGRAFEN_ID, 42L, null);
        when(taskRepository.findById(50L)).thenReturn(Optional.of(existing));
        stubUploadReturning(99L);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateImageVerificationTaskRequest req = new UpdateImageVerificationTaskRequest(
                TaskDifficulty.EASY, "x", ImageCategory.REAL, "j",
                List.of("j"), "ok", "nope");

        taskAdminService.updateImageVerificationTask(50L, req, mockFile("new"), teacher);

        verify(fileObjectService).delete(42L);
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        JsonNode json = objectMapper.readTree(captor.getValue().getTaskJson());
        assertEquals(99L, json.get("fileId").asLong());
    }

    @DisplayName("Updating a task whose chapter mismatches the endpoint is rejected")
    @Test
    void updateImageVerificationTask_whenChapterMismatch_throwsIllegalArgument() {
        Task existing = imageTaskRow(50L, NYHETSKVARTALET_ID, 42L, null);
        when(taskRepository.findById(50L)).thenReturn(Optional.of(existing));

        UpdateImageVerificationTaskRequest req = new UpdateImageVerificationTaskRequest(
                TaskDifficulty.EASY, "x", ImageCategory.REAL, "j",
                List.of("j"), "ok", "nope");

        assertThrows(IllegalArgumentException.class,
                () -> taskAdminService.updateImageVerificationTask(50L, req, null, teacher));
    }

    @DisplayName("Updating a classroom-scoped task is denied if teacher is not in that classroom")
    @Test
    void updateImageVerificationTask_whenTeacherNotInClassroom_throwsAccessDenied() {
        Classroom classroom = new Classroom();
        classroom.setClassroomCode("ABCABC");
        Task existing = imageTaskRow(50L, FOTOGRAFEN_ID, 42L, classroom);
        when(taskRepository.findById(50L)).thenReturn(Optional.of(existing));
        when(teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode("teacher@example.com", "ABCABC"))
                .thenReturn(false);

        UpdateImageVerificationTaskRequest req = new UpdateImageVerificationTaskRequest(
                TaskDifficulty.EASY, "x", ImageCategory.REAL, "j",
                List.of("j"), "ok", "nope");

        assertThrows(AccessDeniedException.class,
                () -> taskAdminService.updateImageVerificationTask(50L, req, null, teacher));
    }

    @DisplayName("Updating a Nyhetskvartalet task replaces only the file slot that was provided")
    @Test
    void updateNewsVerificationTask_whenOnlyFile2Provided_replacesOnlyFile2() throws Exception {
        Task existing = newsTaskRow(60L, NYHETSKVARTALET_ID, 11L, 12L, 12L);
        when(taskRepository.findById(60L)).thenReturn(Optional.of(existing));
        stubUploadReturning(202L);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        UpdateNewsVerificationTaskRequest req = new UpdateNewsVerificationTaskRequest(
                TaskDifficulty.MEDIUM, "x", CorrectFileSlot.FILE_2, "ok", "nope");

        taskAdminService.updateNewsVerificationTask(60L, req, null, mockFile("new2"), teacher);

        verify(fileObjectService).delete(12L);
        verify(fileObjectService, never()).delete(11L);
        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        JsonNode json = objectMapper.readTree(captor.getValue().getTaskJson());
        assertEquals(11L, json.get("fileId_1").asLong());
        assertEquals(202L, json.get("fileId_2").asLong());
        assertEquals(202L, json.get("correctFileId").asLong());
    }

    // ----- LISTING -----

    @DisplayName("Listing tasks for a classroom returns tasks scoped to that chapter and classroom when teacher is authorized")
    @Test
    void listFotografenTasksForClassroom_whenTeacherAuthorized_returnsScopedTasks() {
        String email = "teacher@example.com";
        String code = "ABCABC";
        Task t = new Task();
        t.setDifficulty(TaskDifficulty.EASY);
        t.setChapter(chapter(FOTOGRAFEN_ID, "Fotografen"));
        t.setTaskJson("{}");
        when(teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode(email, code)).thenReturn(true);
        when(taskRepository.findByChapterIdAndClassroomClassroomCode(FOTOGRAFEN_ID, code))
                .thenReturn(List.of(t));

        List<TaskAdminResponse> result = taskAdminService.listFotografenTasksForClassroom(email, code);

        assertEquals(1, result.size());
        assertEquals(FOTOGRAFEN_ID, result.get(0).chapterId());
    }

    @DisplayName("Listing tasks for a classroom rejects callers who do not teach that classroom")
    @Test
    void listFotografenTasksForClassroom_whenTeacherNotInClassroom_throwsAccessDenied() {
        when(teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode("intruder@example.com", "ABCABC"))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class,
                () -> taskAdminService.listFotografenTasksForClassroom("intruder@example.com", "ABCABC"));
        verify(taskRepository, never()).findByChapterIdAndClassroomClassroomCode(any(), any());
    }

    // ----- helpers -----

    private Chapter chapter(Long id, String name) {
        Chapter chapter = new Chapter();
        chapter.setId(id);
        chapter.setChapterName(name);
        chapter.setSuccessfulRunsNeededForCompletion(3);
        return chapter;
    }

    private Task imageTaskRow(Long id, Long chapterId, Long fileId, Classroom classroom) {
        Task t = new Task();
        t.setId(id);
        t.setChapter(chapter(chapterId, "c"));
        t.setClassroom(classroom);
        t.setDifficulty(TaskDifficulty.EASY);
        t.setTaskJson(String.format(
                "{\"taskDescription\":\"old\",\"fileId\":%d,\"correctCategory\":\"REAL\","
                + "\"correctJustification\":\"j\",\"justifications\":[\"j\"],"
                + "\"correctDescription\":\"ok\",\"falseDescription\":\"nope\"}", fileId));
        return t;
    }

    private Task newsTaskRow(Long id, Long chapterId, Long fileId1, Long fileId2, Long correctFileId) {
        Task t = new Task();
        t.setId(id);
        t.setChapter(chapter(chapterId, "c"));
        t.setDifficulty(TaskDifficulty.EASY);
        t.setTaskJson(String.format(
                "{\"taskDescription\":\"old\",\"fileId_1\":%d,\"fileId_2\":%d,"
                + "\"correctFileId\":%d,\"correctDescription\":\"ok\",\"incorrectDescription\":\"nope\"}",
                fileId1, fileId2, correctFileId));
        return t;
    }
}
