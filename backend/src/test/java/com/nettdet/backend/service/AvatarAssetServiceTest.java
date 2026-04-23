package com.nettdet.backend.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nettdet.backend.domain.FileObject;
import com.nettdet.backend.domain.enums.ClassroomAccess;
import com.nettdet.backend.dto.AvatarAssetResponse;
import com.nettdet.backend.repository.FileObjectRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class AvatarAssetServiceTest {

    @Mock
    private FileObjectRepository fileObjectRepository;

    @Mock
    private FileObjectService fileObjectService;

    @DisplayName("Lists valid avatar assets once, normalized and sorted")
    @Test
    void getAllAssets_whenAssetsExist_returnsNormalizedUniqueSortedAssets() {
        AvatarAssetService service = new AvatarAssetService(fileObjectRepository, fileObjectService);
        when(fileObjectRepository.findAllByObjectKeyStartingWith("avatar-assets/")).thenReturn(List.of(
                fileObject("avatar-assets/hair/brown.png"),
                fileObject("avatar-assets/costume/detective.svg"),
                fileObject("avatar-assets/invalid"),
                fileObject("other-assets/person/boy.png")));
        when(fileObjectRepository.findAllByObjectKeyStartingWith("/avatar-assets/")).thenReturn(List.of(
                fileObject("/avatar-assets/person/boy.png"),
                fileObject("/avatar-assets/hair/brown.png")));

        List<AvatarAssetResponse> response = service.getAllAssets();

        assertEquals(3, response.size());
        assertEquals(new AvatarAssetResponse("costume", "detective", "svg"), response.get(0));
        assertEquals(new AvatarAssetResponse("hair", "brown", "png"), response.get(1));
        assertEquals(new AvatarAssetResponse("person", "boy", "png"), response.get(2));
    }

    @DisplayName("Reads avatar asset from normalized object key when it exists")
    @Test
    void getAsset_whenNormalizedObjectKeyExists_returnsStoredFile() {
        AvatarAssetService service = new AvatarAssetService(fileObjectRepository, fileObjectService);
        FileObject fileObject = fileObject("avatar-assets/hair/brown.png");
        byte[] bytes = new byte[] {4, 5, 6};
        FileObjectService.StoredFile storedFile = new FileObjectService.StoredFile(fileObject, "image/png", bytes);
        when(fileObjectService.read("avatar-assets/hair/brown.png")).thenReturn(storedFile);

        FileObjectService.StoredFile response = service.getAsset("hair", "brown.png");

        assertEquals(fileObject, response.fileObject());
        assertArrayEquals(bytes, response.bytes());
        verify(fileObjectService).read("avatar-assets/hair/brown.png");
    }

    @DisplayName("Falls back to leading-slash object key for legacy avatar assets")
    @Test
    void getAsset_whenNormalizedObjectKeyMissing_readsLegacyLeadingSlashObjectKey() {
        AvatarAssetService service = new AvatarAssetService(fileObjectRepository, fileObjectService);
        FileObject fileObject = fileObject("/avatar-assets/hair/brown.png");
        FileObjectService.StoredFile storedFile = new FileObjectService.StoredFile(
                fileObject,
                "image/png",
                new byte[] {7});
        when(fileObjectService.read("avatar-assets/hair/brown.png"))
                .thenThrow(new EntityNotFoundException("missing"));
        when(fileObjectService.read("/avatar-assets/hair/brown.png")).thenReturn(storedFile);

        FileObjectService.StoredFile response = service.getAsset("hair", "brown.png");

        assertEquals(fileObject, response.fileObject());
        verify(fileObjectService).read("avatar-assets/hair/brown.png");
        verify(fileObjectService).read("/avatar-assets/hair/brown.png");
    }

    @DisplayName("Rejects blank asset type before reading storage")
    @Test
    void getAsset_whenAssetTypeIsBlank_rejectsRequestBeforeRead() {
        AvatarAssetService service = new AvatarAssetService(fileObjectRepository, fileObjectService);

        assertThrows(IllegalArgumentException.class, () -> service.getAsset(" ", "brown.png"));

        verifyNoInteractions(fileObjectService);
    }

    @DisplayName("Rejects avatar asset path traversal before reading storage")
    @Test
    void getAsset_whenPathContainsTraversal_rejectsRequestBeforeRead() {
        AvatarAssetService service = new AvatarAssetService(fileObjectRepository, fileObjectService);

        assertThrows(IllegalArgumentException.class, () -> service.getAsset("hair", "../secret.png"));

        verifyNoInteractions(fileObjectService);
    }

    private FileObject fileObject(String objectKey) {
        FileObject fileObject = new FileObject();
        fileObject.setObjectKey(objectKey);
        fileObject.setFileName(objectKey.substring(objectKey.lastIndexOf('/') + 1));
        fileObject.setAccessLevel(ClassroomAccess.PUBLIC);
        return fileObject;
    }
}
