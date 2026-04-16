package com.nettdet.backend.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.*;

@Entity
@Table(name = "classroom")
public class Classroom {

    @Id
    @Column(name = "classroom_code", length = 6)
    private String classroomCode;

    @Column(name = "classroom_name", nullable = false)
    private String classroomName;

    @Column(name = "classroom_description", columnDefinition = "TEXT")
    private String classroomDescription;

    @OneToMany(mappedBy = "classroom")
    private List<FileObject> files = new ArrayList<>();

    @OneToMany(mappedBy = "classroom")
    private List<AccountClassroomBridgeTeacher> teacherBridges = new ArrayList<>();

    @OneToMany(mappedBy = "classroom")
    private List<PupilClassroomMembership> pupilMemberships = new ArrayList<>();

    @OneToMany(mappedBy = "classroom")
    private List<Task> tasks = new ArrayList<>();

    public Classroom() {
    }

    public String getClassroomCode() {
        return classroomCode;
    }

    public void setClassroomCode(String classroomCode) {
        this.classroomCode = classroomCode;
    }

    public String getClassroomName() {
        return classroomName;
    }

    public void setClassroomName(String classroomName) {
        this.classroomName = classroomName;
    }

    public String getClassroomDescription() {
        return classroomDescription;
    }

    public void setClassroomDescription(String classroomDescription) {
        this.classroomDescription = classroomDescription;
    }

    public List<FileObject> getFiles() {
        return files;
    }

    public void setFiles(List<FileObject> files) {
        this.files = files;
    }

    public List<AccountClassroomBridgeTeacher> getTeacherBridges() {
        return teacherBridges;
    }

    public void setTeacherBridges(List<AccountClassroomBridgeTeacher> teacherBridges) {
        this.teacherBridges = teacherBridges;
    }

    public List<PupilClassroomMembership> getPupilMemberships() {
        return pupilMemberships;
    }

    public void setPupilMemberships(List<PupilClassroomMembership> pupilMemberships) {
        this.pupilMemberships = pupilMemberships;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }
}
