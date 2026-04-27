package com.nettdet.backend.service;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nettdet.backend.domain.FileObject;
import com.nettdet.backend.domain.enums.ClassroomAccess;
import com.nettdet.backend.repository.FileObjectRepository;

import jakarta.persistence.EntityNotFoundException;

@ExtendWith(MockitoExtension.class)
class AvatarServiceTest {

    @Mock
    private FileObjectService fileObjectService;

    @Mock
    private FileObjectRepository fileObjectRepository;

    private AvatarService service;
    private byte[] tinyPng;

    @BeforeEach
    void setUp() throws IOException {
        service = new AvatarService(fileObjectService, fileObjectRepository);
        tinyPng = makePng();
    }

    @DisplayName("Returns the existing avatar without uploading when one already exists for the avatar key")
    @Test
    void getOrCreateAvatar_whenAvatarAlreadyExists_returnsExistingFileObject() {
        FileObject existing = fileObject(7L, "avatar-images/p_h_c.png");
        when(fileObjectRepository.findByObjectKey("avatar-images/p_h_c.png"))
                .thenReturn(Optional.of(existing));

        FileObject result = service.getOrCreateAvatar("p_h_c");

        assertSame(existing, result);
        verify(fileObjectService, never()).upload(
                any(), any(), any(), any(), any(), any(), any());
    }

    @DisplayName("Generates and uploads a new avatar when none exists for the avatar key")
    @Test
    void getOrCreateAvatar_whenAvatarMissing_composesAssetsAndUploadsPng() {
        when(fileObjectRepository.findByObjectKey("avatar-images/p_h_c.png"))
                .thenReturn(Optional.empty());
        FileObject personAsset = fileObject(1L, "avatar-assets/person/p.png");
        FileObject hairAsset = fileObject(2L, "avatar-assets/hair/h.png");
        FileObject costumeAsset = fileObject(3L, "avatar-assets/costume/c.png");
        when(fileObjectRepository.findByObjectKey("avatar-assets/person/p.png"))
                .thenReturn(Optional.of(personAsset));
        when(fileObjectRepository.findByObjectKey("avatar-assets/hair/h.png"))
                .thenReturn(Optional.of(hairAsset));
        when(fileObjectRepository.findByObjectKey("avatar-assets/costume/c.png"))
                .thenReturn(Optional.of(costumeAsset));
        FileObjectService.StoredFile stored = new FileObjectService.StoredFile(personAsset, "image/png", tinyPng);
        when(fileObjectService.read("avatar-assets/person/p.png")).thenReturn(stored);
        when(fileObjectService.read("avatar-assets/hair/h.png")).thenReturn(stored);
        when(fileObjectService.read("avatar-assets/costume/c.png")).thenReturn(stored);

        FileObject uploaded = fileObject(99L, "avatar-images/p_h_c.png");
        when(fileObjectService.upload(
                eq("avatar-images/p_h_c.png"),
                any(byte[].class),
                eq("image/png"),
                eq(null),
                eq(null),
                eq(ClassroomAccess.PUBLIC),
                eq("p_h_c.png")))
                .thenReturn(uploaded);

        FileObject result = service.getOrCreateAvatar("p_h_c");

        assertSame(uploaded, result);
    }

    @DisplayName("Throws when one of the requested asset parts is not present")
    @Test
    void getOrCreateAvatar_whenAssetMissing_throwsEntityNotFound() {
        when(fileObjectRepository.findByObjectKey(any(String.class))).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.getOrCreateAvatar("p_h_c"));
        verify(fileObjectService, never()).upload(
                any(), any(), any(), any(), any(), any(), any());
    }

    @DisplayName("Rejects blank avatar selectors")
    @Test
    void getOrCreateAvatar_whenAvatarIsBlank_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.getOrCreateAvatar("   "));
    }

    @DisplayName("Rejects null avatar selectors")
    @Test
    void getOrCreateAvatar_whenAvatarIsNull_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.getOrCreateAvatar(null));
    }

    @DisplayName("Rejects avatar selectors that do not match person_hair_costume")
    @Test
    void getOrCreateAvatar_whenAvatarHasFewerThanThreeParts_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.getOrCreateAvatar("only_two"));
    }

    @DisplayName("Rejects avatar selectors with an empty middle segment")
    @Test
    void getOrCreateAvatar_whenAvatarHasBlankSegment_throwsIllegalArgument() {
        assertThrows(IllegalArgumentException.class, () -> service.getOrCreateAvatar("p__c"));
    }

    private FileObject fileObject(Long id, String key) {
        FileObject fileObject = new FileObject();
        fileObject.setId(id);
        fileObject.setObjectKey(key);
        fileObject.setFileName(key);
        fileObject.setAccessLevel(ClassroomAccess.PUBLIC);
        return fileObject;
    }

    private byte[] makePng() throws IOException {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        image.setRGB(0, 0, 0xFFFF0000);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        return out.toByteArray();
    }
}
