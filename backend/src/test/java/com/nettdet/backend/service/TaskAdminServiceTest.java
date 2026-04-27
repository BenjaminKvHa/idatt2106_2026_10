package com.nettdet.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nettdet.backend.domain.Chapter;
import com.nettdet.backend.domain.FileObject;
import com.nettdet.backend.domain.Task;
import com.nettdet.backend.domain.enums.ClassroomAccess;
import com.nettdet.backend.domain.enums.DatacenterTaskFormat;
import com.nettdet.backend.domain.enums.ImageCategory;
import com.nettdet.backend.domain.enums.TaskDifficulty;
import com.nettdet.backend.dto.CreateDatacenterImageTaskRequest;
import com.nettdet.backend.dto.CreateDatacenterNewsTaskRequest;
import com.nettdet.backend.dto.CreateImageVerificationTaskRequest;
import com.nettdet.backend.dto.CreateNewsVerificationTaskRequest;
import com.nettdet.backend.dto.FileReferenceResponse;
import com.nettdet.backend.repository.ChapterRepository;
import com.nettdet.backend.repository.FileObjectRepository;
import com.nettdet.backend.repository.TaskRepository;

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
    private FileObjectRepository fileObjectRepository;
    @Mock
    private FileObjectService fileObjectService;

    private ObjectMapper objectMapper;
    private TaskAdminService taskAdminService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        taskAdminService = new TaskAdminService(
                taskRepository, chapterRepository, fileObjectRepository,
                fileObjectService, objectMapper);
    }

    @DisplayName("Image-verification task creation persists chapter, difficulty, and serialized payload")
    @Test
    void createImageVerificationTask_whenChapterAndFileExist_persistsTaskWithSerializedPayload() throws Exception {
        when(chapterRepository.findById(FOTOGRAFEN_ID)).thenReturn(Optional.of(chapter(FOTOGRAFEN_ID, "Fotografen")));
        when(fileObjectRepository.existsById(42L)).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateImageVerificationTaskRequest request = new CreateImageVerificationTaskRequest(
                FOTOGRAFEN_ID,
                TaskDifficulty.MEDIUM,
                "Vurder bildet",
                42L,
                ImageCategory.AI_GENERATED,
                "right",
                List.of("right", "wrong-1", "wrong-2"),
                "ok",
                "nope");

        Task saved = taskAdminService.createImageVerificationTask(request);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        Task captured = captor.getValue();
        assertSame(captured, saved);
        assertEquals(FOTOGRAFEN_ID, captured.getChapter().getId());
        assertEquals(TaskDifficulty.MEDIUM, captured.getDifficulty());
        assertNull(captured.getClassroom());

        JsonNode json = objectMapper.readTree(captured.getTaskJson());
        assertEquals("Vurder bildet", json.get("taskDescription").asText());
        assertEquals(42L, json.get("fileId").asLong());
        assertEquals("AI_GENERATED", json.get("correctCategory").asText());
        assertEquals("right", json.get("correctJustification").asText());
    }

    @DisplayName("Image-verification task creation rejects unknown chapter id")
    @Test
    void createImageVerificationTask_whenChapterMissing_throwsEntityNotFound() {
        when(chapterRepository.findById(FOTOGRAFEN_ID)).thenReturn(Optional.empty());

        CreateImageVerificationTaskRequest request = new CreateImageVerificationTaskRequest(
                FOTOGRAFEN_ID, TaskDifficulty.EASY, "x", 1L,
                ImageCategory.REAL, "j", List.of("j"), "ok", "nope");

        assertThrows(EntityNotFoundException.class,
                () -> taskAdminService.createImageVerificationTask(request));
        verify(taskRepository, never()).save(any());
    }

    @DisplayName("Image-verification task creation rejects unknown file id")
    @Test
    void createImageVerificationTask_whenFileMissing_throwsEntityNotFound() {
        when(chapterRepository.findById(FOTOGRAFEN_ID)).thenReturn(Optional.of(chapter(FOTOGRAFEN_ID, "Fotografen")));
        when(fileObjectRepository.existsById(99L)).thenReturn(false);

        CreateImageVerificationTaskRequest request = new CreateImageVerificationTaskRequest(
                FOTOGRAFEN_ID, TaskDifficulty.EASY, "x", 99L,
                ImageCategory.REAL, "j", List.of("j"), "ok", "nope");

        assertThrows(EntityNotFoundException.class,
                () -> taskAdminService.createImageVerificationTask(request));
        verify(taskRepository, never()).save(any());
    }

    @DisplayName("News-verification task creation persists serialized payload")
    @Test
    void createNewsVerificationTask_whenInputsValid_persistsTaskWithSerializedPayload() throws Exception {
        when(chapterRepository.findById(NYHETSKVARTALET_ID)).thenReturn(Optional.of(chapter(NYHETSKVARTALET_ID, "Nyhetskvartalet")));
        when(fileObjectRepository.existsById(101L)).thenReturn(true);
        when(fileObjectRepository.existsById(102L)).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateNewsVerificationTaskRequest request = new CreateNewsVerificationTaskRequest(
                NYHETSKVARTALET_ID, TaskDifficulty.HARD, "Velg",
                101L, 102L, 102L, "good", "bad");

        Task saved = taskAdminService.createNewsVerificationTask(request);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertSame(captor.getValue(), saved);

        JsonNode json = objectMapper.readTree(captor.getValue().getTaskJson());
        assertEquals(101L, json.get("fileId_1").asLong());
        assertEquals(102L, json.get("fileId_2").asLong());
        assertEquals(102L, json.get("correctFileId").asLong());
    }

    @DisplayName("News-verification task creation rejects correctFileId that is neither candidate")
    @Test
    void createNewsVerificationTask_whenCorrectFileIdMatchesNeither_throwsIllegalArgument() {
        when(chapterRepository.findById(NYHETSKVARTALET_ID)).thenReturn(Optional.of(chapter(NYHETSKVARTALET_ID, "Nyhetskvartalet")));
        when(fileObjectRepository.existsById(101L)).thenReturn(true);
        when(fileObjectRepository.existsById(102L)).thenReturn(true);

        CreateNewsVerificationTaskRequest request = new CreateNewsVerificationTaskRequest(
                NYHETSKVARTALET_ID, TaskDifficulty.HARD, "Velg",
                101L, 102L, 999L, "good", "bad");

        assertThrows(IllegalArgumentException.class,
                () -> taskAdminService.createNewsVerificationTask(request));
        verify(taskRepository, never()).save(any());
    }

    @DisplayName("Datasenteret image task wraps the inner DTO with the IMAGE_VERIFICATION discriminator")
    @Test
    void createDatacenterImageTask_whenInputsValid_persistsWrappedTaskAtHardDifficulty() throws Exception {
        when(chapterRepository.findById(DATACENTER_ID)).thenReturn(Optional.of(chapter(DATACENTER_ID, "Datasenteret")));
        when(fileObjectRepository.existsById(7L)).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateDatacenterImageTaskRequest request = new CreateDatacenterImageTaskRequest(
                "desc", 7L, ImageCategory.MANIPULATED, "j",
                List.of("j", "x", "y"), "ok", "nope");

        Task saved = taskAdminService.createDatacenterImageTask(request);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        Task captured = captor.getValue();
        assertSame(captured, saved);
        assertEquals(DATACENTER_ID, captured.getChapter().getId());
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
        when(fileObjectRepository.existsById(11L)).thenReturn(true);
        when(fileObjectRepository.existsById(12L)).thenReturn(true);
        when(taskRepository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        CreateDatacenterNewsTaskRequest request = new CreateDatacenterNewsTaskRequest(
                "desc", 11L, 12L, 11L, "good", "bad");

        Task saved = taskAdminService.createDatacenterNewsTask(request);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        Task captured = captor.getValue();
        assertSame(captured, saved);
        assertEquals(TaskDifficulty.HARD, captured.getDifficulty());

        JsonNode json = objectMapper.readTree(captured.getTaskJson());
        assertEquals(DatacenterTaskFormat.NEWS_VERIFICATION.name(), json.get("format").asText());
        assertNotNull(json.get("newsTask"));
        assertTrue(json.get("imageTask").isNull());
        assertEquals(11L, json.get("newsTask").get("correctFileId").asLong());
    }

    @DisplayName("Datasenteret news task creation rejects correctFileId not matching either candidate")
    @Test
    void createDatacenterNewsTask_whenCorrectFileIdMatchesNeither_throwsIllegalArgument() {
        when(chapterRepository.findById(DATACENTER_ID)).thenReturn(Optional.of(chapter(DATACENTER_ID, "Datasenteret")));
        when(fileObjectRepository.existsById(11L)).thenReturn(true);
        when(fileObjectRepository.existsById(12L)).thenReturn(true);

        CreateDatacenterNewsTaskRequest request = new CreateDatacenterNewsTaskRequest(
                "desc", 11L, 12L, 99L, "good", "bad");

        assertThrows(IllegalArgumentException.class,
                () -> taskAdminService.createDatacenterNewsTask(request));
        verify(taskRepository, never()).save(any());
    }

    @DisplayName("getOrCreateFileReference returns the file id and newlyCreated flag from the underlying service")
    @Test
    void getOrCreateFileReference_whenFileObjectIsCreated_returnsIdAndCreatedFlag() {
        FileObject fileObject = new FileObject();
        fileObject.setId(77L);
        fileObject.setObjectKey("task-assets/x.png");
        when(fileObjectService.getOrCreateFileObject(
                "task-assets/x.png",
                "image/png",
                null,
                null,
                ClassroomAccess.PUBLIC,
                "x.png"))
                .thenReturn(new FileObjectService.FileObjectResult(fileObject, true));

        FileReferenceResponse response = taskAdminService.getOrCreateFileReference(
                "task-assets/x.png", "image/png", "x.png");

        assertEquals(77L, response.fileId());
        assertEquals("task-assets/x.png", response.objectKey());
        assertTrue(response.newlyCreated());
    }

    private Chapter chapter(Long id, String name) {
        Chapter chapter = new Chapter();
        chapter.setId(id);
        chapter.setChapterName(name);
        chapter.setSuccessfulRunsNeededForCompletion(3);
        return chapter;
    }
}
