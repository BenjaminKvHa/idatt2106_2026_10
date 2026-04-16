package com.nettdet.backend.domain;

import com.nettdet.backend.domain.ids.AccountClassroomBridgeTeacherId;

import jakarta.persistence.*;

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
