package com.nettdet.backend.service;

import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nettdet.backend.domain.Account;
import com.nettdet.backend.domain.Classroom;
import com.nettdet.backend.domain.FileObject;
import com.nettdet.backend.domain.PupilClassroomMembership;
import com.nettdet.backend.dto.PupilClassroomMembershipResponse;
import com.nettdet.backend.dto.PupilProgressionResponse;
import com.nettdet.backend.dto.PupilRequest;
import com.nettdet.backend.dto.PupilResponse;
import com.nettdet.backend.repository.AccountRepository;
import com.nettdet.backend.repository.ClassroomRepository;
import com.nettdet.backend.repository.PupilClassroomMembershipRepository;

import jakarta.persistence.EntityNotFoundException;

@Service
public class PupilService {

    private static final Logger logger = LoggerFactory.getLogger(PupilService.class);

    private final PupilClassroomMembershipRepository pupilMembershipRepository;
    private final AccountRepository accountRepository;
    private final ClassroomRepository classroomRepository;
    private final AvatarService avatarService;

    public PupilService(PupilClassroomMembershipRepository pupilMembershipRepository,
                        AccountRepository accountRepository,
                        ClassroomRepository classroomRepository,
                        AvatarService avatarService) {
        this.pupilMembershipRepository = pupilMembershipRepository;
        this.accountRepository = accountRepository;
        this.classroomRepository = classroomRepository;
        this.avatarService = avatarService;
    }

    // 1. method for retrieving a pupils information (email, username)
    public PupilResponse getPupilInformation(String email, String classroomCode) {
        return pupilMembershipRepository
               .findByAccountEmailAndClassroomClassroomCode(email, classroomCode)
               .map(PupilResponse::from)
               .orElseThrow(() -> {
                   logger.warn("Pupil membership not found for email {} in classroom {}", email, classroomCode);
                   return new EntityNotFoundException("Pupil membership not found");
               });
    }

    // 2. method for retrieving a pupils avatar

    // 3. method for retrieving a pupils progression (per chapter)
    public PupilProgressionResponse getPupilProgressionPerChapter(String email, String classroomCode) {
        return pupilMembershipRepository
               .findByAccountEmailAndClassroomClassroomCode(email, classroomCode)
               .map(PupilProgressionResponse::from)
               .orElseThrow(() -> {
                   logger.warn("Pupil progression not found for email {} in classroom {}", email, classroomCode);
                   return new EntityNotFoundException("Pupil membership not found");
               });
    }


    // 4. method for checking up against database to confirm a pupils membership to the requested class
    public boolean checkPupilMembershipForClass(PupilRequest pupilRequest) {
        return pupilMembershipRepository
               .findByAccountEmailAndClassroomClassroomCode(pupilRequest.email(), pupilRequest.classroomCode())
               .map(PupilClassroomMembership::isMembershipConfirmedByTeacher)
               .orElse(false);
    }

    // 5. method for requesting to join a classroom
    @Transactional
    public PupilResponse requestToJoinClassroom(PupilRequest pupilRequest) {
        Account account = accountRepository.findById(pupilRequest.email())
               .orElseThrow(() -> {
                   logger.warn("Join request failed: account not found for email {}", pupilRequest.email());
                   return new EntityNotFoundException("Account not found");
               });

        Classroom classroom = classroomRepository.findById(pupilRequest.classroomCode())
               .orElseThrow(() -> {
                   logger.warn("Join request failed: classroom not found for code {}", pupilRequest.classroomCode());
                   return new EntityNotFoundException("Classroom not found");
               });

        Optional<PupilClassroomMembership> existing = pupilMembershipRepository
               .findByAccountEmailAndClassroomClassroomCode(pupilRequest.email(), pupilRequest.classroomCode());
        if (existing.isPresent()) {
            logger.info("Join request: membership already exists for {} in {}", pupilRequest.email(), pupilRequest.classroomCode());
            return PupilResponse.from(existing.get());
        }

        PupilClassroomMembership membership = new PupilClassroomMembership();
        membership.setAccount(account);
        membership.setClassroom(classroom);
        membership.setMembershipConfirmedByTeacher(false);

        PupilClassroomMembership saved = pupilMembershipRepository.save(membership);
        logger.info("Pupil {} requested to join classroom {}", pupilRequest.email(), pupilRequest.classroomCode());
        return PupilResponse.from(saved);
    }

    @Transactional
    public PupilClassroomMembershipResponse updateClassroomMembership(
            String email,
            String classroomCode,
            String username,
            String avatar) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("Username must not be blank");
        }

        PupilClassroomMembership membership = pupilMembershipRepository
                .findByAccountEmailAndClassroomClassroomCode(email, classroomCode)
                .orElseThrow(() -> {
                    logger.warn("Pupil membership not found for email {} in classroom {}", email, classroomCode);
                    return new EntityNotFoundException("Pupil membership not found");
                });

        membership.setUsername(username.trim());
        FileObject avatarFileObject = avatarService.getOrCreateAvatar(avatar);
        membership.setAvatarFileObject(avatarFileObject);

        PupilClassroomMembership savedMembership = pupilMembershipRepository.save(membership);
        return PupilClassroomMembershipResponse.from(savedMembership);
    }
}
