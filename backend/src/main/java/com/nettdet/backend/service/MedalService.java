package com.nettdet.backend.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nettdet.backend.domain.FileObject;
import com.nettdet.backend.domain.PupilChapterBridgeProgression;
import com.nettdet.backend.domain.enums.AccountRole;
import com.nettdet.backend.dto.PupilMedalResponse;
import com.nettdet.backend.repository.FileObjectRepository;
import com.nettdet.backend.repository.PupilChapterBridgeProgressionRepository;
import com.nettdet.backend.repository.WeeklyMysteryRepository;
import com.nettdet.backend.security.JwtUserPrincipal;

@Service
public class MedalService {

    private static final Logger logger = LoggerFactory.getLogger(MedalService.class);

    private static final String LOCKED_MEDAL_OBJECT_KEY = "medal-assets/locked.png";

    private static final List<MedalDefinition> MEDAL_CATALOG = List.of(
            MedalDefinition.chapter(
                    "chapter-1",
                    "Nyhetskvartalet",
                    "Fullfør Nyhetskvartalet for å låse opp denne medaljen.",
                    "medal-assets/chapter-1.png",
                    1L),
            MedalDefinition.chapter(
                    "chapter-2",
                    "Fotografen",
                    "Fullfør Fotografen for å låse opp denne medaljen.",
                    "medal-assets/chapter-2.png",
                    2L),
            MedalDefinition.chapter(
                    "chapter-3",
                    "Datasenteret",
                    "Fullfør Datasenteret for å låse opp denne medaljen.",
                    "medal-assets/chapter-3.png",
                    3L),
            MedalDefinition.weeklyMystery(
                    "weekly-mystery",
                    "Ukens mysterium",
                    "Få et bidrag godkjent i Ukens mysterium for å låse opp denne medaljen.",
                    "medal-assets/weekly-mystery.png"));

    private final PupilChapterBridgeProgressionRepository progressionRepository;
    private final WeeklyMysteryRepository weeklyMysteryRepository;
    private final FileObjectRepository fileObjectRepository;

    public MedalService(PupilChapterBridgeProgressionRepository progressionRepository,
                        WeeklyMysteryRepository weeklyMysteryRepository,
                        FileObjectRepository fileObjectRepository) {
        this.progressionRepository = progressionRepository;
        this.weeklyMysteryRepository = weeklyMysteryRepository;
        this.fileObjectRepository = fileObjectRepository;
    }

    @Transactional(readOnly = true)
    public List<PupilMedalResponse> getPupilMedals(JwtUserPrincipal principal) {
        verifyPupilPrincipal(principal);

        Map<Long, PupilChapterBridgeProgression> progressionsByChapterId = new HashMap<>();
        for (PupilChapterBridgeProgression progression :
                progressionRepository.findAllByPupilClassroomMembershipAccountEmailOrderByIdChapterIdAsc(principal.email())) {
            progressionsByChapterId.put(progression.getId().getChapterId(), progression);
        }

        boolean hasApprovedWeeklyMystery = weeklyMysteryRepository
                .existsByPupilClassroomMembershipAccountEmailAndApprovedTrue(principal.email());

        Long lockedImageFileObjectId = findFileObjectId(LOCKED_MEDAL_OBJECT_KEY);

        return MEDAL_CATALOG.stream()
                .map(definition -> toResponse(
                        definition,
                        isUnlocked(definition, progressionsByChapterId, hasApprovedWeeklyMystery),
                        lockedImageFileObjectId))
                .toList();
    }

    private PupilMedalResponse toResponse(MedalDefinition definition,
                                          boolean unlocked,
                                          Long lockedImageFileObjectId) {
        Long imageFileObjectId = unlocked
                ? findFileObjectId(definition.objectKey())
                : lockedImageFileObjectId;

        return new PupilMedalResponse(
                definition.medalKey(),
                definition.name(),
                definition.description(),
                unlocked,
                imageFileObjectId);
    }

    private boolean isUnlocked(MedalDefinition definition,
                               Map<Long, PupilChapterBridgeProgression> progressionsByChapterId,
                               boolean hasApprovedWeeklyMystery) {
        if (definition.type() == MedalType.WEEKLY_MYSTERY) {
            return hasApprovedWeeklyMystery;
        }

        PupilChapterBridgeProgression progression = progressionsByChapterId.get(definition.chapterId());
        return progression != null
                && progression.getSuccessfulRuns() != null
                && progression.getSuccessfulRuns() >= 1;
    }

    private Long findFileObjectId(String objectKey) {
        return fileObjectRepository.findByObjectKey(objectKey)
                .map(FileObject::getId)
                .orElseGet(() -> {
                    logger.warn("Medal asset is missing from file_object: {}", objectKey);
                    return null;
                });
    }

    private void verifyPupilPrincipal(JwtUserPrincipal principal) {
        if (principal == null || principal.email() == null || principal.role() == null) {
            throw new AccessDeniedException("Pupil access is required");
        }

        if (!AccountRole.PUPIL.name().equals(principal.role())) {
            throw new AccessDeniedException("Pupil access is required");
        }
    }

    private enum MedalType {
        CHAPTER,
        WEEKLY_MYSTERY
    }

    private record MedalDefinition(
            String medalKey,
            String name,
            String description,
            String objectKey,
            MedalType type,
            Long chapterId) {

        private static MedalDefinition chapter(String medalKey,
                                                String name,
                                                String description,
                                                String objectKey,
                                                Long chapterId) {
            return new MedalDefinition(medalKey, name, description, objectKey, MedalType.CHAPTER, chapterId);
        }

        private static MedalDefinition weeklyMystery(String medalKey,
                                                     String name,
                                                     String description,
                                                     String objectKey) {
            return new MedalDefinition(medalKey, name, description, objectKey, MedalType.WEEKLY_MYSTERY, null);
        }
    }
}
