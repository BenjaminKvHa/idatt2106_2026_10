package com.nettdet.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nettdet.backend.domain.PupilChapterBridgeProgression;
import com.nettdet.backend.domain.ids.PupilChapterBridgeProgressionId;

/**
 * Repository interface for managing {@link PupilChapterBridgeProgression} entities.
 * 
 * <p>Provides specialized methods to track and retrieve pupil progress across chapters.
 * Supports lookups by pupil email, chapter ID, and classroom context to manage detailed
 * learning statistics and notebooks.
 */
public interface PupilChapterBridgeProgressionRepository
        extends JpaRepository<PupilChapterBridgeProgression, PupilChapterBridgeProgressionId> {

    /**
     * Counts the number of chapter progressions recorded for a specific pupil.
     *
     * @param accountEmail the email of the pupil
     * @return the total count of chapters the pupil has interacted with
     */
    long countByPupilClassroomMembershipAccountEmail(String accountEmail);

    /**
     * Finds the progression record for a specific pupil and chapter.
     *
     * @param email     the email of the pupil
     * @param chapterId the unique identifier of the chapter
     * @return an {@link Optional} containing the progression record if found
     */
    Optional<PupilChapterBridgeProgression> findByIdEmailAndIdChapterId(String email, Long chapterId);

    /**
     * Checks whether a progression record exists for a specific pupil and chapter.
     *
     * @param email     the email of the pupil
     * @param chapterId the unique identifier of the chapter
     * @return true if the progression record exists, false otherwise
     */
    boolean existsByIdEmailAndIdChapterId(String email, Long chapterId);

    /**
     * Finds the progression record for a pupil and chapter within a specific classroom.
     * This ensures the progression is relevant to the current classroom context.
     *
     * @param email         the email of the pupil
     * @param chapterId     the unique identifier of the chapter
     * @param classroomCode the unique code of the classroom
     * @return an {@link Optional} containing the progression record if found
     */
    Optional<PupilChapterBridgeProgression> findByIdEmailAndIdChapterIdAndPupilClassroomMembershipClassroomClassroomCode(
            String email,
            Long chapterId,
            String classroomCode);

    /**
     * Retrieves all chapter progressions for a specific pupil, sorted by chapter ID.
     *
     * @param accountEmail the email of the pupil
     * @return a list of progressions in sequential chapter order
     */
    List<PupilChapterBridgeProgression> findAllByPupilClassroomMembershipAccountEmailOrderByIdChapterIdAsc(String accountEmail);
}
