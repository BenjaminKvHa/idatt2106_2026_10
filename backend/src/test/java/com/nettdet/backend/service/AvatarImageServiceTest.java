package com.nettdet.backend.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Optional;

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
class AvatarImageServiceTest {

    @Mock
    private FileObjectRepository fileObjectRepository;

    @Mock
    private FileObjectService fileObjectService;

    @DisplayName("Returns public avatar image bytes from the avatar image folder")
    @Test
    void getAvatarImage_whenPublicAvatarExists_returnsStoredFile() {
        AvatarImageService service = new AvatarImageService(fileObjectRepository, fileObjectService);
        FileObject fileObject = fileObject("avatar-images/boy_black_coat.png");
        byte[] bytes = new byte[] {1, 2, 3};
        FileObjectService.StoredFile storedFile = new FileObjectService.StoredFile(fileObject, "image/png", bytes);
        when(fileObjectRepository.findByObjectKeyAndAccessLevel(
                "avatar-images/boy_black_coat.png",
                ClassroomAccess.PUBLIC))
                .thenReturn(Optional.of(fileObject));
        when(fileObjectService.read("avatar-images/boy_black_coat.png")).thenReturn(storedFile);

        FileObjectService.StoredFile response = service.getAvatarImage("boy_black_coat.png");

        assertEquals(fileObject, response.fileObject());
        assertEquals("image/png", response.contentType());
        assertArrayEquals(bytes, response.bytes());
        verify(fileObjectService).read("avatar-images/boy_black_coat.png");
    }

    @DisplayName("Rejects blank avatar image file name")
    @Test
    void getAvatarImage_whenFileNameIsBlank_rejectsRequestBeforeLookup() {
        AvatarImageService service = new AvatarImageService(fileObjectRepository, fileObjectService);

        assertThrows(IllegalArgumentException.class, () -> service.getAvatarImage(" "));

        verifyNoInteractions(fileObjectRepository, fileObjectService);
    }

    @DisplayName("Rejects avatar image path traversal")
    @Test
    void getAvatarImage_whenFileNameContainsPathTraversal_rejectsRequestBeforeLookup() {
        AvatarImageService service = new AvatarImageService(fileObjectRepository, fileObjectService);

        assertThrows(IllegalArgumentException.class, () -> service.getAvatarImage("../secret.png"));

        verifyNoInteractions(fileObjectRepository, fileObjectService);
    }

    @DisplayName("Rejects non-png avatar image file names")
    @Test
    void getAvatarImage_whenFileNameIsNotPng_rejectsRequestBeforeLookup() {
        AvatarImageService service = new AvatarImageService(fileObjectRepository, fileObjectService);

        assertThrows(IllegalArgumentException.class, () -> service.getAvatarImage("avatar.jpg"));

        verifyNoInteractions(fileObjectRepository, fileObjectService);
    }

    @DisplayName("Does not serve avatar image unless the database marks it public")
    @Test
    void getAvatarImage_whenAvatarIsMissingOrNotPublic_throwsNotFound() {
        AvatarImageService service = new AvatarImageService(fileObjectRepository, fileObjectService);
        when(fileObjectRepository.findByObjectKeyAndAccessLevel(
                "avatar-images/private_avatar.png",
                ClassroomAccess.PUBLIC))
                .thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.getAvatarImage("private_avatar.png"));

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
