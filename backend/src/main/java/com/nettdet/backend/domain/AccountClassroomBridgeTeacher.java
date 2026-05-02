package com.nettdet.backend.domain;

import com.nettdet.backend.domain.ids.AccountClassroomBridgeTeacherId;

import jakarta.persistence.*;

/**
 * Represents a many-to-many bridge entity linking {@link Account}s to {@link Classroom}s with teacher-level access.
 * 
 * <p>This entity explicitly defines the teaching relationship, allowing an account to manage
 * multiple classrooms and a classroom to have multiple teachers. It uses a composite key
 * {@link AccountClassroomBridgeTeacherId} to ensure unique pairings.
 * 
 * <p>Inherits audit information from {@link CreatedAtEntity}.
 */
@Entity
@Table(name = "account_classroom_bridge_teacher")
public class AccountClassroomBridgeTeacher extends CreatedAtEntity {

    @EmbeddedId
    private AccountClassroomBridgeTeacherId id;

    @MapsId("accountEmail")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_email", nullable = false)
    private Account account;

    @MapsId("classroomCode")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_code", nullable = false)
    private Classroom classroom;

    public AccountClassroomBridgeTeacher() {
    }

    public AccountClassroomBridgeTeacherId getId() {
        return id;
    }

    public void setId(AccountClassroomBridgeTeacherId id) {
        this.id = id;
    }

    public Account getAccount() {
        return account;
    }

    public void setAccount(Account account) {
        this.account = account;
    }

    public Classroom getClassroom() {
        return classroom;
    }

    public void setClassroom(Classroom classroom) {
        this.classroom = classroom;
    }
}
