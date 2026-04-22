package com.nettdet.backend.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nettdet.backend.domain.PupilChapterBridgeProgression;
import com.nettdet.backend.domain.ids.PupilChapterBridgeProgressionId;

public interface PupilChapterBridgeProgressionRepository
        extends JpaRepository<PupilChapterBridgeProgression, PupilChapterBridgeProgressionId> {

    long countByPupilClassroomMembershipAccountEmail(String accountEmail);

    Optional<PupilChapterBridgeProgression> findByIdEmailAndIdChapterId(String email, Long chapterId);

    Optional<PupilChapterBridgeProgression> findByIdEmailAndIdChapterIdAndPupilClassroomMembershipClassroomClassroomCode(
            String email,
            Long chapterId,
            String classroomCode);

    List<PupilChapterBridgeProgression> findAllByPupilClassroomMembershipAccountEmailOrderByIdChapterIdAsc(String accountEmail);
}
