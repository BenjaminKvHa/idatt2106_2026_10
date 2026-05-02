package com.nettdet.backend.domain;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

/**
 * Represents a pupil's submission for the "Weekly Mystery" challenge.
 * 
 * <p>A WeeklyMystery entry links a pupil's uploaded {@link FileObject} to a {@link Classroom}
 * and includes their reasoning or {@code comment}. Submissions are reviewed by teachers,
 * with approval status and timestamps tracked via {@code approved} and {@code approvedAt}.
 * 
 * <p>Inherits audit information from {@link CreatedAtEntity}.
 */
@Entity
@Table(name = "weekly_mystery")
public class WeeklyMystery extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "file_object_id", nullable = false)
    private FileObject fileObject;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_code", nullable = false)
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_email", nullable = false, referencedColumnName = "account_email")
    private PupilClassroomMembership pupilClassroomMembership;

    @Column(name = "comment", nullable = false, columnDefinition = "TEXT")
    private String comment;

    @Column(name = "alt_text", columnDefinition = "TEXT")
    private String altText;

    @Column(name = "approved", nullable = false)
    private boolean approved;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    public WeeklyMystery() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public FileObject getFileObject() {
        return fileObject;
    }

    public void setFileObject(FileObject fileObject) {
        this.fileObject = fileObject;
    }

    public Classroom getClassroom() {
        return classroom;
    }

    public void setClassroom(Classroom classroom) {
        this.classroom = classroom;
    }

    public PupilClassroomMembership getPupilClassroomMembership() {
        return pupilClassroomMembership;
    }

    public void setPupilClassroomMembership(PupilClassroomMembership pupilClassroomMembership) {
        this.pupilClassroomMembership = pupilClassroomMembership;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }

    public String getAltText() {
        return altText;
    }

    public void setAltText(String altText) {
        this.altText = altText;
    }

    public boolean isApproved() {
        return approved;
    }

    public void setApproved(boolean approved) {
        this.approved = approved;
    }

    public LocalDateTime getApprovedAt() {
        return approvedAt;
    }

    public void setApprovedAt(LocalDateTime approvedAt) {
        this.approvedAt = approvedAt;
    }
}
