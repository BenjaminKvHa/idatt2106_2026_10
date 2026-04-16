package com.nettdet.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nettdet.backend.domain.PupilClassroomMembership;

public interface PupilClassroomMembershipRepository extends JpaRepository<PupilClassroomMembership, String> {

    Optional<PupilClassroomMembership> findByAccountEmailAndClassroomClassroomCode(String accountEmail, String classroomCode);
}
