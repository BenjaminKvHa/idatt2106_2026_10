package com.nettdet.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nettdet.backend.domain.WeeklyMystery;

public interface WeeklyMysteryRepository extends JpaRepository<WeeklyMystery, Long> {

    @EntityGraph(attributePaths = {"fileObject", "classroom", "pupilClassroomMembership", "pupilClassroomMembership.account"})
    List<WeeklyMystery> findAllByClassroomClassroomCodeAndApprovedFalseOrderByCreatedAtDesc(String classroomCode);

    @EntityGraph(attributePaths = {"fileObject", "classroom"})
    List<WeeklyMystery> findAllByClassroomClassroomCodeAndApprovedTrueOrderByCreatedAtDesc(String classroomCode);

    @EntityGraph(attributePaths = {"fileObject", "classroom", "pupilClassroomMembership", "pupilClassroomMembership.account"})
    Optional<WeeklyMystery> findByIdAndClassroomClassroomCode(Long id, String classroomCode);

    boolean existsByPupilClassroomMembershipAccountEmailAndApprovedTrue(String accountEmail);
}
