package com.nettdet.backend.domain.ids;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class AccountClassroomBridgeTeacherId implements Serializable {

    @Column(name = "account_email")
    private String accountEmail;

    @Column(name = "classroom_code")
    private String classroomCode;

    public AccountClassroomBridgeTeacherId() {
    }

    public AccountClassroomBridgeTeacherId(String accountEmail, String classroomCode) {
        this.accountEmail = accountEmail;
        this.classroomCode = classroomCode;
    }

    public String getAccountEmail() {
        return accountEmail;
    }

    public void setAccountEmail(String accountEmail) {
        this.accountEmail = accountEmail;
    }

    public String getClassroomCode() {
        return classroomCode;
    }

    public void setClassroomCode(String classroomCode) {
        this.classroomCode = classroomCode;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        AccountClassroomBridgeTeacherId that = (AccountClassroomBridgeTeacherId) o;
        return Objects.equals(accountEmail, that.accountEmail)
                && Objects.equals(classroomCode, that.classroomCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(accountEmail, classroomCode);
    }
}
