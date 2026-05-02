package com.nettdet.backend.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nettdet.backend.dto.ClassroomLeaderboardEntryResponse;
import com.nettdet.backend.dto.ClassroomPupilLeaderboardResponse;
import com.nettdet.backend.dto.TeacherClassroomOverviewResponse;
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.service.LeaderboardService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller for managing and retrieving leaderboard data.
 * <p>
 * This controller provides endpoints for various ranking views, including a global classroom
 * leaderboard, teacher-specific classroom overviews, and pupil rankings within a specific classroom.
 * It integrates with {@link LeaderboardService} to calculate scores and ranks across the system.
 */
@RestController
@RequestMapping
@Tag(name = "Leaderboards", description = "Authenticated leaderboard endpoints for classroom rankings, teacher overview cards, and classroom-specific pupil rankings.")
@SecurityRequirement(name = "bearerAuth")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    /**
     * Constructs a new LeaderboardController with the required service.
     *
     * @param leaderboardService the service for calculating leaderboard rankings and scores
     */
    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    /**
     * Retrieves the global leaderboard for all classrooms.
     * <p>
     * This endpoint returns a list of classrooms ranked by their cumulative XP. It is accessible
     * to all authenticated users, allowing pupils and teachers to see how their classrooms
     * compare against others in the system.
     *
     * @return a {@link ResponseEntity} containing a list of {@link ClassroomLeaderboardEntryResponse} sorted by XP descending
     */
    @GetMapping("/leaderboards/classrooms")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get global classroom leaderboard", description = "Returns the global classroom leaderboard for authenticated users. Entries are sorted by total XP descending.")
    public ResponseEntity<List<ClassroomLeaderboardEntryResponse>> getGlobalClassroomLeaderboard() {
        return ResponseEntity.ok(leaderboardService.getGlobalClassroomLeaderboard());
    }

    /**
     * Retrieves an overview of classrooms managed by the authenticated teacher.
     * <p>
     * Returns a summary for each classroom the teacher is associated with, including the
     * classroom's total score and its current global rank. This is used to populate the
     * teacher's dashboard overview cards.
     *
     * @param principal the authenticated teacher's principal
     * @return a {@link ResponseEntity} containing a list of {@link TeacherClassroomOverviewResponse}
     */
    @GetMapping("/teacher/classrooms")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Get teacher classroom overview", description = "Returns the authenticated teacher's classrooms with total score and global rank. Only classrooms connected to the teacher through the teacher bridge are included.")
    public ResponseEntity<List<TeacherClassroomOverviewResponse>> getTeacherClassrooms(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(leaderboardService.getTeacherClassrooms(principal));
    }

    /**
     * Retrieves the internal pupil leaderboard for a specific classroom.
     * <p>
     * Returns a ranked list of all pupils within the specified classroom based on their
     * individual XP. This endpoint is restricted to teachers who manage the classroom,
     * providing them with insight into individual pupil performance.
     *
     * @param classroomCode the unique code of the classroom
     * @param principal the authenticated teacher's principal
     * @return a {@link ResponseEntity} containing the {@link ClassroomPupilLeaderboardResponse}
     * @throws org.springframework.security.access.AccessDeniedException if the teacher does not manage the classroom
     */
    @GetMapping("/classrooms/{classroomCode}/leaderboard")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Get classroom pupil leaderboard", description = "Returns the pupil leaderboard for one classroom. The authenticated teacher must belong to that classroom.")
    public ResponseEntity<ClassroomPupilLeaderboardResponse> getClassroomLeaderboard(
            @PathVariable String classroomCode,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(leaderboardService.getClassroomLeaderboard(classroomCode, principal));
    }
}
