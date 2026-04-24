package com.nettdet.backend.service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.persistence.EntityNotFoundException;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nettdet.backend.domain.Classroom;
import com.nettdet.backend.domain.PupilChapterBridgeProgression;
import com.nettdet.backend.domain.PupilClassroomMembership;
import com.nettdet.backend.domain.enums.AccountRole;
import com.nettdet.backend.dto.ClassroomLeaderboardEntryResponse;
import com.nettdet.backend.dto.ClassroomPupilLeaderboardEntryResponse;
import com.nettdet.backend.dto.ClassroomPupilLeaderboardResponse;
import com.nettdet.backend.dto.TeacherClassroomOverviewResponse;
import com.nettdet.backend.repository.AccountClassroomBridgeTeacherRepository;
import com.nettdet.backend.repository.ClassroomRepository;
import com.nettdet.backend.repository.PupilClassroomMembershipRepository;
import com.nettdet.backend.security.JwtUserPrincipal;

@Service
public class LeaderboardService {

    private static final int XP_PER_HIGHSCORE_POINT = 30;
    private static final int XP_PER_COMPLETED_CHAPTER = 30;

    private final ClassroomRepository classroomRepository;
    private final PupilClassroomMembershipRepository pupilClassroomMembershipRepository;
    private final AccountClassroomBridgeTeacherRepository bridgeRepository;

    public LeaderboardService(ClassroomRepository classroomRepository,
                              PupilClassroomMembershipRepository pupilClassroomMembershipRepository,
                              AccountClassroomBridgeTeacherRepository bridgeRepository) {
        this.classroomRepository = classroomRepository;
        this.pupilClassroomMembershipRepository = pupilClassroomMembershipRepository;
        this.bridgeRepository = bridgeRepository;
    }

