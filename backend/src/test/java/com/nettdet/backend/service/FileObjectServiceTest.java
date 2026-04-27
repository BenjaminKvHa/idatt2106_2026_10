package com.nettdet.backend.service;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.nettdet.backend.domain.FileObject;
import com.nettdet.backend.domain.enums.ClassroomAccess;
import com.nettdet.backend.repository.FileObjectRepository;

import jakarta.persistence.EntityNotFoundException;
import software.amazon.awssdk.core.ResponseBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

@ExtendWith(MockitoExtension.class)
class FileObjectServiceTest {

    private static final String BUCKET = "test-bucket";

    @Mock
    private S3Client s3Client;

    @Mock
    private FileObjectRepository repository;

    private FileObjectService service;

    @BeforeEach
    void setUp() {
        service = new FileObjectService(s3Client, repository, BUCKET);
    }

    @DisplayName("Upload sends bytes to S3 and persists the file object metadata")
    @Test
    void upload_whenInputsValid_putsToS3AndSavesEntity() {
        when(repository.save(any(FileObject.class))).thenAnswer(inv -> inv.getArgument(0));

        FileObject saved = service.upload(
                "key.png",
                new byte[] {1, 2, 3},
                "image/png",
                null,
                null,
                ClassroomAccess.PUBLIC,
                "name.png");

        ArgumentCaptor<PutObjectRequest> putCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(putCaptor.capture(), any(RequestBody.class));
        PutObjectRequest put = putCaptor.getValue();
        assertEquals(BUCKET, put.bucket());
        assertEquals("key.png", put.key());
        assertEquals("image/png", put.contentType());

        ArgumentCaptor<FileObject> entityCaptor = ArgumentCaptor.forClass(FileObject.class);
        verify(repository).save(entityCaptor.capture());
        FileObject entity = entityCaptor.getValue();
        assertSame(entity, saved);
        assertEquals("key.png", entity.getObjectKey());
        assertEquals("name.png", entity.getFileName());
        assertEquals(ClassroomAccess.PUBLIC, entity.getAccessLevel());
    }

    @DisplayName("Upload deletes the S3 object when the database save fails")
    @Test
    void upload_whenRepositorySaveFails_deletesUploadedS3Object() {
        when(repository.save(any(FileObject.class))).thenThrow(new RuntimeException("db down"));

        assertThrows(RuntimeException.class, () -> service.upload(
                "key.png", new byte[] {1}, "image/png",
                null, null, ClassroomAccess.PUBLIC, "name.png"));

        verify(s3Client).putObject(any(PutObjectRequest.class), any(RequestBody.class));
        ArgumentCaptor<DeleteObjectRequest> deleteCaptor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(deleteCaptor.capture());
        assertEquals("key.png", deleteCaptor.getValue().key());
    }

    @DisplayName("Delete removes the S3 object and the entity")
    @Test
    void delete_whenFileExists_deletesFromS3AndRepository() {
        FileObject fileObject = fileObject(7L, "k.png");
        when(repository.findById(7L)).thenReturn(Optional.of(fileObject));

        service.delete(7L);

        ArgumentCaptor<DeleteObjectRequest> captor = ArgumentCaptor.forClass(DeleteObjectRequest.class);
        verify(s3Client).deleteObject(captor.capture());
        assertEquals("k.png", captor.getValue().key());
        verify(repository).delete(fileObject);
    }

    @DisplayName("Delete throws EntityNotFoundException when the file object does not exist")
    @Test
    void delete_whenFileMissing_throwsEntityNotFound() {
        when(repository.findById(404L)).thenReturn(Optional.empty());

        assertThrows(EntityNotFoundException.class, () -> service.delete(404L));
        verify(s3Client, never()).deleteObject(any(DeleteObjectRequest.class));
    }

    @DisplayName("Read by id returns the stored bytes and content type")
    @Test
    void readById_whenFileExists_returnsStoredBytesAndContentType() {
        FileObject fileObject = fileObject(8L, "k.png");
        when(repository.findById(8L)).thenReturn(Optional.of(fileObject));
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(responseBytes("image/png", new byte[] {9}));

        FileObjectService.StoredFile result = service.read(8L);

        assertSame(fileObject, result.fileObject());
        assertEquals("image/png", result.contentType());
        assertArrayEquals(new byte[] {9}, result.bytes());
    }

