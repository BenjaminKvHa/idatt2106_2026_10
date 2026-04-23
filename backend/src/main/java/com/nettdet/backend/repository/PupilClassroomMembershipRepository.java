package com.nettdet.backend.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nettdet.backend.domain.PupilClassroomMembership;

public interface PupilClassroomMembershipRepository extends JpaRepository<PupilClassroomMembership, String> {

    boolean existsByAccountEmailAndClassroomClassroomCode(String accountEmail, String classroomCode);
    @EntityGraph(attributePaths = {"classroom", "avatarFileObject"})
    Optional<PupilClassroomMembership> findByAccountEmail(String accountEmail);
    Optional<PupilClassroomMembership> findByAccountEmailAndClassroomClassroomCode(String accountEmail, String classroomCode);
    Optional<PupilClassroomMembership> findByAccountEmailAndMembershipConfirmedByTeacherTrue(String accountEmail);
    Optional<PupilClassroomMembership> findByAccountEmailAndClassroomClassroomCodeAndMembershipConfirmedByTeacherTrue(
            String accountEmail,
            String classroomCode);

    @EntityGraph(attributePaths = "account")
    Optional<PupilClassroomMembership> findByAccountEmailAndClassroomClassroomCodeAndMembershipConfirmedByTeacherFalse(
            String accountEmail,
            String classroomCode);

    @EntityGraph(attributePaths = {"account", "chapterProgressions"})
    List<PupilClassroomMembership> findAllByMembershipConfirmedByTeacherTrue();

    @EntityGraph(attributePaths = {"account", "avatarFileObject", "chapterProgressions"})
    List<PupilClassroomMembership> findAllByClassroomClassroomCodeAndMembershipConfirmedByTeacherTrue(String classroomCode);

    @EntityGraph(attributePaths = "account")
    List<PupilClassroomMembership> findAllByClassroomClassroomCodeAndMembershipConfirmedByTeacherFalse(String classroomCode);

    @Query("SELECT m.classroom.classroomCode FROM PupilClassroomMembership m " +
           "WHERE m.account.email = :email AND m.membershipConfirmedByTeacher = true")
    Optional<String> findConfirmedClassroomCodeByAccountEmail(@Param("email") String email);
}
