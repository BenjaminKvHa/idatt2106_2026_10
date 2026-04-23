package com.nettdet.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.AccessDeniedException;

import com.nettdet.backend.domain.Account;
import com.nettdet.backend.domain.Classroom;
import com.nettdet.backend.domain.FileObject;
import com.nettdet.backend.domain.enums.ClassroomAccess;
import com.nettdet.backend.repository.AccountClassroomBridgeTeacherRepository;
import com.nettdet.backend.repository.AccountRepository;
import com.nettdet.backend.repository.ClassroomRepository;
import com.nettdet.backend.repository.FileObjectRepository;
import com.nettdet.backend.repository.PupilClassroomMembershipRepository;
import com.nettdet.backend.security.JwtUserPrincipal;

@ExtendWith(MockitoExtension.class)
class FileObjectAccessControlServiceTest {

    private static final String CLASSROOM_CODE = "ABCABC";

    @Mock
    private FileObjectService fileObjectService;

    @Mock
    private FileObjectRepository fileObjectRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private AccountClassroomBridgeTeacherRepository teacherBridgeRepository;

    @Mock
    private PupilClassroomMembershipRepository pupilMembershipRepository;

    private FileObjectAccessControlService service;

    @BeforeEach
    void setUp() {
        service = new FileObjectAccessControlService(
                fileObjectService,
                fileObjectRepository,
                accountRepository,
                classroomRepository,
                teacherBridgeRepository,
                pupilMembershipRepository);
    }

    @DisplayName("Allows public file reads without requiring a principal")
    @Test
    void read_whenFileIsPublic_allowsReadWithoutPrincipal() {
        FileObject fileObject = fileObject(7L, ClassroomAccess.PUBLIC, null, null);
        FileObjectService.StoredFile storedFile = new FileObjectService.StoredFile(
                fileObject,
                "image/png",
                new byte[] {1});
        when(fileObjectRepository.findById(7L)).thenReturn(Optional.of(fileObject));
        when(fileObjectService.read(7L)).thenReturn(storedFile);

        FileObjectService.StoredFile response = service.read(7L, null);

        assertEquals(storedFile, response);
        verify(fileObjectService).read(7L);
        verifyNoInteractions(teacherBridgeRepository, pupilMembershipRepository);
    }

    @DisplayName("Denies teacher-only file reads for users who are not teachers in the classroom")
    @Test
    void read_whenFileIsTeacherOnlyAndActorIsNotTeacher_deniesAccess() {
        FileObject fileObject = fileObject(8L, ClassroomAccess.ONLY_TEACHERS_IN_CLASS, classroom(), null);
        when(fileObjectRepository.findById(8L)).thenReturn(Optional.of(fileObject));
        when(teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode("pupil@example.com", CLASSROOM_CODE))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.read(8L, principal("Pupil", "pupil@example.com")));

