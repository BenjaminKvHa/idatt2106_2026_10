package com.nettdet.backend.service;

import java.util.Comparator;
import java.util.List;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nettdet.backend.domain.Account;
import com.nettdet.backend.domain.AccountClassroomBridgeTeacher;
import com.nettdet.backend.domain.Classroom;
import com.nettdet.backend.domain.FileObject;
import com.nettdet.backend.domain.PupilClassroomMembership;
import com.nettdet.backend.domain.enums.AccountRole;
import com.nettdet.backend.domain.ids.AccountClassroomBridgeTeacherId;
import com.nettdet.backend.dto.AcceptedClassroomPupilResponse;
import com.nettdet.backend.dto.AcceptedPupilMembershipResponse;
import com.nettdet.backend.dto.ClassroomResponse;
import com.nettdet.backend.dto.ClassroomTeacherResponse;
import com.nettdet.backend.dto.CreateClassroomRequest;
import com.nettdet.backend.dto.InviteTeacherRequest;
import com.nettdet.backend.dto.PendingPupilRequestResponse;
import com.nettdet.backend.dto.TeacherLeaveStatusResponse;
import com.nettdet.backend.dto.UpdateClassroomRequest;
import com.nettdet.backend.dto.UpdatePupilUsernameRequest;
import com.nettdet.backend.dto.ValidateClassroomCodeRequest;
import com.nettdet.backend.dto.ValidateClassroomCodeResponse;
import com.nettdet.backend.repository.AccountClassroomBridgeTeacherRepository;
import com.nettdet.backend.repository.AccountRepository;
import com.nettdet.backend.repository.ClassroomRepository;
import com.nettdet.backend.repository.PupilClassroomMembershipRepository;
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.util.ClassroomCodeGenerator;

/**
 * Service class for managing classrooms and their memberships.
 * 
 * <p>Handles the entire lifecycle of a classroom, including:
 * <ul>
 *   <li>Creation and deletion of classrooms with unique code generation.</li>
 *   <li>Management of teacher ownership and collaboration (inviting/leaving).</li>
 *   <li>Pupil join requests and teacher approval workflows.</li>
 *   <li>Management of pupil memberships, including kicking and updating usernames.</li>
 *   <li>Retrieval of classroom details and member lists (teachers and pupils).</li>
 * </ul>
 * 
 * <p>Security is enforced throughout by verifying the role and classroom 
 * ownership of the acting principal.
 */
@Service
public class ClassroomService {

    private static final int MAX_CODE_GENERATION_ATTEMPTS = 10;

    private final ClassroomRepository classroomRepository;
    private final AccountClassroomBridgeTeacherRepository bridgeRepository;
    private final AccountRepository accountRepository;
    private final PupilClassroomMembershipRepository pupilClassroomMembershipRepository;
    private final ClassroomCodeGenerator classroomCodeGenerator;
    private final PupilXpService pupilXpService;

    public ClassroomService(ClassroomRepository classroomRepository,
                            AccountClassroomBridgeTeacherRepository bridgeRepository,
                            AccountRepository accountRepository,
                            PupilClassroomMembershipRepository pupilClassroomMembershipRepository,
                            ClassroomCodeGenerator classroomCodeGenerator,
                            PupilXpService pupilXpService) {
        this.classroomRepository = classroomRepository;
        this.bridgeRepository = bridgeRepository;
        this.accountRepository = accountRepository;
        this.pupilClassroomMembershipRepository = pupilClassroomMembershipRepository;
        this.classroomCodeGenerator = classroomCodeGenerator;
        this.pupilXpService = pupilXpService;
    }

