package com.nettdet.backend.domain;

import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

@Entity
@Table(name = "account_classroom_bridge_teacher")
public class AccountClassroomBridgeTeacher extends CreatedAtEntity {

    @EmbeddedId
    private AccountClassroomBridgeTeacherId id;

    public AccountClassroomBridgeTeacherId getId() {
        return id;
    }

    public void setId(AccountClassroomBridgeTeacherId id) {
        this.id = id;
    }
}
