package com.nettdet.backend.config;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.DefaultApplicationArguments;

import com.nettdet.backend.domain.FileObject;
import com.nettdet.backend.domain.enums.ClassroomAccess;
import com.nettdet.backend.repository.FileObjectRepository;
import com.nettdet.backend.service.FileObjectService;

@ExtendWith(MockitoExtension.class)
class MedalAssetInitializerTest {

    @TempDir
    private Path tempDir;

    @Mock
    private FileObjectRepository fileObjectRepository;

    @Mock
    private FileObjectService fileObjectService;

    @DisplayName("Skips medal seeding when asset directory is missing")
    @Test
    void run_whenAssetDirectoryIsMissing_doesNotReadOrUploadAssets() {
        MedalAssetInitializer initializer = new MedalAssetInitializer(
                fileObjectRepository,
                fileObjectService,
                List.of(tempDir.resolve("missing").toString()));

        initializer.run(new DefaultApplicationArguments());

        verifyNoInteractions(fileObjectRepository, fileObjectService);
    }

    @DisplayName("Seeds new medal png assets as public file objects")
    @Test
    void run_whenPngAssetExists_uploadsPublicMedalAsset() throws Exception {
        Path asset = tempDir.resolve("chapter-1.png");
        Files.write(asset, new byte[] {1, 2, 3});
        when(fileObjectRepository.findByObjectKey("medal-assets/chapter-1.png"))
                .thenReturn(Optional.empty());

        MedalAssetInitializer initializer = new MedalAssetInitializer(
                fileObjectRepository,
                fileObjectService,
                List.of(tempDir.toString()));

        initializer.run(new DefaultApplicationArguments());

        verify(fileObjectService).upload(
                eq("medal-assets/chapter-1.png"),
                eq(new byte[] {1, 2, 3}),
                eq("image/png"),
                eq(null),
                eq(null),
                eq(ClassroomAccess.PUBLIC),
                eq("chapter-1.png"));
    }

    @DisplayName("Does not re-upload existing medal asset object keys")
    @Test
    void run_whenObjectKeyAlreadyExists_skipsUpload() throws Exception {
        Path asset = tempDir.resolve("locked.png");
        Files.write(asset, new byte[] {4, 5, 6});
        when(fileObjectRepository.findByObjectKey("medal-assets/locked.png"))
                .thenReturn(Optional.of(new FileObject()));

        MedalAssetInitializer initializer = new MedalAssetInitializer(
                fileObjectRepository,
                fileObjectService,
                List.of(tempDir.toString()));

        initializer.run(new DefaultApplicationArguments());

        verify(fileObjectService, never()).upload(
                eq("medal-assets/locked.png"),
                eq(new byte[] {4, 5, 6}),
                eq("image/png"),
                eq(null),
                eq(null),
                eq(ClassroomAccess.PUBLIC),
                eq("locked.png"));
    }
}
