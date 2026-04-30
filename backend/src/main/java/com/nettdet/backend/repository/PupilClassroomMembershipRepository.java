package com.nettdet.backend.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.nettdet.backend.domain.PupilClassroomMembership;

/**
 * Repository interface for managing {@link PupilClassroomMembership} entities.
 * 
 * <p>Provides a wide range of methods to manage pupil affiliations with classrooms.
 * Includes support for checking confirmation status, retrieving memberships with 
 * optimized fetching (EntityGraphs), and custom queries for specific identifiers.
 */
public interface PupilClassroomMembershipRepository extends JpaRepository<PupilClassroomMembership, String> {

    /**
     * Checks if a membership exists for a specific account and classroom.
     *
     * @param accountEmail  the email of the account
     * @param classroomCode the unique code of the classroom
     * @return true if the membership exists, false otherwise
     */
    boolean existsByAccountEmailAndClassroomClassroomCode(String accountEmail, String classroomCode);

    /**
     * Finds a pupil's membership by their account email.
     * Uses an {@link EntityGraph} to eagerly load classroom and avatar data.
     *
     * @param accountEmail the email of the account
     * @return an {@link Optional} containing the membership if found
     */
    @EntityGraph(attributePaths = {"classroom", "avatarFileObject"})
    Optional<PupilClassroomMembership> findByAccountEmail(String accountEmail);

    /**
     * Finds a membership by account email and classroom code.
     *
     * @param accountEmail  the email of the account
     * @param classroomCode the unique code of the classroom
     * @return an {@link Optional} containing the membership if found
     */
    Optional<PupilClassroomMembership> findByAccountEmailAndClassroomClassroomCode(String accountEmail, String classroomCode);

    /**
     * Finds a membership that has been confirmed by a teacher.
     *
     * @param accountEmail the email of the account
     * @return an {@link Optional} containing the confirmed membership if found
     */
    Optional<PupilClassroomMembership> findByAccountEmailAndMembershipConfirmedByTeacherTrue(String accountEmail);

    /**
     * Finds a confirmed membership for a specific classroom.
     *
     * @param accountEmail  the email of the account
     * @param classroomCode the unique code of the classroom
     * @return an {@link Optional} containing the confirmed membership if found
     */
    Optional<PupilClassroomMembership> findByAccountEmailAndClassroomClassroomCodeAndMembershipConfirmedByTeacherTrue(
            String accountEmail,
            String classroomCode);

    /**
     * Finds a membership that is pending teacher confirmation.
     * Uses an {@link EntityGraph} to load the associated account.
     *
     * @param accountEmail  the email of the account
     * @param classroomCode the unique code of the classroom
     * @return an {@link Optional} containing the unconfirmed membership if found
     */
    @EntityGraph(attributePaths = "account")
    Optional<PupilClassroomMembership> findByAccountEmailAndClassroomClassroomCodeAndMembershipConfirmedByTeacherFalse(
            String accountEmail,
            String classroomCode);

    /**
     * Retrieves all confirmed memberships across all classrooms.
     * Uses an {@link EntityGraph} to load accounts and chapter progressions.
     *
     * @return a list of all confirmed pupil memberships
     */
    @EntityGraph(attributePaths = {"account", "chapterProgressions"})
    List<PupilClassroomMembership> findAllByMembershipConfirmedByTeacherTrue();

    /**
     * Retrieves all confirmed memberships for a specific classroom.
     * Uses an {@link EntityGraph} to load accounts, avatars, and chapter progressions.
     *
     * @param classroomCode the unique code of the classroom
     * @return a list of confirmed pupil memberships in the classroom
     */
    @EntityGraph(attributePaths = {"account", "avatarFileObject", "chapterProgressions"})
    List<PupilClassroomMembership> findAllByClassroomClassroomCodeAndMembershipConfirmedByTeacherTrue(String classroomCode);

    /**
     * Retrieves all unconfirmed (pending) memberships for a specific classroom.
     * Uses an {@link EntityGraph} to load accounts and avatars.
     *
     * @param classroomCode the unique code of the classroom
     * @return a list of pending pupil memberships in the classroom
     */
    @EntityGraph(attributePaths = {"account", "avatarFileObject"})
    List<PupilClassroomMembership> findAllByClassroomClassroomCodeAndMembershipConfirmedByTeacherFalse(String classroomCode);

    /**
     * Custom query to retrieve the classroom code for a confirmed pupil.
     *
     * @param email the email of the account
     * @return an {@link Optional} containing the classroom code if found and confirmed
     */
    @Query("SELECT m.classroom.classroomCode FROM PupilClassroomMembership m " +
           "WHERE m.account.email = :email AND m.membershipConfirmedByTeacher = true")
    Optional<String> findConfirmedClassroomCodeByAccountEmail(@Param("email") String email);
}