    /**
     * Creates a new classroom and assigns the acting teacher as its owner.
     * 
     * <p>Generates a unique classroom code and establishes a bridge between the teacher 
     * and the new classroom. Retries code generation if a collision occurs.
     *
     * @param request   the classroom details (name, description)
     * @param principal the authenticated teacher principal
     * @return the created {@link ClassroomResponse}
     * @throws AccessDeniedException if the principal is not a teacher
     * @throws IllegalStateException if a unique code cannot be generated after multiple attempts
     */
    @Transactional
    public ClassroomResponse createClassroom(CreateClassroomRequest request, JwtUserPrincipal principal) {
        verifyTeacherPrincipal(principal);
        Account actingTeacher = getRequiredAccount(principal.email());

        String normalizedName = normalizeName(request.name());
        String normalizedDescription = normalizeDescription(request.description());

        for (int attempt = 0; attempt < MAX_CODE_GENERATION_ATTEMPTS; attempt++) {
            String classroomCode = classroomCodeGenerator.generateCode();

            if (classroomRepository.existsById(classroomCode)) {
                continue;
            }

            Classroom classroom = new Classroom();
            classroom.setClassroomCode(classroomCode);
            classroom.setClassroomName(normalizedName);
            classroom.setClassroomDescription(normalizedDescription);

            try {
                Classroom savedClassroom = classroomRepository.saveAndFlush(classroom);

                AccountClassroomBridgeTeacher bridge = new AccountClassroomBridgeTeacher();
                bridge.setId(new AccountClassroomBridgeTeacherId(principal.email(), classroomCode));
                bridge.setAccount(actingTeacher);
                bridge.setClassroom(savedClassroom);
                bridgeRepository.save(bridge);

                return toResponse(savedClassroom);
            } catch (DataIntegrityViolationException exception) {
                // Retry on the rare chance that the generated classroom code collides concurrently.
            }
        }

        throw new IllegalStateException("Unable to generate a unique classroom code");
    }

    /**
     * Validates if a classroom code exists in the system.
     * 
     * @param request   the request containing the classroom code
     * @param principal the authenticated pupil principal
     * @return a {@link ValidateClassroomCodeResponse} indicating if the code is valid
     * @throws AccessDeniedException if the principal is not a pupil
     */
    @Transactional(readOnly = true)
    public ValidateClassroomCodeResponse validateClassroomCode(
            ValidateClassroomCodeRequest request,
            JwtUserPrincipal principal) {
        verifyPupilPrincipal(principal);

        String classroomCode = request == null || request.classroomCode() == null
                ? ""
                : request.classroomCode().trim().toUpperCase();

        return new ValidateClassroomCodeResponse(!classroomCode.isBlank()
                && classroomRepository.existsById(classroomCode));
    }

    /**
     * Invites another teacher to collaborate in a classroom.
     * 
     * <p>Verifies that the acting principal is an owner of the classroom and that the 
     * invited account has the teacher role.
     *
     * @param classroomCode the code of the classroom
     * @param request       the details of the teacher to invite
     * @param principal     the authenticated teacher principal
     * @throws AccessDeniedException    if the principal is not a teacher or not an owner
     * @throws IllegalArgumentException if the invited account is not a teacher
     * @throws EntityNotFoundException  if the classroom or invited account is not found
     */
    @Transactional
    public void inviteTeacher(String classroomCode,
                              InviteTeacherRequest request,
                              JwtUserPrincipal principal) {
        verifyTeacherPrincipal(principal);

        Classroom classroom = getRequiredClassroom(classroomCode);
        verifyTeacherOwnership(principal.email(), classroomCode);

        String invitedEmail = normalizeEmail(request.email());
        Account invitedAccount = getRequiredAccount(invitedEmail);
        if (invitedAccount.getAccountRole() != AccountRole.TEACHER) {
            throw new IllegalArgumentException("Account must have teacher role");
        }

        if (bridgeRepository.existsByIdAccountEmailAndIdClassroomCode(invitedEmail, classroomCode)) {
            return;
        }

        AccountClassroomBridgeTeacher bridge = new AccountClassroomBridgeTeacher();
        bridge.setId(new AccountClassroomBridgeTeacherId(invitedEmail, classroomCode));
        bridge.setAccount(invitedAccount);
        bridge.setClassroom(classroom);
        bridgeRepository.save(bridge);
    }