        verifyNoInteractions(fileObjectService);
    }

    @DisplayName("Allows class file reads for confirmed pupils in the classroom")
    @Test
    void read_whenFileIsClassOnlyAndActorIsPupilInClass_allowsRead() {
        FileObject fileObject = fileObject(9L, ClassroomAccess.ONLY_CLASS, classroom(), null);
        FileObjectService.StoredFile storedFile = new FileObjectService.StoredFile(
                fileObject,
                "application/pdf",
                new byte[] {2});
        when(fileObjectRepository.findById(9L)).thenReturn(Optional.of(fileObject));
        when(teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode("pupil@example.com", CLASSROOM_CODE))
                .thenReturn(false);
        when(pupilMembershipRepository.existsByAccountEmailAndClassroomClassroomCode("pupil@example.com", CLASSROOM_CODE))
                .thenReturn(true);
        when(fileObjectService.read(9L)).thenReturn(storedFile);

        FileObjectService.StoredFile response = service.read(9L, principal("Pupil", "pupil@example.com"));

        assertEquals(storedFile, response);
    }

    @DisplayName("Rejects non-public uploads without classroom code")
    @Test
    void upload_whenFileIsNonPublicAndClassroomCodeMissing_rejectsRequest() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "notes.txt",
                "text/plain",
                "content".getBytes());
        Account actor = account("teacher@example.com");
        when(accountRepository.findById("teacher@example.com")).thenReturn(Optional.of(actor));

        assertThrows(IllegalArgumentException.class, () -> service.upload(
                file,
                null,
                ClassroomAccess.ONLY_CLASS,
                principal("Teacher", "teacher@example.com")));

        verifyNoInteractions(fileObjectService);
    }

    @DisplayName("Denies teacher-only uploads from pupils even when they belong to the classroom")
    @Test
    void upload_whenTeacherOnlyFileAndActorIsOnlyPupilInClass_deniesUpload() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "teacher-notes.txt",
                "text/plain",
                "content".getBytes());
        Account actor = account("pupil@example.com");
        Classroom classroom = classroom();
        when(accountRepository.findById("pupil@example.com")).thenReturn(Optional.of(actor));
        when(classroomRepository.findById(CLASSROOM_CODE)).thenReturn(Optional.of(classroom));
        when(teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode("pupil@example.com", CLASSROOM_CODE))
                .thenReturn(false);
        when(pupilMembershipRepository.existsByAccountEmailAndClassroomClassroomCode("pupil@example.com", CLASSROOM_CODE))
                .thenReturn(true);

        assertThrows(AccessDeniedException.class, () -> service.upload(
                file,
                CLASSROOM_CODE,
                ClassroomAccess.ONLY_TEACHERS_IN_CLASS,
                principal("Pupil", "pupil@example.com")));

        verifyNoInteractions(fileObjectService);
    }

    @DisplayName("Uploads public files with default public access when access level is omitted")
    @Test
    void upload_whenAccessLevelIsNull_uploadsAsPublicFile() {
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "avatar.png",
                "image/png",
                new byte[] {3});
        Account actor = account("teacher@example.com");
        FileObject stored = fileObject(10L, ClassroomAccess.PUBLIC, null, actor);
        when(accountRepository.findById("teacher@example.com")).thenReturn(Optional.of(actor));
        when(fileObjectService.upload(
                any(String.class),
                eq(new byte[] {3}),
                eq("image/png"),
                eq(actor),
                eq(null),
                eq(ClassroomAccess.PUBLIC),
                eq("avatar.png")))
                .thenReturn(stored);

        FileObject response = service.upload(file, null, null, principal("Teacher", "teacher@example.com"));

        assertEquals(stored, response);
    }

    @DisplayName("Allows uploader to delete their own file")
    @Test
    void delete_whenActorIsUploader_deletesFile() {
        Account uploader = account("teacher@example.com");
        FileObject fileObject = fileObject(11L, ClassroomAccess.PUBLIC, null, uploader);
        when(fileObjectRepository.findById(11L)).thenReturn(Optional.of(fileObject));

        service.delete(11L, principal("Teacher", "teacher@example.com"));

        verify(fileObjectService).delete(11L);
    }

    @DisplayName("Denies delete when actor is neither uploader nor classroom teacher")
    @Test
    void delete_whenActorIsNotUploaderOrClassroomTeacher_deniesDelete() {
        Account uploader = account("uploader@example.com");
        FileObject fileObject = fileObject(12L, ClassroomAccess.ONLY_CLASS, classroom(), uploader);
        when(fileObjectRepository.findById(12L)).thenReturn(Optional.of(fileObject));
        when(teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode("other@example.com", CLASSROOM_CODE))
                .thenReturn(false);

        assertThrows(AccessDeniedException.class, () -> service.delete(12L, principal("Other", "other@example.com")));

        verifyNoInteractions(fileObjectService);
    }

    private JwtUserPrincipal principal(String legalName, String email) {
        return new JwtUserPrincipal(legalName, email, "TEACHER");
    }

    private Account account(String email) {
        Account account = new Account();
        account.setEmail(email);
        account.setLegalName("User");
        account.setPasswordData("password");
        return account;
    }

    private Classroom classroom() {
        Classroom classroom = new Classroom();
        classroom.setClassroomCode(CLASSROOM_CODE);
        classroom.setClassroomName("Classroom");
        return classroom;
    }

    private FileObject fileObject(
            Long id,
            ClassroomAccess accessLevel,
            Classroom classroom,
            Account uploadedBy) {
        FileObject fileObject = new FileObject();
        fileObject.setId(id);
        fileObject.setObjectKey("object-" + id + ".png");
        fileObject.setFileName("object-" + id + ".png");
        fileObject.setAccessLevel(accessLevel);
        fileObject.setClassroom(classroom);
        fileObject.setUploadedByAccount(uploadedBy);
        return fileObject;
    }
}