    @DisplayName("Read defaults to application/octet-stream when S3 returns no content type")
    @Test
    void readById_whenContentTypeMissing_defaultsToOctetStream() {
        FileObject fileObject = fileObject(9L, "k.bin");
        when(repository.findById(9L)).thenReturn(Optional.of(fileObject));
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(responseBytes(null, new byte[] {0}));

        FileObjectService.StoredFile result = service.read(9L);

        assertEquals("application/octet-stream", result.contentType());
    }

    @DisplayName("Read by object key returns the stored bytes when the key resolves")
    @Test
    void readByKey_whenFileExists_returnsStoredBytes() {
        FileObject fileObject = fileObject(10L, "k.png");
        when(repository.findByObjectKey("k.png")).thenReturn(Optional.of(fileObject));
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(responseBytes("image/png", new byte[] {11}));

        FileObjectService.StoredFile result = service.read("k.png");

        assertSame(fileObject, result.fileObject());
        assertArrayEquals(new byte[] {11}, result.bytes());
    }

    @DisplayName("getOrCreateFileObject returns the existing entity without uploading when found")
    @Test
    void getOrCreateFileObject_whenAlreadyExists_returnsExistingNotNewlyCreated() {
        FileObject existing = fileObject(12L, "k.png");
        when(repository.findByObjectKey("k.png")).thenReturn(Optional.of(existing));

        FileObjectService.FileObjectResult result = service.getOrCreateFileObject(
                "k.png", "image/png", null, null, ClassroomAccess.PUBLIC, "k.png");

        assertSame(existing, result.fileObject());
        assertFalse(result.newlyCreated());
        verify(s3Client, never()).getObjectAsBytes(any(GetObjectRequest.class));
        verify(s3Client, never()).putObject(any(PutObjectRequest.class), any(RequestBody.class));
    }

    @DisplayName("getOrCreateFileObject downloads the S3 object and uploads a new entity when missing")
    @Test
    void getOrCreateFileObject_whenMissing_downloadsAndUploads() {
        when(repository.findByObjectKey("k.png")).thenReturn(Optional.empty());
        when(s3Client.getObjectAsBytes(any(GetObjectRequest.class)))
                .thenReturn(responseBytes("image/png", new byte[] {77}));
        when(repository.save(any(FileObject.class))).thenAnswer(inv -> inv.getArgument(0));

        FileObjectService.FileObjectResult result = service.getOrCreateFileObject(
                "k.png", "image/png", null, null, ClassroomAccess.PUBLIC, "k.png");

        assertTrue(result.newlyCreated());
        ArgumentCaptor<PutObjectRequest> putCaptor = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(s3Client).putObject(putCaptor.capture(), any(RequestBody.class));
        assertEquals("k.png", putCaptor.getValue().key());
    }

    @DisplayName("findByObjectKey delegates to the repository")
    @Test
    void findByObjectKey_delegatesToRepository() {
        FileObject fileObject = fileObject(13L, "k.png");
        when(repository.findByObjectKey("k.png")).thenReturn(Optional.of(fileObject));

        Optional<FileObject> result = service.findByObjectKey("k.png");

        assertTrue(result.isPresent());
        assertSame(fileObject, result.get());
    }

    private FileObject fileObject(Long id, String key) {
        FileObject fileObject = new FileObject();
        fileObject.setId(id);
        fileObject.setObjectKey(key);
        fileObject.setFileName(key);
        fileObject.setAccessLevel(ClassroomAccess.PUBLIC);
        return fileObject;
    }

    private ResponseBytes<GetObjectResponse> responseBytes(String contentType, byte[] bytes) {
        GetObjectResponse response = GetObjectResponse.builder()
                .contentType(contentType)
                .build();
        return ResponseBytes.fromByteArray(response, bytes);
    }
}
