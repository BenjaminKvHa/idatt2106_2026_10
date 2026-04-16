package com.nettdet.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nettdet.backend.domain.PupilChapterBridgeProgression;
import com.nettdet.backend.domain.ids.PupilChapterBridgeProgressionId;

public interface PupilChapterBridgeProgressionRepository
        extends JpaRepository<PupilChapterBridgeProgression, PupilChapterBridgeProgressionId> {

    long countByPupilClassroomMembershipAccountEmail(String accountEmail);
}
