package com.nettdet.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nettdet.backend.domain.AccountClassroomBridgeTeacher;
import com.nettdet.backend.domain.ids.AccountClassroomBridgeTeacherId;

public interface AccountClassroomBridgeTeacherRepository
        extends JpaRepository<AccountClassroomBridgeTeacher, AccountClassroomBridgeTeacherId> {

    boolean existsByIdAccountEmailAndIdClassroomCode(String accountEmail, String classroomCode);

    long countByIdClassroomCode(String classroomCode);

    void deleteByIdAccountEmailAndIdClassroomCode(String accountEmail, String classroomCode);
}
