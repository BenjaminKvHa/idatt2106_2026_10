package com.nettdet.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.nettdet.backend.domain.Account;
import com.nettdet.backend.domain.Classroom;
import com.nettdet.backend.domain.FileObject;
import com.nettdet.backend.domain.PupilClassroomMembership;
import com.nettdet.backend.domain.enums.AccountRole;
import com.nettdet.backend.dto.AuthMeResponse;
import com.nettdet.backend.dto.LoginRequest;
import com.nettdet.backend.dto.RegisterRequest;
import com.nettdet.backend.dto.UpdateMeRequest;
import com.nettdet.backend.repository.AccountRepository;
import com.nettdet.backend.repository.PupilClassroomMembershipRepository;
import com.nettdet.backend.security.JwtUserPrincipal;

@ExtendWith(MockitoExtension.class)
class AccountServiceTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PupilClassroomMembershipRepository pupilClassroomMembershipRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AvatarService avatarService;

    @InjectMocks
    private AccountService accountService;

    @DisplayName("Registers a new account with encoded password and requested role")
    @Test
    void register_whenEmailIsNew_savesAccountWithEncodedPasswordAndRole() {
        RegisterRequest request = new RegisterRequest(
                "pupil@example.com",
                "Test1234",
                "Pupil Name",
                AccountRole.PUPIL);
        when(accountRepository.findByEmail("pupil@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Test1234")).thenReturn("encoded-password");
        when(accountRepository.save(any(Account.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Account saved = accountService.register(request);

        ArgumentCaptor<Account> accountCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accountRepository).save(accountCaptor.capture());
        Account captured = accountCaptor.getValue();
        assertSame(captured, saved);
        assertEquals("pupil@example.com", captured.getEmail());
        assertEquals("encoded-password", captured.getPasswordData());
        assertEquals("Pupil Name", captured.getLegalName());
        assertEquals(AccountRole.PUPIL, captured.getAccountRole());
    }

    @DisplayName("Rejects duplicate account registration before encoding a password")
    @Test
    void register_whenEmailAlreadyExists_throwsIllegalArgumentException() {
        RegisterRequest request = new RegisterRequest(
                "teacher@example.com",
                "Test1234",
                "Teacher Name",
                AccountRole.TEACHER);
        when(accountRepository.findByEmail("teacher@example.com")).thenReturn(Optional.of(account(
                "teacher@example.com",
                "stored-password",
                "Teacher Name",
                AccountRole.TEACHER)));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> accountService.register(request));

        assertEquals("Email already exists", exception.getMessage());
        verifyNoInteractions(passwordEncoder);
    }

    @DisplayName("Logs in with matching password and returns the stored account")
    @Test
    void login_whenPasswordMatches_returnsAccount() {
        Account account = account("pupil@example.com", "stored-password", "Pupil Name", AccountRole.PUPIL);
        LoginRequest request = new LoginRequest("pupil@example.com", "Test1234");
        when(accountRepository.findByEmail("pupil@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("Test1234", "stored-password")).thenReturn(true);

        Account loggedIn = accountService.login(request);

        assertSame(account, loggedIn);
    }

    @DisplayName("Rejects login for unknown email with generic bad credentials")
    @Test
    void login_whenEmailDoesNotExist_throwsBadCredentialsException() {
        LoginRequest request = new LoginRequest("missing@example.com", "Test1234");
        when(accountRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> accountService.login(request));

        assertEquals("Invalid email or password", exception.getMessage());
        verifyNoInteractions(passwordEncoder);
    }

    @DisplayName("Rejects login for wrong password with generic bad credentials")
    @Test
    void login_whenPasswordDoesNotMatch_throwsBadCredentialsException() {
        Account account = account("pupil@example.com", "stored-password", "Pupil Name", AccountRole.PUPIL);
        LoginRequest request = new LoginRequest("pupil@example.com", "wrong-password");
        when(accountRepository.findByEmail("pupil@example.com")).thenReturn(Optional.of(account));
        when(passwordEncoder.matches("wrong-password", "stored-password")).thenReturn(false);

        BadCredentialsException exception = assertThrows(
                BadCredentialsException.class,
                () -> accountService.login(request));

        assertEquals("Invalid email or password", exception.getMessage());
    }

    @DisplayName("Rejects profile update for non-pupil principals")
    @Test
    void updateAuthenticatedProfile_whenPrincipalIsNotPupil_throwsAccessDenied() {
        JwtUserPrincipal principal = new JwtUserPrincipal("Teacher Name", "teacher@example.com", "TEACHER");
        UpdateMeRequest request = new UpdateMeRequest("newUsername");

        assertThrows(AccessDeniedException.class, () -> accountService.updateAuthenticatedProfile(principal, request));

        verifyNoInteractions(pupilClassroomMembershipRepository);
    }

    @DisplayName("Derives avatar selection from generated avatar object keys")
    @Test
    void getAuthenticatedProfile_whenAvatarKeyIsGenerated_returnsAvatarSelection() {
        Account account = account("pupil@example.com", "stored-password", "Pupil Name", AccountRole.PUPIL);
        PupilClassroomMembership membership = membership(
                account,
                avatar(42L, "avatar-images/white_redditor-black_detective.png"));
        when(accountRepository.findByEmail("pupil@example.com")).thenReturn(Optional.of(account));
        when(pupilClassroomMembershipRepository.findByAccountEmail("pupil@example.com"))
                .thenReturn(Optional.of(membership));

        AuthMeResponse response = accountService.getAuthenticatedProfile("pupil@example.com");

        assertEquals(42L, response.avatarFileObjectId());
        assertEquals("white_redditor-black_detective", response.avatarSelection());
    }

    @DisplayName("Returns no avatar selection for legacy avatar object keys")
    @Test
    void getAuthenticatedProfile_whenAvatarKeyIsLegacy_returnsNullAvatarSelection() {
        Account account = account("pupil@example.com", "stored-password", "Pupil Name", AccountRole.PUPIL);
        PupilClassroomMembership membership = membership(account, avatar(42L, "avatar-images/pupil-start.png"));
        when(accountRepository.findByEmail("pupil@example.com")).thenReturn(Optional.of(account));
        when(pupilClassroomMembershipRepository.findByAccountEmail("pupil@example.com"))
                .thenReturn(Optional.of(membership));

        AuthMeResponse response = accountService.getAuthenticatedProfile("pupil@example.com");

        assertEquals(42L, response.avatarFileObjectId());
        assertNull(response.avatarSelection());
    }

    private Account account(String email, String passwordData, String legalName, AccountRole role) {
        Account account = new Account();
        account.setEmail(email);
        account.setPasswordData(passwordData);
        account.setLegalName(legalName);
        account.setAccountRole(role);
        return account;
    }

    private PupilClassroomMembership membership(Account account, FileObject avatar) {
        Classroom classroom = new Classroom();
        classroom.setClassroomCode("ABCABC");

        PupilClassroomMembership membership = new PupilClassroomMembership();
        membership.setAccountEmail(account.getEmail());
        membership.setAccount(account);
        membership.setClassroom(classroom);
        membership.setUsername("detektiv1");
        membership.setAvatarFileObject(avatar);
        membership.setMembershipConfirmedByTeacher(true);
        return membership;
    }

    private FileObject avatar(Long id, String objectKey) {
        FileObject avatar = new FileObject();
        avatar.setId(id);
        avatar.setObjectKey(objectKey);
        avatar.setFileName(objectKey);
        return avatar;
    }
}
