package com.nettdet.backend.domain;

import com.nettdet.backend.domain.enums.AccountRole;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "account")
public class Account extends CreatedAtEntity {

    @Id
    @Column(name = "email")
    private String email;

    @Column(name = "password_data", columnDefinition = "TEXT")
    private String passwordData;

    @Column(name = "legal_name", nullable = false)
    private String legalName;

    @Column(name = "account_role")
    private AccountRole accountRole;

    @OneToMany(mappedBy = "account")
    private List<RefreshToken> refreshTokens = new ArrayList<>();

    // @Column(name = "classrom_code")
    // private String classromCode;

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
}
