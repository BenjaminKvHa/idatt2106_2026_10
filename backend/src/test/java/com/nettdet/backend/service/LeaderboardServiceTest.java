package com.nettdet.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import jakarta.persistence.EntityNotFoundException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.nettdet.backend.domain.Account;
import com.nettdet.backend.domain.AccountClassroomBridgeTeacher;
import com.nettdet.backend.domain.Classroom;
import com.nettdet.backend.domain.PupilChapterBridgeProgression;
import com.nettdet.backend.domain.PupilClassroomMembership;
import com.nettdet.backend.domain.enums.AccountRole;
import com.nettdet.backend.domain.ids.AccountClassroomBridgeTeacherId;
import com.nettdet.backend.dto.ClassroomLeaderboardEntryResponse;
import com.nettdet.backend.dto.ClassroomPupilLeaderboardResponse;
import com.nettdet.backend.dto.TeacherClassroomOverviewResponse;
import com.nettdet.backend.repository.AccountClassroomBridgeTeacherRepository;
import com.nettdet.backend.repository.ClassroomRepository;
import com.nettdet.backend.repository.PupilClassroomMembershipRepository;
import com.nettdet.backend.security.JwtUserPrincipal;

@ExtendWith(MockitoExtension.class)
class LeaderboardServiceTest {

    private static final String TEACHER_EMAIL = "teacher@example.com";

    @Mock
    private ClassroomRepository classroomRepository;

    @Mock
    private PupilClassroomMembershipRepository membershipRepository;

    @Mock
    private AccountClassroomBridgeTeacherRepository bridgeRepository;

    @InjectMocks
    private LeaderboardService leaderboardService;

    @DisplayName("Builds global classroom leaderboard from confirmed pupil chapter progress")
    @Test
    void getGlobalClassroomLeaderboard_whenClassroomsHaveScores_returnsSortedClassroomScores() {
        Classroom alpha = classroom("AAAAAA", "Alpha");
        Classroom beta = classroom("BBBBBB", "Beta");
        Classroom gamma = classroom("CCCCCC", "Gamma");
        PupilClassroomMembership alphaPupil = membership("alpha@example.com", "alpha", alpha, true,
                progression(3, 1),
                progression(1, 0));
        PupilClassroomMembership betaPupil = membership("beta@example.com", "beta", beta, true,
                progression(4, 0));
        PupilClassroomMembership ignoredUnconfirmed = membership("ignored@example.com", "ignored", gamma, false,
                progression(5, 1));
        when(classroomRepository.findAll()).thenReturn(List.of(gamma, beta, alpha));
        when(membershipRepository.findAllByMembershipConfirmedByTeacherTrue())
                .thenReturn(List.of(alphaPupil, betaPupil, ignoredUnconfirmed));

        List<ClassroomLeaderboardEntryResponse> leaderboard = leaderboardService.getGlobalClassroomLeaderboard();

        assertEquals(List.of("Alpha", "Beta", "Gamma"), leaderboard.stream()
                .map(ClassroomLeaderboardEntryResponse::classroomName)
                .toList());
        assertEquals(List.of(150L, 120L, 0L), leaderboard.stream()
                .map(ClassroomLeaderboardEntryResponse::totalXp)
                .toList());
    }

    @DisplayName("Returns only teacher classrooms with global shared competition ranks")
    @Test
    void getTeacherClassrooms_whenTeacherHasClassrooms_returnsOnlyTeacherClassroomsWithGlobalRanks() {
        Classroom alpha = classroom("AAAAAA", "Alpha");
        Classroom beta = classroom("BBBBBB", "Beta");
        Classroom delta = classroom("DDDDDD", "Delta");
        when(bridgeRepository.findAllByIdAccountEmail(TEACHER_EMAIL))
                .thenReturn(List.of(teacherBridge("BBBBBB"), teacherBridge("DDDDDD")));
        when(classroomRepository.findAll()).thenReturn(List.of(delta, beta, alpha));
        when(membershipRepository.findAllByMembershipConfirmedByTeacherTrue()).thenReturn(List.of(
                membership("alpha@example.com", "alpha", alpha, true, progression(3, 1)),
                membership("beta@example.com", "beta", beta, true, progression(3, 1))));

        List<TeacherClassroomOverviewResponse> overview = leaderboardService.getTeacherClassrooms(teacherPrincipal());

        assertEquals(List.of("BBBBBB", "DDDDDD"), overview.stream()
                .map(TeacherClassroomOverviewResponse::classroomCode)
                .toList());
        assertEquals(List.of(120L, 0L), overview.stream()
                .map(TeacherClassroomOverviewResponse::totalScore)
                .toList());
        assertEquals(List.of(1, 3), overview.stream()
                .map(TeacherClassroomOverviewResponse::globalRank)
                .toList());
    }

    @DisplayName("Returns empty teacher classroom overview when teacher has no classroom bridges")
    @Test
    void getTeacherClassrooms_whenTeacherHasNoClassrooms_returnsEmptyListWithoutScoreCalculation() {
        when(bridgeRepository.findAllByIdAccountEmail(TEACHER_EMAIL)).thenReturn(List.of());

        List<TeacherClassroomOverviewResponse> overview = leaderboardService.getTeacherClassrooms(teacherPrincipal());

        assertEquals(List.of(), overview);
        verifyNoInteractions(classroomRepository, membershipRepository);
    }

