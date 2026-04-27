package com.nettdet.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.clearInvocations;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nettdet.backend.domain.Account;
import com.nettdet.backend.domain.Chapter;
import com.nettdet.backend.domain.Classroom;
import com.nettdet.backend.domain.PupilChapterBridgeProgression;
import com.nettdet.backend.domain.PupilClassroomMembership;
import com.nettdet.backend.domain.Task;
import com.nettdet.backend.domain.enums.AccountRole;
import com.nettdet.backend.domain.enums.DatacenterTaskFormat;
import com.nettdet.backend.domain.enums.ImageCategory;
import com.nettdet.backend.domain.enums.TaskDifficulty;
import com.nettdet.backend.domain.ids.PupilChapterBridgeProgressionId;
import com.nettdet.backend.dto.ChapterProgressionResponse;
import com.nettdet.backend.dto.ChapterResponse;
import com.nettdet.backend.dto.DatacenterAnswerRequest;
import com.nettdet.backend.dto.DatacenterRunResponse;
import com.nettdet.backend.dto.DatacenterTask;
import com.nettdet.backend.dto.DatacenterTaskResponse;
import com.nettdet.backend.dto.ImageVerificationRequest;
import com.nettdet.backend.dto.ImageVerificationTask;
import com.nettdet.backend.dto.NewsVerificationRequest;
import com.nettdet.backend.dto.NewsVerificationTask;
import com.nettdet.backend.dto.ValidationResult;
import com.nettdet.backend.repository.ChapterRepository;
import com.nettdet.backend.repository.PupilChapterBridgeProgressionRepository;
import com.nettdet.backend.repository.PupilClassroomMembershipRepository;
import com.nettdet.backend.repository.TaskRepository;
import com.nettdet.backend.validation.ImageVerificationTaskValidator;
import com.nettdet.backend.validation.NewsVerificationTaskValidator;
import com.nettdet.backend.validation.TaskValidator;

@ExtendWith(MockitoExtension.class)
class ChapterServiceTest {

    private static final String PUPIL_EMAIL = "pupil@example.com";
    private static final Long CHAPTER_ID = 1L;
    private static final Long DATACENTER_CHAPTER_ID = 3L;
    private static final String CLASSROOM_CODE = "CHAPAA";

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private PupilClassroomMembershipRepository membershipRepository;

    @Mock
    private PupilChapterBridgeProgressionRepository progressionRepository;

    @Mock
    private ChapterRepository chapterRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private TipService tipService;