    /**
     * Updates the basic information of a classroom.
     * 
     * @param classroomCode the code of the classroom
     * @param request       the new name and description
     * @param principal     the authenticated teacher principal
     * @return the updated {@link ClassroomResponse}
     * @throws AccessDeniedException   if the principal is not a teacher or not an owner
     * @throws EntityNotFoundException if the classroom is not found
     */
    @Transactional
    public ClassroomResponse updateClassroom(String classroomCode,
                                             UpdateClassroomRequest request,
                                             JwtUserPrincipal principal) {
        verifyTeacherPrincipal(principal);

        Classroom classroom = getRequiredClassroom(classroomCode);
        verifyTeacherOwnership(principal.email(), classroomCode);

        classroom.setClassroomName(normalizeName(request.name()));
        classroom.setClassroomDescription(normalizeDescription(request.description()));

        return toResponse(classroomRepository.save(classroom));
    }

    /**
     * Deletes a classroom and all associated data.
     * 
     * <p>This is a destructive operation that removes the classroom, its pupil 
     * memberships, and teacher bridges.
     *
     * @param classroomCode the code of the classroom to delete
     * @param principal     the authenticated teacher principal
     * @throws AccessDeniedException   if the principal is not a teacher or not an owner
     * @throws EntityNotFoundException if the classroom is not found
     */
    @Transactional
    public void deleteClassroom(String classroomCode, JwtUserPrincipal principal) {
        verifyTeacherPrincipal(principal);

        Classroom classroom = getRequiredClassroom(classroomCode);
        verifyTeacherOwnership(principal.email(), classroomCode);
        classroomRepository.delete(classroom);
    }

    /**
     * Checks if a teacher is allowed to leave a classroom.
     * 
     * <p>A teacher can only leave if there is at least one other teacher remaining 
     * in the classroom.
     *
     * @param classroomCode the code of the classroom
     * @param principal     the authenticated teacher principal
     * @return a {@link TeacherLeaveStatusResponse} indicating if leaving is allowed
     * @throws AccessDeniedException   if the principal is not a teacher or not an owner
     * @throws EntityNotFoundException if the classroom is not found
     */
    @Transactional(readOnly = true)
    public TeacherLeaveStatusResponse getTeacherLeaveStatus(String classroomCode, JwtUserPrincipal principal) {
        verifyTeacherPrincipal(principal);
        getRequiredClassroom(classroomCode);
        verifyTeacherOwnership(principal.email(), classroomCode);

        return new TeacherLeaveStatusResponse(canTeacherLeave(classroomCode));
    }

    /**
     * Removes the acting teacher's ownership from a classroom.
     * 
     * @param classroomCode the code of the classroom
     * @param principal     the authenticated teacher principal
     * @throws AccessDeniedException    if the principal is not a teacher or not an owner
     * @throws IllegalArgumentException if the teacher is the last one in the classroom
     * @throws EntityNotFoundException  if the classroom is not found
     */
    @Transactional
    public void leaveClassroom(String classroomCode, JwtUserPrincipal principal) {
        verifyTeacherPrincipal(principal);
        getRequiredClassroom(classroomCode);
        verifyTeacherOwnership(principal.email(), classroomCode);

        if (!canTeacherLeave(classroomCode)) {
            throw new IllegalArgumentException("Cannot leave classroom without another teacher remaining");
        }

        bridgeRepository.deleteByIdAccountEmailAndIdClassroomCode(principal.email(), classroomCode);
    }

    /**
     * Removes the acting pupil's membership from a classroom.
     * 
     * @param classroomCode the code of the classroom
     * @param principal     the authenticated pupil principal
     * @throws AccessDeniedException   if the principal is not a pupil
     * @throws EntityNotFoundException if the membership or classroom is not found
     */
    @Transactional
    public void leaveClassroomAsPupil(String classroomCode, JwtUserPrincipal principal) {
        verifyPupilPrincipal(principal);
        getRequiredClassroom(classroomCode);

        PupilClassroomMembership membership = pupilClassroomMembershipRepository
                .findByAccountEmailAndClassroomClassroomCode(principal.email(), classroomCode)
                .orElseThrow(() -> new EntityNotFoundException("Pupil membership not found"));

        pupilClassroomMembershipRepository.delete(membership);
    }

