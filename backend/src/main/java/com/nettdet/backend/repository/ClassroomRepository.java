package com.nettdet.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nettdet.backend.domain.Classroom;

/**
 * Repository interface for managing {@link Classroom} entities.
 * 
 * <p>Supports standard persistence operations for classrooms using the 
 * unique 6-character classroom code as the primary identifier.
 */
public interface ClassroomRepository extends JpaRepository<Classroom, String> {
}
