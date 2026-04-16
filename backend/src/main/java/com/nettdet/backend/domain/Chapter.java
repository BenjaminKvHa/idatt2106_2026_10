package com.nettdet.backend.domain;

import java.util.ArrayList;
import java.util.List;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

@Entity
@Table(name = "chapter")
public class Chapter extends CreatedAtEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "chapter_name", nullable = false)
    private String chapterName;

    @Column(name = "successful_runs_needed_for_completion", nullable = false)
    private Integer successfulRunsNeededForCompletion;

    @OneToMany(mappedBy = "chapter")
    private List<Task> tasks = new ArrayList<>();

    @OneToMany(mappedBy = "chapter")
    private List<PupilChapterBridgeProgression> pupilProgressions = new ArrayList<>();

    public Chapter() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getChapterName() {
        return chapterName;
    }

    public void setChapterName(String chapterName) {
        this.chapterName = chapterName;
    }

    public Integer getSuccessfulRunsNeededForCompletion() {
        return successfulRunsNeededForCompletion;
    }

    public void setSuccessfulRunsNeededForCompletion(Integer successfulRunsNeededForCompletion) {
        this.successfulRunsNeededForCompletion = successfulRunsNeededForCompletion;
    }

    public List<Task> getTasks() {
        return tasks;
    }

    public void setTasks(List<Task> tasks) {
        this.tasks = tasks;
    }

    public List<PupilChapterBridgeProgression> getPupilProgressions() {
        return pupilProgressions;
    }

    public void setPupilProgressions(List<PupilChapterBridgeProgression> pupilProgressions) {
        this.pupilProgressions = pupilProgressions;
    }
}
