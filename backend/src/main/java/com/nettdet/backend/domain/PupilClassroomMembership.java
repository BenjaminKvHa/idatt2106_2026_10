package com.nettdet.backend.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

@Entity
@Table(name = "pupil_classroom_membership")
public class PupilClassroomMembership extends CreatedAtEntity {

    @Id
    @Column(name = "account_email")
    private String accountEmail;

    @MapsId
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_email", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_code")
    private Classroom classroom;

    @Column(name = "username")
    private String username;

    @Column(name = "membership_confirmed_by_teacher", nullable = false)
    private boolean membershipConfirmedByTeacher;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "avatar_file_object_id")
    private FileObject avatarFileObject;

    @OneToMany(mappedBy = "pupilClassroomMembership")
    private List<PupilChapterBridgeProgression> chapterProgressions = new ArrayList<>();

    public PupilClassroomMembership() {
    }

    public String getAccountEmail() {
        return accountEmail;
    }

    public void setAccountEmail(String accountEmail) {
        this.accountEmail = accountEmail;
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public boolean isMembershipConfirmedByTeacher() {
        return membershipConfirmedByTeacher;
    }

    public void setMembershipConfirmedByTeacher(boolean membershipConfirmedByTeacher) {
        this.membershipConfirmedByTeacher = membershipConfirmedByTeacher;
    }

    public FileObject getAvatarFileObject() {
        return avatarFileObject;
    }

    public void setAvatarFileObject(FileObject avatarFileObject) {
        this.avatarFileObject = avatarFileObject;
    }

    public List<PupilChapterBridgeProgression> getChapterProgressions() {
        return chapterProgressions;
    }

    public void setChapterProgressions(List<PupilChapterBridgeProgression> chapterProgressions) {
        this.chapterProgressions = chapterProgressions;
    }
}