    /**
     * Retrieves all pending pupil join requests for a classroom.
     * 
     * <p>Results are sorted by pupil legal name and email.
     *
     * @param classroomCode the code of the classroom
     * @param principal     the authenticated teacher principal
     * @return a list of {@link PendingPupilRequestResponse} objects
     * @throws AccessDeniedException   if the principal is not a teacher or not an owner
     * @throws EntityNotFoundException if the classroom is not found
     */
    @Transactional(readOnly = true)
    public List<PendingPupilRequestResponse> getPendingPupilRequests(String classroomCode, JwtUserPrincipal principal) {
        verifyTeacherPrincipal(principal);
        getRequiredClassroom(classroomCode);
        verifyTeacherOwnership(principal.email(), classroomCode);

        return pupilClassroomMembershipRepository
                .findAllByClassroomClassroomCodeAndMembershipConfirmedByTeacherFalse(classroomCode)
                .stream()
                .sorted(Comparator.comparing(
                                (PupilClassroomMembership membership) -> membership.getAccount().getLegalName(),
                                String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(PupilClassroomMembership::getAccountEmail))
                .map(this::toPendingPupilRequestResponse)
                .toList();
    }

    /**
     * Retrieves the details of a specific classroom.
     * 
     * <p>Verifies that the acting principal belongs to the classroom as either a 
     * teacher or a pupil.
     *
     * @param classroomCode the code of the classroom
     * @param principal     the authenticated user principal
     * @return a {@link ClassroomResponse} containing the classroom details
     * @throws AccessDeniedException   if the principal does not belong to the classroom
     * @throws EntityNotFoundException if the classroom is not found
     */
    @Transactional(readOnly = true)
    public ClassroomResponse getClassroom(String classroomCode, JwtUserPrincipal principal) {
        if (principal == null || principal.email() == null) {
            throw new AccessDeniedException("Authentication required");
        }

        Classroom classroom = getRequiredClassroom(classroomCode);

        boolean isTeacher = bridgeRepository
                .existsByIdAccountEmailAndIdClassroomCode(principal.email(), classroomCode);
        boolean isPupil = pupilClassroomMembershipRepository
                .existsByAccountEmailAndClassroomClassroomCode(principal.email(), classroomCode);

        if (!isTeacher && !isPupil) {
            throw new AccessDeniedException("You do not belong to this classroom");
        }

        return new ClassroomResponse(
                classroom.getClassroomCode(),
                classroom.getClassroomName(),
                classroom.getClassroomDescription());
    }

    /**
     * Retrieves all teachers who have ownership of a classroom.
     * 
     * <p>Results are sorted by legal name and email.
     *
     * @param classroomCode the code of the classroom
     * @param principal     the authenticated teacher principal
     * @return a list of {@link ClassroomTeacherResponse} objects
     * @throws AccessDeniedException   if the principal is not a teacher or not an owner
     * @throws EntityNotFoundException if the classroom is not found
     */
    @Transactional(readOnly = true)
    public List<ClassroomTeacherResponse> getClassroomTeachers(String classroomCode, JwtUserPrincipal principal) {
        verifyTeacherPrincipal(principal);
        getRequiredClassroom(classroomCode);
        verifyTeacherOwnership(principal.email(), classroomCode);

        return bridgeRepository.findAllByIdClassroomCode(classroomCode).stream()
                .map(AccountClassroomBridgeTeacher::getAccount)
                .sorted(Comparator.comparing(
                                Account::getLegalName,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(Account::getEmail))
                .map(account -> new ClassroomTeacherResponse(account.getEmail(), account.getLegalName()))
                .toList();
    }

    /**
     * Retrieves all pupils who are members of a classroom and have been confirmed by a teacher.
     * 
     * <p>Results are sorted by display username and email.
     *
     * @param classroomCode the code of the classroom
     * @param principal     the authenticated teacher principal
     * @return a list of {@link AcceptedClassroomPupilResponse} objects including pupil XP
     * @throws AccessDeniedException   if the principal is not a teacher or not an owner
     * @throws EntityNotFoundException if the classroom is not found
     */
    @Transactional(readOnly = true)
    public List<AcceptedClassroomPupilResponse> getAcceptedPupils(String classroomCode, JwtUserPrincipal principal) {
        verifyTeacherPrincipal(principal);
        getRequiredClassroom(classroomCode);
        verifyTeacherOwnership(principal.email(), classroomCode);

        return pupilClassroomMembershipRepository
                .findAllByClassroomClassroomCodeAndMembershipConfirmedByTeacherTrue(classroomCode)
                .stream()
                .sorted(Comparator.comparing(
                                PupilClassroomMembership::getUsername,
                                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(PupilClassroomMembership::getAccountEmail))
                .map(this::toAcceptedClassroomPupilResponse)
                .toList();
    }

    /**
     * Confirms a pupil's join request for a classroom.
     * 
     * @param classroomCode the code of the classroom
     * @param pupilEmail    the email of the pupil to accept
     * @param principal     the authenticated teacher principal
     * @return the updated {@link AcceptedPupilMembershipResponse}
     * @throws AccessDeniedException   if the principal is not a teacher or not an owner
     * @throws EntityNotFoundException if the pending membership is not found
     */
    @Transactional
    public AcceptedPupilMembershipResponse acceptPupilRequest(String classroomCode,
                                                              String pupilEmail,
                                                              JwtUserPrincipal principal) {
        verifyTeacherPrincipal(principal);
        getRequiredClassroom(classroomCode);
        verifyTeacherOwnership(principal.email(), classroomCode);

        PupilClassroomMembership membership = pupilClassroomMembershipRepository
                .findByAccountEmailAndClassroomClassroomCodeAndMembershipConfirmedByTeacherFalse(
                        normalizeEmail(pupilEmail),
                        classroomCode)
                .orElseThrow(() -> new EntityNotFoundException("Pending pupil membership not found"));

        membership.setMembershipConfirmedByTeacher(true);
        return toAcceptedPupilMembershipResponse(pupilClassroomMembershipRepository.save(membership));
    }

    /**
     * Removes a pupil's membership from a classroom.
     * 
     * <p>This can be used to reject a pending request or remove an already confirmed member.
     *
     * @param classroomCode the code of the classroom
     * @param pupilEmail    the email of the pupil to kick
     * @param principal     the authenticated teacher principal
     * @throws AccessDeniedException   if the principal is not a teacher or not an owner
     * @throws EntityNotFoundException if the membership is not found
     */
    @Transactional
    public void kickPupil(String classroomCode, String pupilEmail, JwtUserPrincipal principal) {
        verifyTeacherPrincipal(principal);
        getRequiredClassroom(classroomCode);
        verifyTeacherOwnership(principal.email(), classroomCode);

        PupilClassroomMembership membership = pupilClassroomMembershipRepository
                .findByAccountEmailAndClassroomClassroomCode(normalizeEmail(pupilEmail), classroomCode)
                .orElseThrow(() -> new EntityNotFoundException("Pupil membership not found"));

        pupilClassroomMembershipRepository.delete(membership);
    }

    /**
     * Updates the display username of a confirmed pupil in a classroom.
     * 
     * @param classroomCode the code of the classroom
     * @param pupilEmail    the email of the pupil
     * @param request       the new username
     * @param principal     the authenticated teacher principal
     * @return the updated {@link AcceptedClassroomPupilResponse}
     * @throws AccessDeniedException   if the principal is not a teacher or not an owner
     * @throws EntityNotFoundException if the confirmed membership is not found
     */
    @Transactional
    public AcceptedClassroomPupilResponse updatePupilUsername(String classroomCode,
                                                             String pupilEmail,
                                                             UpdatePupilUsernameRequest request,
                                                             JwtUserPrincipal principal) {
        verifyTeacherPrincipal(principal);
        getRequiredClassroom(classroomCode);
        verifyTeacherOwnership(principal.email(), classroomCode);

        PupilClassroomMembership membership = pupilClassroomMembershipRepository
                .findByAccountEmailAndClassroomClassroomCodeAndMembershipConfirmedByTeacherTrue(
                        normalizeEmail(pupilEmail),
                        classroomCode)
                .orElseThrow(() -> new EntityNotFoundException("Confirmed pupil membership not found"));

        membership.setUsername(normalizeUsername(request.username()));
        return toAcceptedClassroomPupilResponse(pupilClassroomMembershipRepository.save(membership));
    }

    private void verifyTeacherPrincipal(JwtUserPrincipal principal) {
        if (principal == null || principal.email() == null || principal.role() == null) {
            throw new AccessDeniedException("Teacher access is required");
        }

        if (!AccountRole.TEACHER.name().equals(principal.role())) {
            throw new AccessDeniedException("Teacher access is required");
        }
    }

    private void verifyPupilPrincipal(JwtUserPrincipal principal) {
        if (principal == null || principal.email() == null || principal.role() == null) {
            throw new AccessDeniedException("Pupil access is required");
        }

        if (!AccountRole.PUPIL.name().equals(principal.role())) {
            throw new AccessDeniedException("Pupil access is required");
        }
    }

    private Account getRequiredAccount(String email) {
        return accountRepository.findById(email)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));
    }

    private Classroom getRequiredClassroom(String classroomCode) {
        return classroomRepository.findById(classroomCode)
                .orElseThrow(() -> new EntityNotFoundException("Classroom not found"));
    }

    private void verifyTeacherOwnership(String email, String classroomCode) {
        if (!bridgeRepository.existsByIdAccountEmailAndIdClassroomCode(email, classroomCode)) {
            throw new AccessDeniedException("You do not have access to manage this classroom");
        }
    }

    private boolean canTeacherLeave(String classroomCode) {
        return bridgeRepository.countByIdClassroomCode(classroomCode) > 1;
    }

    private String normalizeName(String name) {
        return name.trim();
    }

    private String normalizeDescription(String description) {
        if (description == null) {
            return null;
        }

        String trimmedDescription = description.trim();
        return trimmedDescription.isEmpty() ? null : trimmedDescription;
    }

    private String normalizeEmail(String email) {
        return email.trim();
    }

    private String normalizeUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username is required");
        }

