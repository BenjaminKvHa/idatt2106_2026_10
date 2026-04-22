package com.nettdet.backend.domain;

import com.nettdet.backend.domain.ids.PupilChapterBridgeProgressionId;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "pupil_chapter_bridge_progression")
public class PupilChapterBridgeProgression extends CreatedAtEntity {

    @EmbeddedId
    private PupilChapterBridgeProgressionId id;

    @MapsId("email")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "email", nullable = false, referencedColumnName = "account_email")
    private PupilClassroomMembership pupilClassroomMembership;

    @MapsId("chapterId")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chapter_id", nullable = false)
    private Chapter chapter;

    @Column(name = "successful_runs", nullable = false)
    private Integer successfulRuns;

    @Column(name = "highscore", nullable = false)
    private Integer highscore;

    @Column(name = "unsuccessful_streak", nullable = false)
    private Integer unsuccessfulStreak;

    @Column(name = "current_run_attempts", nullable = false)
    private Integer currentRunAttempts;

    @Column(name = "current_run_correct", nullable = false)
    private Integer currentRunCorrect;

    public PupilChapterBridgeProgression() {
    }

    public PupilChapterBridgeProgressionId getId() {
        return id;
    }

    public void setId(PupilChapterBridgeProgressionId id) {
        this.id = id;
    }

    public PupilClassroomMembership getPupilClassroomMembership() {
        return pupilClassroomMembership;
    }

    public void setPupilClassroomMembership(PupilClassroomMembership pupilClassroomMembership) {
        this.pupilClassroomMembership = pupilClassroomMembership;
    }

    public Chapter getChapter() {
        return chapter;
    }

    public void setChapter(Chapter chapter) {
        this.chapter = chapter;
    }

    public Integer getSuccessfulRuns() {
        return successfulRuns;
    }

    public void setSuccessfulRuns(Integer successfulRuns) {
        this.successfulRuns = successfulRuns;
    }

    public Integer getHighscore() {
        return highscore;
    }

    public void setHighscore(Integer highscore) {
        this.highscore = highscore;
    }

    public Integer getUnsuccessfulStreak() {
        return unsuccessfulStreak;
    }

    public void setUnsuccessfulStreak(Integer unsuccessfulStreak) {
        this.unsuccessfulStreak = unsuccessfulStreak;
    }

    public Integer getCurrentRunAttempts() {
        return currentRunAttempts;
    }

    public void setCurrentRunAttempts(Integer currentRunAttempts) {
        this.currentRunAttempts = currentRunAttempts;
    }

    public Integer getCurrentRunCorrect() {
        return currentRunCorrect;
    }

    public void setCurrentRunCorrect(Integer currentRunCorrect) {
        this.currentRunCorrect = currentRunCorrect;
    }
}
