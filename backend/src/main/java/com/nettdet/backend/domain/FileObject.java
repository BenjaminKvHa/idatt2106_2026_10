package com.nettdet.backend.domain;

import java.util.ArrayList;
import java.util.List;

import com.nettdet.backend.domain.enums.ClassroomAccess;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Represents metadata for a file stored in an external object storage service (e.g., AWS S3).
 * 
 * <p>Each FileObject contains a unique {@code objectKey} used to retrieve the physical content,
 * original filename, and access control settings via {@link ClassroomAccess}. It may be 
 * associated with a specific {@link Classroom} and is tracked by the uploader's {@link Account}.
 */
@Entity
@Table(name = "file_object")
public class FileObject {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "object_key", nullable = false, unique = true, columnDefinition = "TEXT")
    private String objectKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "uploaded_by")
    private Account uploadedByAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_code")
    private Classroom classroom;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "access_level", nullable = false, columnDefinition = "classroom_access_enum")
    private ClassroomAccess accessLevel;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @OneToMany(mappedBy = "avatarFileObject")
    private List<PupilClassroomMembership> avatarForPupils = new ArrayList<>();

    public FileObject() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }

    public Account getUploadedByAccount() {
        return uploadedByAccount;
    }

    public void setUploadedByAccount(Account uploadedByAccount) {
        this.uploadedByAccount = uploadedByAccount;
    }

    public Classroom getClassroom() {
        return classroom;
    }

    public void setClassroom(Classroom classroom) {
        this.classroom = classroom;
    }

    public ClassroomAccess getAccessLevel() {
        return accessLevel;
    }

    public void setAccessLevel(ClassroomAccess accessLevel) {
        this.accessLevel = accessLevel;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public List<PupilClassroomMembership> getAvatarForPupils() {
        return avatarForPupils;
    }

    public void setAvatarForPupils(List<PupilClassroomMembership> avatarForPupils) {
        this.avatarForPupils = avatarForPupils;
    }
}
