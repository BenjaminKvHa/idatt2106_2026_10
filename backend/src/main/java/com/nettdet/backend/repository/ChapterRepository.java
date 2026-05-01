package com.nettdet.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nettdet.backend.domain.Chapter;

/**
 * Repository interface for managing {@link Chapter} entities.
 * 
 * <p>Handles persistence operations for curriculum modules and provides
 * functionality to retrieve chapters in their sequential order.
 */
@Repository
public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    /**
     * Retrieves all chapters ordered by their primary identifier in ascending order.
     * This ensures chapters are returned in the sequence they were defined/created.
     *
     * @return a list of chapters sorted by ID
     */
    List<Chapter> findAllByOrderByIdAsc();
}
