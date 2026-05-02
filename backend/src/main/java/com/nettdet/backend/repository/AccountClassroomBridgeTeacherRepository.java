package com.nettdet.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import com.nettdet.backend.domain.AccountClassroomBridgeTeacher;
import com.nettdet.backend.domain.ids.AccountClassroomBridgeTeacherId;

/**
 * Repository interface for managing {@link AccountClassroomBridgeTeacher} entities.
 * 
 * <p>Provides specialized methods to query the teaching relationships between accounts
 * and classrooms, including checking for existence, counting teachers per classroom, 
 * and retrieving memberships for specific accounts or classrooms.
 */
public interface AccountClassroomBridgeTeacherRepository
        extends JpaRepository<AccountClassroomBridgeTeacher, AccountClassroomBridgeTeacherId> {

    /**
     * Checks if a teaching relationship exists for a specific account email and classroom code.
     *
     * @param accountEmail  the email of the account
     * @param classroomCode the unique code of the classroom
     * @return true if the relationship exists, false otherwise
     */
    boolean existsByIdAccountEmailAndIdClassroomCode(String accountEmail, String classroomCode);

    /**
     * Counts the number of teachers assigned to a specific classroom.
     *
     * @param classroomCode the unique code of the classroom
     * @return the number of teachers in the classroom
     */
    long countByIdClassroomCode(String classroomCode);

    /**
     * Retrieves all teaching relationships associated with a specific account.
     *
     * @param accountEmail the email of the account
     * @return a list of teaching bridge entities
     */
    List<AccountClassroomBridgeTeacher> findAllByIdAccountEmail(String accountEmail);

    /**
     * Retrieves all teaching relationships for a specific classroom.
     * Uses an {@link EntityGraph} to eagerly fetch associated account data.
     *
     * @param classroomCode the unique code of the classroom
     * @return a list of teaching bridge entities with loaded accounts
     */
    @EntityGraph(attributePaths = "account")
    List<AccountClassroomBridgeTeacher> findAllByIdClassroomCode(String classroomCode);

    /**
     * Deletes a teaching relationship for a specific account email and classroom code.
     *
     * @param accountEmail  the email of the account
     * @param classroomCode the unique code of the classroom
     */
    void deleteByIdAccountEmailAndIdClassroomCode(String accountEmail, String classroomCode);
}
