package com.nettdet.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nettdet.backend.domain.Task;
import com.nettdet.backend.domain.enums.TaskDifficulty;

/**
 * Repository interface for managing {@link Task} entities.
 * 
 * <p>Provides methods to retrieve educational tasks based on chapter associations, 
 * difficulty levels, and classroom scoping. Includes a complex query to filter 
 * tasks that are eligible for a specific pupil based on their classroom.
 */
@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    /**
     * Retrieves tasks belonging to a specific chapter with a designated difficulty.
     *
     * @param chapterId  the unique identifier of the chapter
     * @param difficulty the {@link TaskDifficulty} level
     * @return a list of matching tasks
     */
    List<Task> findByChapterIdAndDifficulty(Long chapterId, TaskDifficulty difficulty);

    /**
     * Retrieves all tasks assigned to a specific chapter and classroom.
     *
     * @param chapterId     the unique identifier of the chapter
     * @param classroomCode the unique code of the classroom
     * @return a list of matching tasks
     */
    List<Task> findByChapterIdAndClassroomClassroomCode(Long chapterId, String classroomCode);

    /**
     * Finds all tasks within a chapter and difficulty level that a pupil is eligible to see.
     * A task is eligible if it is either globally available (no classroom assigned)
     * or specifically assigned to the pupil's classroom.
     *
     * @param chapterId     the unique identifier of the chapter
     * @param difficulty    the {@link TaskDifficulty} level
     * @param classroomCode the unique code of the pupil's classroom
     * @return a list of eligible tasks for the pupil
     */
    @Query("SELECT t FROM Task t LEFT JOIN t.classroom c " +
           "WHERE t.chapter.id = :chapterId " +
           "AND t.difficulty = :difficulty " +
           "AND (c.classroomCode = :classroomCode OR c IS NULL)")
    List<Task> findEligibleForPupil(
            @Param("chapterId") Long chapterId,
            @Param("difficulty") TaskDifficulty difficulty,
            @Param("classroomCode") String classroomCode);
}