    @DisplayName("Rejects teacher classroom overview for non-teacher principals")
    @Test
    void getTeacherClassrooms_whenPrincipalIsNotTeacher_throwsAccessDenied() {
        JwtUserPrincipal principal = new JwtUserPrincipal("Pupil Name", "pupil@example.com", AccountRole.PUPIL.name());

        assertThrows(AccessDeniedException.class, () -> leaderboardService.getTeacherClassrooms(principal));

        verifyNoInteractions(bridgeRepository, classroomRepository, membershipRepository);
    }

    @DisplayName("Returns classroom pupil leaderboard sorted by score, username, then email")
    @Test
    void getClassroomLeaderboard_whenTeacherOwnsClassroom_returnsSortedPupilEntries() {
        Classroom classroom = classroom("ABCABC", "8A");
        PupilClassroomMembership zulu = membership("zulu@example.com", "zulu", classroom, true, progression(4, 0));
        PupilClassroomMembership alpha = membership("alpha@example.com", "alpha", classroom, true, progression(3, 1));
        PupilClassroomMembership noScore = membership("noscore@example.com", null, classroom, true, progression(null, null));
        when(classroomRepository.findById("ABCABC")).thenReturn(Optional.of(classroom));
        when(bridgeRepository.existsByIdAccountEmailAndIdClassroomCode(TEACHER_EMAIL, "ABCABC")).thenReturn(true);
        when(membershipRepository.findAllByClassroomClassroomCodeAndMembershipConfirmedByTeacherTrue("ABCABC"))
                .thenReturn(List.of(zulu, noScore, alpha));

        ClassroomPupilLeaderboardResponse response = leaderboardService.getClassroomLeaderboard(
                "ABCABC",
                teacherPrincipal());

        assertEquals("ABCABC", response.classroomCode());
        assertEquals("8A", response.classroomName());
        assertEquals("Classroom 8A description", response.classroomDescription());
        assertEquals("alpha", response.entries().get(0).username());
        assertEquals("zulu", response.entries().get(1).username());
        assertEquals(null, response.entries().get(2).username());
        assertEquals(List.of(120L, 120L, 0L), response.entries().stream()
                .map(entry -> entry.totalXp())
                .toList());
    }

    @DisplayName("Rejects classroom pupil leaderboard when teacher does not belong to classroom")
    @Test
    void getClassroomLeaderboard_whenTeacherDoesNotOwnClassroom_throwsAccessDenied() {
        when(classroomRepository.findById("ABCABC")).thenReturn(Optional.of(classroom("ABCABC", "8A")));
        when(bridgeRepository.existsByIdAccountEmailAndIdClassroomCode(TEACHER_EMAIL, "ABCABC")).thenReturn(false);

        assertThrows(
                AccessDeniedException.class,
                () -> leaderboardService.getClassroomLeaderboard("ABCABC", teacherPrincipal()));

        verify(membershipRepository, org.mockito.Mockito.never())
                .findAllByClassroomClassroomCodeAndMembershipConfirmedByTeacherTrue("ABCABC");
    }

    @DisplayName("Returns not found for classroom pupil leaderboard when classroom is missing")
    @Test
    void getClassroomLeaderboard_whenClassroomDoesNotExist_throwsEntityNotFoundException() {
        when(classroomRepository.findById("MISSING")).thenReturn(Optional.empty());

        EntityNotFoundException exception = assertThrows(
                EntityNotFoundException.class,
                () -> leaderboardService.getClassroomLeaderboard("MISSING", teacherPrincipal()));

        assertEquals("Classroom not found", exception.getMessage());
    }

    private JwtUserPrincipal teacherPrincipal() {
        return new JwtUserPrincipal("Teacher Name", TEACHER_EMAIL, AccountRole.TEACHER.name());
    }

    private Classroom classroom(String code, String name) {
        Classroom classroom = new Classroom();
        classroom.setClassroomCode(code);
        classroom.setClassroomName(name);
        classroom.setClassroomDescription("Classroom " + name + " description");
        return classroom;
    }

    private AccountClassroomBridgeTeacher teacherBridge(String classroomCode) {
        AccountClassroomBridgeTeacher bridge = new AccountClassroomBridgeTeacher();
        bridge.setId(new AccountClassroomBridgeTeacherId(TEACHER_EMAIL, classroomCode));
        return bridge;
    }

    private PupilClassroomMembership membership(String email,
                                                String username,
                                                Classroom classroom,
                                                boolean confirmed,
                                                PupilChapterBridgeProgression... progressions) {
        Account account = new Account();
        account.setEmail(email);
        account.setLegalName(username == null ? "No Username" : username);
        account.setAccountRole(AccountRole.PUPIL);
        account.setPasswordData("encoded-password");

        PupilClassroomMembership membership = new PupilClassroomMembership();
        membership.setAccountEmail(email);
        membership.setAccount(account);
        membership.setClassroom(classroom);
        membership.setUsername(username);
        membership.setMembershipConfirmedByTeacher(confirmed);
        membership.setChapterProgressions(List.of(progressions));
        return membership;
    }

    private PupilChapterBridgeProgression progression(Integer highscore, Integer successfulRuns) {
        PupilChapterBridgeProgression progression = new PupilChapterBridgeProgression();
        progression.setHighscore(highscore);
        progression.setSuccessfulRuns(successfulRuns);
        return progression;
    }
}
