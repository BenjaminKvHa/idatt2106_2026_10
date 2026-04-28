package com.nettdet.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.nettdet.backend.domain.Task;
import com.nettdet.backend.domain.enums.TaskDifficulty;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {

    List<Task> findByChapterIdAndDifficulty(Long chapterId, TaskDifficulty difficulty);

    List<Task> findByChapterIdAndClassroomClassroomCode(Long chapterId, String classroomCode);

    @Query("SELECT t FROM Task t LEFT JOIN t.classroom c " +
           "WHERE t.chapter.id = :chapterId " +
           "AND t.difficulty = :difficulty " +
           "AND (c.classroomCode = :classroomCode OR c IS NULL)")
    List<Task> findEligibleForPupil(
            @Param("chapterId") Long chapterId,
            @Param("difficulty") TaskDifficulty difficulty,
            @Param("classroomCode") String classroomCode);
}
