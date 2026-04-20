package com.nettdet.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nettdet.backend.domain.PupilChapterBridgeProgression;
import com.nettdet.backend.domain.ids.PupilChapterBridgeProgressionId;

public interface PupilChapterBridgeProgressionRepository
        extends JpaRepository<PupilChapterBridgeProgression, PupilChapterBridgeProgressionId> {

    long countByPupilClassroomMembershipAccountEmail(String accountEmail);

    List<PupilChapterBridgeProgression> findAllByPupilClassroomMembershipAccountEmailOrderByIdChapterIdAsc(String accountEmail);
}