    private ObjectMapper objectMapper;
    private ChapterService chapterService;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        List<TaskValidator<?, ?>> validators = List.of(
                new ImageVerificationTaskValidator(),
                new NewsVerificationTaskValidator());
        chapterService = new ChapterService(
                taskRepository,
                membershipRepository,
                progressionRepository,
                chapterRepository,
                objectMapper,
                validators,
                notificationService,
                tipService);
    }

    @DisplayName("Returns all chapters in repository order as chapter responses")
    @Test
    void getAllChapters_whenChaptersExist_returnsChapterResponsesInRepositoryOrder() {
        when(chapterRepository.findAllByOrderByIdAsc()).thenReturn(List.of(
                chapter(1L, "Nyhetskvartalet"),
                chapter(2L, "Fotografen"),
                chapter(3L, "Datasenteret")));

        List<ChapterResponse> response = chapterService.getAllChapters();

        assertEquals(List.of(1L, 2L, 3L), response.stream()
                .map(ChapterResponse::id)
                .toList());
        assertEquals(List.of("Nyhetskvartalet", "Fotografen", "Datasenteret"), response.stream()
                .map(ChapterResponse::chapterName)
                .toList());
    }

    @DisplayName("Returns zeroed chapter progression when pupil has no progression row")
    @Test
    void getProgression_whenProgressionIsMissing_returnsZeroedProgression() {
        when(progressionRepository.findById(new PupilChapterBridgeProgressionId(PUPIL_EMAIL, CHAPTER_ID)))
                .thenReturn(Optional.empty());

        ChapterProgressionResponse response = chapterService.getProgression(PUPIL_EMAIL, CHAPTER_ID);

        assertEquals(0, response.currentRunAttempts());
        assertEquals(0, response.currentRunCorrect());
        assertEquals(0, response.successfulRuns());
        assertEquals(0, response.highscore());
        assertEquals(0, response.unsuccessfulStreak());
    }

    @DisplayName("Returns stored chapter progression counters when progression row exists")
    @Test
    void getProgression_whenProgressionExists_returnsStoredProgressionCounters() {
        PupilChapterBridgeProgression progression = new PupilChapterBridgeProgression();
        progression.setId(new PupilChapterBridgeProgressionId(PUPIL_EMAIL, CHAPTER_ID));
        progression.setCurrentRunAttempts(2);
        progression.setCurrentRunCorrect(1);
        progression.setSuccessfulRuns(3);
        progression.setHighscore(4);
        progression.setUnsuccessfulStreak(5);
        when(progressionRepository.findById(new PupilChapterBridgeProgressionId(PUPIL_EMAIL, CHAPTER_ID)))
                .thenReturn(Optional.of(progression));

        ChapterProgressionResponse response = chapterService.getProgression(PUPIL_EMAIL, CHAPTER_ID);

        assertEquals(2, response.currentRunAttempts());
        assertEquals(1, response.currentRunCorrect());
        assertEquals(3, response.successfulRuns());
        assertEquals(4, response.highscore());
        assertEquals(5, response.unsuccessfulStreak());
    }

    @DisplayName("Datasenteret run returns 5 tasks ordered NEWS, NEWS, IMAGE, IMAGE, IMAGE with no duplicates")
    @Test
    void fetchDatacenterRun_whenPoolIsLargeEnough_returnsFiveTasksInExpectedFormatOrder() throws Exception {
        givenConfirmedClassroom();
        when(taskRepository.findEligibleForPupil(DATACENTER_CHAPTER_ID, TaskDifficulty.HARD, CLASSROOM_CODE))
                .thenReturn(datacenterPool());

        DatacenterRunResponse response = chapterService.fetchDatacenterRun(PUPIL_EMAIL, DATACENTER_CHAPTER_ID);

        List<DatacenterTaskResponse> tasks = response.tasks();
        assertEquals(5, tasks.size());
        assertEquals(DatacenterTaskFormat.NEWS_VERIFICATION, tasks.get(0).format());
        assertEquals(DatacenterTaskFormat.NEWS_VERIFICATION, tasks.get(1).format());
        assertEquals(DatacenterTaskFormat.IMAGE_VERIFICATION, tasks.get(2).format());
        assertEquals(DatacenterTaskFormat.IMAGE_VERIFICATION, tasks.get(3).format());
        assertEquals(DatacenterTaskFormat.IMAGE_VERIFICATION, tasks.get(4).format());

        long distinctIds = tasks.stream().map(DatacenterTaskResponse::taskId).distinct().count();
        assertEquals(5, distinctIds);

        assertNotNull(tasks.get(0).newsTask());
        assertNull(tasks.get(0).imageTask());
        assertNotNull(tasks.get(2).imageTask());
        assertNull(tasks.get(2).newsTask());
    }

    @DisplayName("Datasenteret run rejects requests for chapters other than the boss chapter")
    @Test
    void fetchDatacenterRun_whenChapterIsNotDatasenteret_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> chapterService.fetchDatacenterRun(PUPIL_EMAIL, 1L));
    }

    @DisplayName("Datasenteret run throws when not enough tasks of one format are available")
    @Test
    void fetchDatacenterRun_whenPoolHasInsufficientNewsTasks_throws() throws Exception {
        givenConfirmedClassroom();
        // Only one news task — not enough for 2 news slots.
        List<Task> pool = List.of(
                newsTask(101L, "n1"),
                imageTask(201L, "i1", ImageCategory.REAL),
                imageTask(202L, "i2", ImageCategory.AI_GENERATED),
                imageTask(203L, "i3", ImageCategory.MANIPULATED));
        when(taskRepository.findEligibleForPupil(DATACENTER_CHAPTER_ID, TaskDifficulty.HARD, CLASSROOM_CODE))
                .thenReturn(pool);

        assertThrows(NoSuchElementException.class,
                () -> chapterService.fetchDatacenterRun(PUPIL_EMAIL, DATACENTER_CHAPTER_ID));
    }

    @DisplayName("Datasenteret answer validation dispatches image-format answers to the image validator")
    @Test
    void validateDatacenterAnswer_whenImageAnswerCorrect_returnsCorrectAndRecordsProgressionOnChapter3() throws Exception {
        Task task = imageTask(11L, "i1", ImageCategory.AI_GENERATED);
        when(taskRepository.findById(11L)).thenReturn(Optional.of(task));
        givenProgressionExists(0, 0);

        DatacenterAnswerRequest request = new DatacenterAnswerRequest(
                DatacenterTaskFormat.IMAGE_VERIFICATION,
                new ImageVerificationRequest(ImageCategory.AI_GENERATED, "right"),
                null);

        ValidationResult result = chapterService.validateDatacenterAnswer(PUPIL_EMAIL, 11L, request);

        assertTrue(result.correct());
        assertTrue(result.categoryCorrect());
        assertTrue(result.justificationCorrect());
        assertFalse(result.runCompleted());
        assertEquals(1, result.currentRunAttempts());
        assertEquals(1, result.currentRunCorrect());
    }

    @DisplayName("Datasenteret news-format answers dispatch to the news validator")
    @Test
    void validateDatacenterAnswer_whenNewsAnswerWrong_returnsIncorrect() throws Exception {
        Task task = newsTask(22L, "n1");
        when(taskRepository.findById(22L)).thenReturn(Optional.of(task));
        givenProgressionExists(0, 0);

        DatacenterAnswerRequest request = new DatacenterAnswerRequest(
                DatacenterTaskFormat.NEWS_VERIFICATION,
                null,
                new NewsVerificationRequest(7777L)); // not the correct file id

        ValidationResult result = chapterService.validateDatacenterAnswer(PUPIL_EMAIL, 22L, request);

        assertFalse(result.correct());
        assertNull(result.categoryCorrect());
        assertNull(result.justificationCorrect());
    }

    @DisplayName("Datasenteret answer rejects request format that does not match the stored task format")
    @Test
    void validateDatacenterAnswer_whenFormatMismatch_throws() throws Exception {
        Task task = imageTask(33L, "i1", ImageCategory.REAL);
        when(taskRepository.findById(33L)).thenReturn(Optional.of(task));

        DatacenterAnswerRequest request = new DatacenterAnswerRequest(
                DatacenterTaskFormat.NEWS_VERIFICATION,
                null,
                new NewsVerificationRequest(1L));

        assertThrows(IllegalArgumentException.class,
                () -> chapterService.validateDatacenterAnswer(PUPIL_EMAIL, 33L, request));
    }

    @DisplayName("Datasenteret answer rejects tasks that do not belong to the boss chapter")
    @Test
    void validateDatacenterAnswer_whenTaskNotFromDatasenteret_throws() throws Exception {
        Task task = imageTask(44L, "i1", ImageCategory.REAL);
        task.getChapter().setId(2L); // pretend this is a Fotografen task
        when(taskRepository.findById(44L)).thenReturn(Optional.of(task));

        DatacenterAnswerRequest request = new DatacenterAnswerRequest(
                DatacenterTaskFormat.IMAGE_VERIFICATION,
                new ImageVerificationRequest(ImageCategory.REAL, "right"),
                null);

        assertThrows(IllegalArgumentException.class,
                () -> chapterService.validateDatacenterAnswer(PUPIL_EMAIL, 44L, request));
    }

    @DisplayName("Datasenteret 5/5 correct increments successful runs and resets the unsuccessful streak")
    @Test
    void validateDatacenterAnswer_whenFifthAnswerCompletesPerfectRun_incrementsSuccessfulRuns() throws Exception {
        Task task = imageTask(55L, "i1", ImageCategory.REAL);
        when(taskRepository.findById(55L)).thenReturn(Optional.of(task));
        PupilChapterBridgeProgression progression = givenProgressionExists(4, 4);
        progression.setUnsuccessfulStreak(2);
        progression.setSuccessfulRuns(1);

        DatacenterAnswerRequest request = new DatacenterAnswerRequest(
                DatacenterTaskFormat.IMAGE_VERIFICATION,
                new ImageVerificationRequest(ImageCategory.REAL, "right"),
                null);

        ValidationResult result = chapterService.validateDatacenterAnswer(PUPIL_EMAIL, 55L, request);

        assertTrue(result.correct());
        assertTrue(result.runCompleted());
        assertEquals(2, progression.getSuccessfulRuns());
        assertEquals(0, progression.getUnsuccessfulStreak());
        assertEquals(0, progression.getCurrentRunAttempts());
        assertEquals(0, progression.getCurrentRunCorrect());
        verify(tipService).addAllConfiguredTipsToNotebook(progression);
    }

    @DisplayName("Datasenteret partially-correct run increments unsuccessful streak when the 5th attempt is wrong")
    @Test
    void validateDatacenterAnswer_whenFifthAnswerIncorrectAfterFourCorrect_incrementsUnsuccessfulStreak() throws Exception {
        Task task = imageTask(66L, "i1", ImageCategory.REAL);
        when(taskRepository.findById(66L)).thenReturn(Optional.of(task));
        PupilChapterBridgeProgression progression = givenProgressionExists(4, 4);
        progression.setUnsuccessfulStreak(0);
        progression.setSuccessfulRuns(2);

        DatacenterAnswerRequest request = new DatacenterAnswerRequest(
                DatacenterTaskFormat.IMAGE_VERIFICATION,
                new ImageVerificationRequest(ImageCategory.AI_GENERATED, "wrong"),
                null);

        ValidationResult result = chapterService.validateDatacenterAnswer(PUPIL_EMAIL, 66L, request);

        assertFalse(result.correct());
        assertTrue(result.runCompleted());
        assertEquals(2, progression.getSuccessfulRuns());
        assertEquals(1, progression.getUnsuccessfulStreak());
        verify(tipService, never()).addAllConfiguredTipsToNotebook(any());
    }

    @DisplayName("Creates notification when pupil completes a chapter for the first time")
    @Test
    void validateDatacenterAnswer_whenFirstPerfectRunCompletesChapter_createsCompletionNotification() throws Exception {
        Task task = imageTask(77L, "i1", ImageCategory.REAL);
        when(taskRepository.findById(77L)).thenReturn(Optional.of(task));
        PupilChapterBridgeProgression progression = givenProgressionExists(4, 4);
        progression.setSuccessfulRuns(0);

        DatacenterAnswerRequest request = new DatacenterAnswerRequest(
                DatacenterTaskFormat.IMAGE_VERIFICATION,
                new ImageVerificationRequest(ImageCategory.REAL, "right"),
                null);

        ValidationResult result = chapterService.validateDatacenterAnswer(PUPIL_EMAIL, 77L, request);

        assertTrue(result.runCompleted());
        assertEquals(1, progression.getSuccessfulRuns());
        verify(tipService).addAllConfiguredTipsToNotebook(progression);
        verify(notificationService).createNotification(
                CLASSROOM_CODE,
                PUPIL_EMAIL,
                "Pupil Name har fullført Datasenteret for første gang.");
    }

    @DisplayName("Does not create first-completion notification for later successful runs")
    @Test
    void validateDatacenterAnswer_whenLaterPerfectRunCompletesChapter_doesNotCreateCompletionNotification() throws Exception {
        Task task = imageTask(78L, "i1", ImageCategory.REAL);
        when(taskRepository.findById(78L)).thenReturn(Optional.of(task));
        PupilChapterBridgeProgression progression = givenProgressionExists(4, 4);
        progression.setSuccessfulRuns(1);

        DatacenterAnswerRequest request = new DatacenterAnswerRequest(
                DatacenterTaskFormat.IMAGE_VERIFICATION,
                new ImageVerificationRequest(ImageCategory.REAL, "right"),
                null);

        ValidationResult result = chapterService.validateDatacenterAnswer(PUPIL_EMAIL, 78L, request);

        assertTrue(result.runCompleted());
        assertEquals(2, progression.getSuccessfulRuns());
        verify(tipService).addAllConfiguredTipsToNotebook(progression);
        verifyNoInteractions(notificationService);
    }

    @DisplayName("Creates stuck notification exactly when unsuccessful streak reaches five")
    @Test
    void validateDatacenterAnswer_whenUnsuccessfulStreakReachesFive_createsStuckNotification() throws Exception {
        Task task = imageTask(79L, "i1", ImageCategory.REAL);
        when(taskRepository.findById(79L)).thenReturn(Optional.of(task));
        PupilChapterBridgeProgression progression = givenProgressionExists(4, 4);
        progression.setSuccessfulRuns(2);
        progression.setUnsuccessfulStreak(4);

        DatacenterAnswerRequest request = new DatacenterAnswerRequest(
                DatacenterTaskFormat.IMAGE_VERIFICATION,
                new ImageVerificationRequest(ImageCategory.AI_GENERATED, "wrong"),
                null);

        ValidationResult result = chapterService.validateDatacenterAnswer(PUPIL_EMAIL, 79L, request);

        assertTrue(result.runCompleted());
        assertEquals(5, progression.getUnsuccessfulStreak());
        verify(notificationService).createNotification(
                CLASSROOM_CODE,
                PUPIL_EMAIL,
                "Pupil Name står fast på Datasenteret og kan trenge hjelp.");
    }

    @DisplayName("Does not create repeated stuck notification after streak has already passed five")
    @Test
    void validateDatacenterAnswer_whenUnsuccessfulStreakIsAlreadyFive_doesNotCreateStuckNotification() throws Exception {
        Task task = imageTask(80L, "i1", ImageCategory.REAL);
        when(taskRepository.findById(80L)).thenReturn(Optional.of(task));
        PupilChapterBridgeProgression progression = givenProgressionExists(4, 4);
        progression.setSuccessfulRuns(2);
        progression.setUnsuccessfulStreak(5);

        DatacenterAnswerRequest request = new DatacenterAnswerRequest(
                DatacenterTaskFormat.IMAGE_VERIFICATION,
                new ImageVerificationRequest(ImageCategory.AI_GENERATED, "wrong"),
                null);

        ValidationResult result = chapterService.validateDatacenterAnswer(PUPIL_EMAIL, 80L, request);

        assertTrue(result.runCompleted());
        assertEquals(6, progression.getUnsuccessfulStreak());
        verifyNoInteractions(notificationService);
    }

    @DisplayName("Does not create notification before a run is completed")
    @Test
    void validateDatacenterAnswer_whenRunIsIncomplete_doesNotCreateNotification() throws Exception {
        Task task = imageTask(81L, "i1", ImageCategory.REAL);
        when(taskRepository.findById(81L)).thenReturn(Optional.of(task));
        givenProgressionExists(1, 1);

        DatacenterAnswerRequest request = new DatacenterAnswerRequest(
                DatacenterTaskFormat.IMAGE_VERIFICATION,
                new ImageVerificationRequest(ImageCategory.REAL, "right"),
                null);

        ValidationResult result = chapterService.validateDatacenterAnswer(PUPIL_EMAIL, 81L, request);

        assertFalse(result.runCompleted());
        verifyNoInteractions(notificationService);
        verifyNoInteractions(tipService);
    }

    @DisplayName("Creates stuck notification again after a successful run resets the streak")
    @Test
    void validateDatacenterAnswer_whenStreakWasResetAndReachesFiveAgain_createsNewStuckNotification() throws Exception {
        Task successTask = imageTask(82L, "i1", ImageCategory.REAL);
        Task failedTask = imageTask(83L, "i2", ImageCategory.REAL);
        when(taskRepository.findById(82L)).thenReturn(Optional.of(successTask));
        when(taskRepository.findById(83L)).thenReturn(Optional.of(failedTask));
        PupilChapterBridgeProgression progression = givenProgressionExists(4, 4);
        progression.setSuccessfulRuns(2);
        progression.setUnsuccessfulStreak(4);

        DatacenterAnswerRequest successRequest = new DatacenterAnswerRequest(
                DatacenterTaskFormat.IMAGE_VERIFICATION,
                new ImageVerificationRequest(ImageCategory.REAL, "right"),
                null);

        chapterService.validateDatacenterAnswer(PUPIL_EMAIL, 82L, successRequest);
        assertEquals(0, progression.getUnsuccessfulStreak());
        verify(notificationService, never()).createNotification(
                CLASSROOM_CODE,
                PUPIL_EMAIL,
                "Pupil Name står fast på Datasenteret og kan trenge hjelp.");
        clearInvocations(notificationService);
        progression.setCurrentRunAttempts(4);
        progression.setCurrentRunCorrect(4);
        progression.setUnsuccessfulStreak(4);

        DatacenterAnswerRequest failedRequest = new DatacenterAnswerRequest(
                DatacenterTaskFormat.IMAGE_VERIFICATION,
                new ImageVerificationRequest(ImageCategory.AI_GENERATED, "wrong"),
                null);
        chapterService.validateDatacenterAnswer(PUPIL_EMAIL, 83L, failedRequest);

        assertEquals(5, progression.getUnsuccessfulStreak());
        verify(notificationService).createNotification(
                CLASSROOM_CODE,
                PUPIL_EMAIL,
                "Pupil Name står fast på Datasenteret og kan trenge hjelp.");
    }

    // --------------------------------------------------
    // helpers
    // --------------------------------------------------

    private void givenConfirmedClassroom() {
        when(membershipRepository.findConfirmedClassroomCodeByAccountEmail(PUPIL_EMAIL))
                .thenReturn(Optional.of(CLASSROOM_CODE));
    }

    private PupilChapterBridgeProgression givenProgressionExists(int attempts, int correct) {
        Chapter chapter = chapter(DATACENTER_CHAPTER_ID, "Datasenteret");
        PupilChapterBridgeProgression progression = new PupilChapterBridgeProgression();
        progression.setId(new PupilChapterBridgeProgressionId(PUPIL_EMAIL, DATACENTER_CHAPTER_ID));
        progression.setPupilClassroomMembership(membership(PUPIL_EMAIL, "Pupil Name", classroom()));
        progression.setChapter(chapter);
        progression.setCurrentRunAttempts(attempts);
        progression.setCurrentRunCorrect(correct);
        progression.setHighscore(0);
        progression.setSuccessfulRuns(0);
        progression.setUnsuccessfulStreak(0);
        when(progressionRepository.findById(
                new PupilChapterBridgeProgressionId(PUPIL_EMAIL, DATACENTER_CHAPTER_ID)))
                .thenReturn(Optional.of(progression));
        lenient().when(progressionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        return progression;
    }

    private List<Task> datacenterPool() throws JsonProcessingException {
        return List.of(
                newsTask(101L, "n1"),
                newsTask(102L, "n2"),
                newsTask(103L, "n3"),
                imageTask(201L, "i1", ImageCategory.REAL),
                imageTask(202L, "i2", ImageCategory.AI_GENERATED),
                imageTask(203L, "i3", ImageCategory.MANIPULATED),
                imageTask(204L, "i4", ImageCategory.AI_GENERATED));
    }

    private Task newsTask(Long id, String tag) throws JsonProcessingException {
        NewsVerificationTask inner = new NewsVerificationTask(
                "desc-" + tag, 1L, 2L, 2L, "good", "bad");
        DatacenterTask wrapped = new DatacenterTask(
                DatacenterTaskFormat.NEWS_VERIFICATION, null, inner);
        return buildTask(id, objectMapper.writeValueAsString(wrapped));
    }

    private Task imageTask(Long id, String tag, ImageCategory category) throws JsonProcessingException {
        ImageVerificationTask inner = new ImageVerificationTask(
                "desc-" + tag,
                42L,
                category,
                "right",
                List.of("right", "wrong-1", "wrong-2"),
                "ok",
                "nope");
        DatacenterTask wrapped = new DatacenterTask(
                DatacenterTaskFormat.IMAGE_VERIFICATION, inner, null);
        return buildTask(id, objectMapper.writeValueAsString(wrapped));
    }

    private Task buildTask(Long id, String json) {
        Task task = new Task();
        task.setId(id);
        task.setDifficulty(TaskDifficulty.HARD);
        task.setTaskJson(json);
        Chapter chapter = chapter(DATACENTER_CHAPTER_ID, "Datasenteret");
        task.setChapter(chapter);
        task.setClassroom(null);
        return task;
    }

    private Chapter chapter(Long id, String name) {
        Chapter chapter = new Chapter();
        chapter.setId(id);
        chapter.setChapterName(name);
        chapter.setSuccessfulRunsNeededForCompletion(3);
        return chapter;
    }

    private Classroom classroom() {
        Classroom classroom = new Classroom();
        classroom.setClassroomCode(CLASSROOM_CODE);
        classroom.setClassroomName("Classroom");
        return classroom;
    }

    private Account account(String email, String legalName) {
        Account account = new Account();
        account.setEmail(email);
        account.setLegalName(legalName);
        account.setPasswordData("encoded-password");
        account.setAccountRole(AccountRole.PUPIL);
        return account;
    }

    private PupilClassroomMembership membership(String email, String legalName, Classroom classroom) {
        PupilClassroomMembership membership = new PupilClassroomMembership();
        membership.setAccountEmail(email);
        membership.setAccount(account(email, legalName));
        membership.setClassroom(classroom);
        membership.setUsername("detektiv");
        membership.setMembershipConfirmedByTeacher(true);
        return membership;
    }
}
