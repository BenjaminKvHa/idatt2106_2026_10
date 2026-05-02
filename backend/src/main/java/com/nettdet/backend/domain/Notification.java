package com.nettdet.backend.domain;

import com.nettdet.backend.domain.enums.NotificationStatus;

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
import jakarta.persistence.Table;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Represents a system alert or notification directed at teachers within a classroom.
 * 
 * <p>Notifications are used to inform teachers about significant events regarding pupil progress,
 * such as when a pupil is struggling with a chapter or has reached a milestone. Each notification
 * is associated with a {@link Classroom} and references the {@link PupilClassroomMembership} 
 * of the relevant pupil. The notification's lifecycle (e.g., READ, UNREAD) is tracked via 
 * {@link NotificationStatus}.
 * 
 * <p>Inherits audit information from {@link CreatedAtEntity}.
 */
@Entity
@Table(name = "notification")
public class Notification extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "classroom_code", nullable = false)
    private Classroom classroom;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_email", nullable = false, referencedColumnName = "account_email")
    private PupilClassroomMembership pupilClassroomMembership;

    @Column(name = "notification_text", nullable = false, columnDefinition = "TEXT")
    private String notificationText;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "status_notifications", nullable = false, columnDefinition = "notification_status_enum")
    private NotificationStatus statusNotifications;

    public Notification() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public String getNotificationText() {
        return notificationText;
    }

    public void setNotificationText(String notificationText) {
        this.notificationText = notificationText;
    }

    public NotificationStatus getStatusNotifications() {
        return statusNotifications;
    }

    public void setStatusNotifications(NotificationStatus statusNotifications) {
        this.statusNotifications = statusNotifications;
    }
}
