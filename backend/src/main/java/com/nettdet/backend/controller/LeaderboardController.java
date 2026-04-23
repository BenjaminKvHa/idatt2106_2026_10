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

@RestController
@RequestMapping
@Tag(name = "Leaderboards", description = "Authenticated leaderboard endpoints for classroom rankings, teacher overview cards, and classroom-specific pupil rankings.")
@SecurityRequirement(name = "bearerAuth")
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/leaderboards/classrooms")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get global classroom leaderboard", description = "Returns the global classroom leaderboard for authenticated users. Entries are sorted by total XP descending.")
    public ResponseEntity<List<ClassroomLeaderboardEntryResponse>> getGlobalClassroomLeaderboard() {
        return ResponseEntity.ok(leaderboardService.getGlobalClassroomLeaderboard());
    }

    @GetMapping("/teacher/classrooms")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Get teacher classroom overview", description = "Returns the authenticated teacher's classrooms with total score and global rank. Only classrooms connected to the teacher through the teacher bridge are included.")
    public ResponseEntity<List<TeacherClassroomOverviewResponse>> getTeacherClassrooms(
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(leaderboardService.getTeacherClassrooms(principal));
    }

    @GetMapping("/classrooms/{classroomCode}/leaderboard")
    @PreAuthorize("hasAuthority('TEACHER')")
    @Operation(summary = "Get classroom pupil leaderboard", description = "Returns the pupil leaderboard for one classroom. The authenticated teacher must belong to that classroom.")
    public ResponseEntity<ClassroomPupilLeaderboardResponse> getClassroomLeaderboard(
            @PathVariable String classroomCode,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(leaderboardService.getClassroomLeaderboard(classroomCode, principal));
    }
}
