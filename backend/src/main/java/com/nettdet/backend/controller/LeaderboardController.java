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
import com.nettdet.backend.security.JwtUserPrincipal;
import com.nettdet.backend.service.LeaderboardService;

@RestController
@RequestMapping
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/leaderboards/classrooms")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<ClassroomLeaderboardEntryResponse>> getGlobalClassroomLeaderboard() {
        return ResponseEntity.ok(leaderboardService.getGlobalClassroomLeaderboard());
    }

    @GetMapping("/classrooms/{classroomCode}/leaderboard")
    @PreAuthorize("hasAuthority('TEACHER')")
    public ResponseEntity<ClassroomPupilLeaderboardResponse> getClassroomLeaderboard(
            @PathVariable String classroomCode,
            @AuthenticationPrincipal JwtUserPrincipal principal) {
        return ResponseEntity.ok(leaderboardService.getClassroomLeaderboard(classroomCode, principal));
    }
}
