package com.nettdet.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nettdet.backend.domain.WeeklyMystery;

/**
 * Repository interface for managing {@link WeeklyMystery} submissions.
 * 
 * <p>Provides methods for retrieving submissions categorized by their approval status,
 * ensuring teachers can efficiently review pending items and view historical approved
 * entries. Uses optimized fetching strategies (EntityGraphs) to reduce database overhead.
 */
public interface WeeklyMysteryRepository extends JpaRepository<WeeklyMystery, Long> {

    /**
     * Retrieves all unapproved submissions for a specific classroom, ordered by creation date.
     * Uses an {@link EntityGraph} to load associated files, classroom, and pupil details.
     *
     * @param classroomCode the unique code of the classroom
     * @return a list of pending submissions for review
     */
    @EntityGraph(attributePaths = {"fileObject", "classroom", "pupilClassroomMembership", "pupilClassroomMembership.account"})
    List<WeeklyMystery> findAllByClassroomClassroomCodeAndApprovedFalseOrderByCreatedAtDesc(String classroomCode);

    /**
     * Retrieves all approved submissions for a specific classroom, ordered by approval and creation date.
     * Uses an {@link EntityGraph} to load associated files, classroom, and pupil details.
     *
     * @param classroomCode the unique code of the classroom
     * @return a list of approved historical submissions
     */
    @EntityGraph(attributePaths = {"fileObject", "classroom", "pupilClassroomMembership", "pupilClassroomMembership.account"})
    List<WeeklyMystery> findAllByClassroomClassroomCodeAndApprovedTrueOrderByApprovedAtDescCreatedAtDesc(String classroomCode);

    /**
     * Finds a specific submission by its ID within a classroom context.
     *
     * @param id            the unique identifier of the submission
     * @param classroomCode the unique code of the classroom
     * @return an {@link Optional} containing the submission if found
     */
    @EntityGraph(attributePaths = {"fileObject", "classroom", "pupilClassroomMembership", "pupilClassroomMembership.account"})
    Optional<WeeklyMystery> findByIdAndClassroomClassroomCode(Long id, String classroomCode);

    /**
     * Checks if a pupil has at least one approved submission.
     * Often used to determine milestone achievement or eligibility for certain rewards.
     *
     * @param accountEmail the email of the pupil
     * @return true if an approved submission exists, false otherwise
     */
    boolean existsByPupilClassroomMembershipAccountEmailAndApprovedTrue(String accountEmail);
}
