package com.nettdet.backend.domain;

import com.nettdet.backend.domain.enums.AccountRole;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "account")
public class Account extends CreatedAtEntity {

    @Id
    @Column(name = "email")
    private String email;

    @Column(name = "password_data", nullable = false, columnDefinition = "TEXT")
    private String passwordData;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "account_role", nullable = false, columnDefinition = "account_role_enum")
    private AccountRole accountRole;

    @OneToMany(mappedBy = "account")
    private List<RefreshToken> refreshTokens = new ArrayList<>();

    @OneToMany(mappedBy = "uploadedByAccount")
    private List<FileObject> uploadedFiles = new ArrayList<>();

    @OneToMany(mappedBy = "account")
    private List<AccountClassroomBridgeTeacher> teacherClassrooms = new ArrayList<>();

    @OneToOne(mappedBy = "account")
    private PupilClassroomMembership pupilMembership;

    public Account() {}

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordData() {
        return passwordData;
    }

    public void setPasswordData(String passwordData) {
        this.passwordData = passwordData;
    }

    public String getLegalName() {
        return legalName;
    }

    public void setLegalName(String legalName) {
        this.legalName = legalName;
    }

    public AccountRole getAccountRole() {
        return accountRole;
    }

    public void setAccountRole(AccountRole accountRole) {
        this.accountRole = accountRole;
    }

    public List<RefreshToken> getRefreshTokens() {
        return refreshTokens;
    }

    public void setRefreshTokens(List<RefreshToken> refreshTokens) {
        this.refreshTokens = refreshTokens;
    }

    public List<FileObject> getUploadedFiles() {
        return uploadedFiles;
    }

    public void setUploadedFiles(List<FileObject> uploadedFiles) {
        this.uploadedFiles = uploadedFiles;
    }

    public List<AccountClassroomBridgeTeacher> getTeacherClassrooms() {
        return teacherClassrooms;
    }

    public void setTeacherClassrooms(List<AccountClassroomBridgeTeacher> teacherClassrooms) {
        this.teacherClassrooms = teacherClassrooms;
    }

    public PupilClassroomMembership getPupilMembership() {
        return pupilMembership;
    }

    public void setPupilMembership(PupilClassroomMembership pupilMembership) {
        this.pupilMembership = pupilMembership;
    }
}
