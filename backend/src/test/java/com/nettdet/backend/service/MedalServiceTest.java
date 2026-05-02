package com.nettdet.backend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;

import com.nettdet.backend.domain.FileObject;
import com.nettdet.backend.domain.PupilChapterBridgeProgression;
import com.nettdet.backend.domain.ids.PupilChapterBridgeProgressionId;
import com.nettdet.backend.dto.PupilMedalResponse;
import com.nettdet.backend.repository.FileObjectRepository;
import com.nettdet.backend.repository.PupilChapterBridgeProgressionRepository;
import com.nettdet.backend.repository.WeeklyMysteryRepository;
import com.nettdet.backend.security.JwtUserPrincipal;

@ExtendWith(MockitoExtension.class)
class MedalServiceTest {

    private static final String PUPIL_EMAIL = "pupil@example.com";

    @Mock
    private PupilChapterBridgeProgressionRepository progressionRepository;

    @Mock
    private WeeklyMysteryRepository weeklyMysteryRepository;

    @Mock
    private FileObjectRepository fileObjectRepository;

    @InjectMocks
    private MedalService medalService;

    @DisplayName("Returns unlocked chapter and weekly mystery medals with unlocked images")
    @Test
    void getPupilMedals_whenPupilHasCompletedChapterAndApprovedMystery_returnsUnlockedMedals() {
        JwtUserPrincipal principal = pupilPrincipal();
        when(progressionRepository.findAllByPupilClassroomMembershipAccountEmailOrderByIdChapterIdAsc(PUPIL_EMAIL))
                .thenReturn(List.of(progression(1L, 1), progression(2L, 0)));
        when(weeklyMysteryRepository.existsByPupilClassroomMembershipAccountEmailAndApprovedTrue(PUPIL_EMAIL))
                .thenReturn(true);
        mockMedalAsset("medal-assets/locked.png", 100L);
        mockMedalAsset("medal-assets/chapter-1.png", 101L);
        mockMedalAsset("medal-assets/weekly-mystery.png", 104L);

        List<PupilMedalResponse> medals = medalService.getPupilMedals(principal);

        assertEquals(4, medals.size());
        assertMedal(medals.get(0), "chapter-1", "Nyhetskvartalet", true, 101L);
        assertMedal(medals.get(1), "chapter-2", "Fotografen", false, 100L);
        assertMedal(medals.get(2), "chapter-3", "Datasenteret", false, 100L);
        assertMedal(medals.get(3), "weekly-mystery", "Ukens mysterium", true, 104L);
        verify(progressionRepository).findAllByPupilClassroomMembershipAccountEmailOrderByIdChapterIdAsc(PUPIL_EMAIL);
        verify(weeklyMysteryRepository).existsByPupilClassroomMembershipAccountEmailAndApprovedTrue(PUPIL_EMAIL);
    }

    @DisplayName("Returns locked image for missing progression and zero successful runs")
    @Test
    void getPupilMedals_whenProgressionMissingOrIncomplete_returnsLockedImages() {
        JwtUserPrincipal principal = pupilPrincipal();
        when(progressionRepository.findAllByPupilClassroomMembershipAccountEmailOrderByIdChapterIdAsc(PUPIL_EMAIL))
                .thenReturn(List.of(progression(1L, 0)));
        when(weeklyMysteryRepository.existsByPupilClassroomMembershipAccountEmailAndApprovedTrue(PUPIL_EMAIL))
                .thenReturn(false);
        mockMedalAsset("medal-assets/locked.png", 100L);

        List<PupilMedalResponse> medals = medalService.getPupilMedals(principal);

        assertEquals(List.of(false, false, false, false), medals.stream()
                .map(PupilMedalResponse::unlocked)
                .toList());
        assertEquals(List.of(100L, 100L, 100L, 100L), medals.stream()
                .map(PupilMedalResponse::imageFileObjectId)
                .toList());
    }

    @DisplayName("Returns null image id when required medal asset is missing")
    @Test
    void getPupilMedals_whenUnlockedAssetIsMissing_returnsNullImageFileObjectId() {
        JwtUserPrincipal principal = pupilPrincipal();
        when(progressionRepository.findAllByPupilClassroomMembershipAccountEmailOrderByIdChapterIdAsc(PUPIL_EMAIL))
                .thenReturn(List.of(progression(1L, 1)));
        when(weeklyMysteryRepository.existsByPupilClassroomMembershipAccountEmailAndApprovedTrue(PUPIL_EMAIL))
                .thenReturn(false);
        mockMedalAsset("medal-assets/locked.png", 100L);
        when(fileObjectRepository.findByObjectKey("medal-assets/chapter-1.png")).thenReturn(Optional.empty());

        List<PupilMedalResponse> medals = medalService.getPupilMedals(principal);

        assertEquals("chapter-1", medals.getFirst().medalKey());
        assertEquals(true, medals.getFirst().unlocked());
        assertNull(medals.getFirst().imageFileObjectId());
    }

