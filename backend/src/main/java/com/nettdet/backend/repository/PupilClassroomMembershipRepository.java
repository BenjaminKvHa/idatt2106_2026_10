package com.nettdet.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nettdet.backend.domain.PupilClassroomMembership;

public interface PupilClassroomMembershipRepository extends JpaRepository<PupilClassroomMembership, String> {

    boolean existsByAccountEmailAndClassroomClassroomCode(String accountEmail, String classroomCode);
}
