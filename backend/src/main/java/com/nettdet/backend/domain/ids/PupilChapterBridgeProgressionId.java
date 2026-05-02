package com.nettdet.backend.domain.ids;

import java.io.Serializable;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

@Embeddable
public class PupilChapterBridgeProgressionId implements Serializable {

    @Column(name = "email")
    private String email;

    @Column(name = "chapter_id")
    private Long chapterId;

    public PupilChapterBridgeProgressionId() {
    }

    public PupilChapterBridgeProgressionId(String email, Long chapterId) {
        this.email = email;
        this.chapterId = chapterId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public Long getChapterId() {
        return chapterId;
    }

    public void setChapterId(Long chapterId) {
        this.chapterId = chapterId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        PupilChapterBridgeProgressionId that = (PupilChapterBridgeProgressionId) o;
        return Objects.equals(email, that.email)
                && Objects.equals(chapterId, that.chapterId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(email, chapterId);
    }
}
