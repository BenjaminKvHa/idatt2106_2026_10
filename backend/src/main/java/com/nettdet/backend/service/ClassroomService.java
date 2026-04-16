package com.nettdet.backend.service;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nettdet.backend.domain.AccountClassroomBridgeTeacher;
import com.nettdet.backend.domain.AccountClassroomBridgeTeacherId;
import com.nettdet.backend.domain.Classroom;
import com.nettdet.backend.domain.enums.AccountRole;
import com.nettdet.backend.dto.ClassroomResponse;
import com.nettdet.backend.dto.CreateClassroomRequest;
import com.nettdet.backend.dto.UpdateClassroomRequest;
import com.nettdet.backend.repository.AccountClassroomBridgeTeacherRepository;
import com.nettdet.backend.repository.AccountRepository;
import com.nettdet.backend.repository.ClassroomRepository;
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.util.ClassroomCodeGenerator;

@Service
public class ClassroomService {

    private static final int MAX_CODE_GENERATION_ATTEMPTS = 10;

    private final ClassroomRepository classroomRepository;
    private final AccountClassroomBridgeTeacherRepository bridgeRepository;
    private final AccountRepository accountRepository;
    private final ClassroomCodeGenerator classroomCodeGenerator;

    public ClassroomService(ClassroomRepository classroomRepository,
                            AccountClassroomBridgeTeacherRepository bridgeRepository,
                            AccountRepository accountRepository,
                            ClassroomCodeGenerator classroomCodeGenerator) {
        this.classroomRepository = classroomRepository;
        this.bridgeRepository = bridgeRepository;
        this.accountRepository = accountRepository;
        this.classroomCodeGenerator = classroomCodeGenerator;
    }

    @Transactional
    public ClassroomResponse createClassroom(CreateClassroomRequest request, JwtUserPrincipal principal) {
        verifyTeacherPrincipal(principal);
        ensureAccountExists(principal.email());

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
                bridgeRepository.save(bridge);

                return toResponse(savedClassroom);
            } catch (DataIntegrityViolationException exception) {
                // Retry on the rare chance that the generated classroom code collides concurrently.
            }
        }

        throw new IllegalStateException("Unable to generate a unique classroom code");
    }

    @Transactional
    public ClassroomResponse updateClassroom(String classroomCode,
                                             UpdateClassroomRequest request,
                                             JwtUserPrincipal principal) {
        verifyTeacherPrincipal(principal);

        Classroom classroom = classroomRepository.findById(classroomCode)
                .orElseThrow(() -> new EntityNotFoundException("Classroom not found"));

        verifyTeacherOwnership(principal.email(), classroomCode);

        classroom.setClassroomName(normalizeName(request.name()));
        classroom.setClassroomDescription(normalizeDescription(request.description()));

        return toResponse(classroomRepository.save(classroom));
    }

    @Transactional
    public void deleteClassroom(String classroomCode, JwtUserPrincipal principal) {
        verifyTeacherPrincipal(principal);

        Classroom classroom = classroomRepository.findById(classroomCode)
                .orElseThrow(() -> new EntityNotFoundException("Classroom not found"));

        verifyTeacherOwnership(principal.email(), classroomCode);
        classroomRepository.delete(classroom);
    }

    private void verifyTeacherPrincipal(JwtUserPrincipal principal) {
        if (principal == null || principal.email() == null || principal.role() == null) {
            throw new AccessDeniedException("Teacher access is required");
        }

        if (!AccountRole.TEACHER.name().equals(principal.role())) {
            throw new AccessDeniedException("Teacher access is required");
        }
    }

    private void ensureAccountExists(String email) {
        if (!accountRepository.existsById(email)) {
            throw new EntityNotFoundException("Account not found");
        }
    }

    private void verifyTeacherOwnership(String email, String classroomCode) {
        if (!bridgeRepository.existsByIdAccountEmailAndIdClassroomCode(email, classroomCode)) {
            throw new AccessDeniedException("You do not have access to manage this classroom");
        }
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

    private ClassroomResponse toResponse(Classroom classroom) {
        return new ClassroomResponse(
                classroom.getClassroomCode(),
                classroom.getClassroomName(),
                classroom.getClassroomDescription());
    }
}