    @Transactional(readOnly = true)
    public List<ClassroomLeaderboardEntryResponse> getGlobalClassroomLeaderboard() {
        return buildRankedClassroomScores().stream()
                .map(score -> new ClassroomLeaderboardEntryResponse(
                        score.classroomCode(),
                        score.classroomName(),
                        score.totalScore()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<TeacherClassroomOverviewResponse> getTeacherClassrooms(JwtUserPrincipal principal) {
        verifyTeacherPrincipal(principal);

        List<String> teacherClassroomCodes = bridgeRepository.findAllByIdAccountEmail(principal.email()).stream()
                .map(bridge -> bridge.getId().getClassroomCode())
                .toList();

        if (teacherClassroomCodes.isEmpty()) {
            return List.of();
        }

        return buildRankedClassroomScores().stream()
                .filter(score -> teacherClassroomCodes.contains(score.classroomCode()))
                .map(score -> new TeacherClassroomOverviewResponse(
                        score.classroomCode(),
                        score.classroomName(),
                        score.totalScore(),
                        score.globalRank()))
                .toList();
    }

    private List<RankedClassroomScore> buildRankedClassroomScores() {
        Map<String, ClassroomScore> scoresByClassroom = new LinkedHashMap<>();
        for (Classroom classroom : classroomRepository.findAll()) {
            scoresByClassroom.put(
                    classroom.getClassroomCode(),
                    new ClassroomScore(classroom.getClassroomCode(), classroom.getClassroomName(), 0L));
        }

        for (PupilClassroomMembership membership : pupilClassroomMembershipRepository.findAllByMembershipConfirmedByTeacherTrue()) {
            if (!membership.isMembershipConfirmedByTeacher()) {
                continue;
            }

            Classroom classroom = membership.getClassroom();
            if (classroom == null) {
                continue;
            }

            ClassroomScore score = scoresByClassroom.get(classroom.getClassroomCode());
            if (score != null) {
                score.totalXp += calculateMembershipXp(membership);
            }
        }

        List<ClassroomScore> sortedScores = scoresByClassroom.values().stream()
                .sorted(Comparator.comparingLong(ClassroomScore::totalXp).reversed()
                        .thenComparing(ClassroomScore::classroomName, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(ClassroomScore::classroomCode))
                .toList();

        List<RankedClassroomScore> rankedScores = new ArrayList<>();
        long previousScore = Long.MIN_VALUE;
        int currentRank = 0;

        for (int index = 0; index < sortedScores.size(); index++) {
            ClassroomScore score = sortedScores.get(index);
            if (score.totalXp() != previousScore) {
                currentRank = index + 1;
                previousScore = score.totalXp();
            }

            rankedScores.add(new RankedClassroomScore(
                    score.classroomCode(),
                    score.classroomName(),
                    score.totalXp(),
                    currentRank));
        }

        return rankedScores;
    }

    @Transactional(readOnly = true)
    public ClassroomPupilLeaderboardResponse getClassroomLeaderboard(String classroomCode, JwtUserPrincipal principal) {
        verifyTeacherPrincipal(principal);

        Classroom classroom = classroomRepository.findById(classroomCode)
                .orElseThrow(() -> new EntityNotFoundException("Classroom not found"));
        verifyTeacherOwnership(principal.email(), classroomCode);

        List<PupilScore> pupilScores = new ArrayList<>();
        for (PupilClassroomMembership membership :
                pupilClassroomMembershipRepository.findAllByClassroomClassroomCodeAndMembershipConfirmedByTeacherTrue(classroomCode)) {
            if (!membership.isMembershipConfirmedByTeacher()) {
                continue;
            }

            pupilScores.add(new PupilScore(
                    membership.getAccountEmail(),
                    membership.getUsername(),
                    calculateMembershipXp(membership)));
        }

        List<ClassroomPupilLeaderboardEntryResponse> entries = pupilScores.stream()
                .sorted(Comparator.comparingLong(PupilScore::totalXp).reversed()
                        .thenComparing(PupilScore::username, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                        .thenComparing(PupilScore::accountEmail))
                .map(score -> new ClassroomPupilLeaderboardEntryResponse(score.username(), score.totalXp()))
                .toList();

        return new ClassroomPupilLeaderboardResponse(
                classroom.getClassroomCode(),
                classroom.getClassroomName(),
                classroom.getClassroomDescription(),
                entries);
    }

    private void verifyTeacherPrincipal(JwtUserPrincipal principal) {
        if (principal == null || principal.email() == null || principal.role() == null) {
            throw new AccessDeniedException("Teacher access is required");
        }

        if (!AccountRole.TEACHER.name().equals(principal.role())) {
            throw new AccessDeniedException("Teacher access is required");
        }
    }

    private void verifyTeacherOwnership(String email, String classroomCode) {
        if (!bridgeRepository.existsByIdAccountEmailAndIdClassroomCode(email, classroomCode)) {
            throw new AccessDeniedException("You do not have access to manage this classroom");
        }
    }

    private long calculateMembershipXp(PupilClassroomMembership membership) {
        long totalXp = 0L;

        for (PupilChapterBridgeProgression progression : membership.getChapterProgressions()) {
            int highscore = progression.getHighscore() == null ? 0 : progression.getHighscore();
            int successfulRuns = progression.getSuccessfulRuns() == null ? 0 : progression.getSuccessfulRuns();

            totalXp += (long) highscore * XP_PER_HIGHSCORE_POINT;
            if (successfulRuns >= 1) {
                totalXp += XP_PER_COMPLETED_CHAPTER;
            }
        }

        return totalXp;
    }

    private record PupilScore(String accountEmail, String username, long totalXp) {
    }

    private static final class ClassroomScore {
        private final String classroomCode;
        private final String classroomName;
        private long totalXp;

        private ClassroomScore(String classroomCode, String classroomName, long totalXp) {
            this.classroomCode = classroomCode;
            this.classroomName = classroomName;
            this.totalXp = totalXp;
        }

        private String classroomCode() {
            return classroomCode;
        }

        private String classroomName() {
            return classroomName;
        }

        private long totalXp() {
            return totalXp;
        }
    }

    private record RankedClassroomScore(
            String classroomCode,
            String classroomName,
            long totalScore,
            int globalRank) {
    }
}