    @DisplayName("Returns null locked image id when shared locked asset is missing")
    @Test
    void getPupilMedals_whenLockedAssetIsMissing_returnsNullForLockedMedals() {
        JwtUserPrincipal principal = pupilPrincipal();
        when(progressionRepository.findAllByPupilClassroomMembershipAccountEmailOrderByIdChapterIdAsc(PUPIL_EMAIL))
                .thenReturn(List.of());
        when(weeklyMysteryRepository.existsByPupilClassroomMembershipAccountEmailAndApprovedTrue(PUPIL_EMAIL))
                .thenReturn(false);
        when(fileObjectRepository.findByObjectKey("medal-assets/locked.png")).thenReturn(Optional.empty());

        List<PupilMedalResponse> medals = medalService.getPupilMedals(principal);

        assertEquals(false, medals.getFirst().unlocked());
        assertNull(medals.getFirst().imageFileObjectId());
    }

    @DisplayName("Rejects non-pupil principal")
    @Test
    void getPupilMedals_whenPrincipalIsNotPupil_throwsAccessDenied() {
        JwtUserPrincipal principal = new JwtUserPrincipal("Teacher Name", "teacher@example.com", "TEACHER");

        assertThrows(AccessDeniedException.class, () -> medalService.getPupilMedals(principal));

        verifyNoInteractions(progressionRepository, weeklyMysteryRepository, fileObjectRepository);
    }

    @DisplayName("Rejects null principal")
    @Test
    void getPupilMedals_whenPrincipalIsNull_throwsAccessDenied() {
        assertThrows(AccessDeniedException.class, () -> medalService.getPupilMedals(null));

        verifyNoInteractions(progressionRepository, weeklyMysteryRepository, fileObjectRepository);
    }

    @DisplayName("Returns an unlocked chapter medal by chapter id")
    @Test
    void getUnlockedChapterMedal_whenChapterMedalExists_returnsUnlockedMedal() {
        mockMedalAsset("medal-assets/chapter-3.png", 103L);

        PupilMedalResponse medal = medalService.getUnlockedChapterMedal(3L);

        assertMedal(medal, "chapter-3", "Datasenteret", true, 103L);
    }

    @DisplayName("Returns null when no chapter medal exists for chapter id")
    @Test
    void getUnlockedChapterMedal_whenChapterMedalDoesNotExist_returnsNull() {
        PupilMedalResponse medal = medalService.getUnlockedChapterMedal(999L);

        assertNull(medal);
        verifyNoInteractions(fileObjectRepository);
    }

    private JwtUserPrincipal pupilPrincipal() {
        return new JwtUserPrincipal("Pupil Name", PUPIL_EMAIL, "PUPIL");
    }

    private PupilChapterBridgeProgression progression(Long chapterId, Integer successfulRuns) {
        PupilChapterBridgeProgression progression = new PupilChapterBridgeProgression();
        progression.setId(new PupilChapterBridgeProgressionId(PUPIL_EMAIL, chapterId));
        progression.setSuccessfulRuns(successfulRuns);
        return progression;
    }

    private void mockMedalAsset(String objectKey, Long id) {
        when(fileObjectRepository.findByObjectKey(objectKey)).thenReturn(Optional.of(fileObject(id, objectKey)));
    }

    private FileObject fileObject(Long id, String objectKey) {
        FileObject fileObject = new FileObject();
        fileObject.setId(id);
        fileObject.setObjectKey(objectKey);
        fileObject.setFileName(objectKey.substring(objectKey.lastIndexOf('/') + 1));
        return fileObject;
    }

    private void assertMedal(PupilMedalResponse response,
                             String medalKey,
                             String name,
                             boolean unlocked,
                             Long imageFileObjectId) {
        assertEquals(medalKey, response.medalKey());
        assertEquals(name, response.name());
        assertEquals(unlocked, response.unlocked());
        assertEquals(imageFileObjectId, response.imageFileObjectId());
    }
}
