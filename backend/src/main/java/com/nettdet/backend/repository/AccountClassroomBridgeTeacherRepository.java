package com.nettdet.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nettdet.backend.domain.AccountClassroomBridgeTeacher;
import com.nettdet.backend.domain.ids.AccountClassroomBridgeTeacherId;

public interface AccountClassroomBridgeTeacherRepository
        extends JpaRepository<AccountClassroomBridgeTeacher, AccountClassroomBridgeTeacherId> {

    boolean existsByIdAccountEmailAndIdClassroomCode(String accountEmail, String classroomCode);

    long countByIdClassroomCode(String classroomCode);

    List<AccountClassroomBridgeTeacher> findAllByIdAccountEmail(String accountEmail);

    void deleteByIdAccountEmailAndIdClassroomCode(String accountEmail, String classroomCode);
}