        return username.trim();
    }

    private ClassroomResponse toResponse(Classroom classroom) {
        return new ClassroomResponse(
                classroom.getClassroomCode(),
                classroom.getClassroomName(),
                classroom.getClassroomDescription());
    }

    private PendingPupilRequestResponse toPendingPupilRequestResponse(PupilClassroomMembership membership) {
        FileObject avatarFileObject = membership.getAvatarFileObject();

        return new PendingPupilRequestResponse(
                membership.getAccountEmail(),
                membership.getAccount().getLegalName(),
                membership.getUsername(),
                avatarFileObject != null ? avatarFileObject.getObjectKey() : null);
    }

    private AcceptedPupilMembershipResponse toAcceptedPupilMembershipResponse(PupilClassroomMembership membership) {
        return new AcceptedPupilMembershipResponse(
                membership.getAccountEmail(),
                membership.getAccount().getLegalName(),
                membership.isMembershipConfirmedByTeacher());
    }

    private AcceptedClassroomPupilResponse toAcceptedClassroomPupilResponse(PupilClassroomMembership membership) {
        FileObject avatarFileObject = membership.getAvatarFileObject();

        return new AcceptedClassroomPupilResponse(
                membership.getAccountEmail(),
                membership.getAccount().getLegalName(),
                membership.getUsername(),
                avatarFileObject != null ? avatarFileObject.getObjectKey() : null,
                pupilXpService.calculateMembershipXp(membership));
    }
}
