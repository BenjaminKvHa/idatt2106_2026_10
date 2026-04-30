package com.nettdet.backend.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nettdet.backend.domain.Account;
import com.nettdet.backend.domain.Classroom;
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

/**
 * Service class responsible for managing {@link FileObject} entities and their physical 
 * storage in S3/MinIO.
 * 
 * <p>Handles the core operations of uploading, reading, and deleting files. 
 * This service ensures that metadata in the database stays synchronized with the 
 * actual file content in the object store.
 */
@Service
public class FileObjectService {

    private final S3Client s3Client;
    private final FileObjectRepository repository;
    private final String bucket;

    /**
     * Data carrier for a stored file's metadata and its raw content.
     */
    public record StoredFile(
            FileObject fileObject,
            String contentType,
            byte[] bytes) {
    }

    public FileObjectService(
            S3Client s3Client,
            FileObjectRepository repository,
            @Value("${s3.bucket}") String bucket) {
        this.s3Client = s3Client;
        this.repository = repository;
        this.bucket = bucket;
    }

    /**
     * Uploads a file to the object store and persists its metadata in the database.
     * 
     * <p>If the database persistence fails, the file is automatically deleted 
     * from the object store to maintain consistency.
     *
     * @param objectKey    the unique key for the file in the object store
     * @param bytes        the raw content of the file
     * @param contentType  the MIME type of the file
     * @param uploadedBy   the account responsible for the upload
     * @param classroom    the optional classroom context
     * @param accessLevel  the {@link ClassroomAccess} level for the file
     * @param fileName     the display name for the file
     * @return the created and persisted {@link FileObject}
     */
    @Transactional
    public FileObject upload(
            String objectKey,
            byte[] bytes,
            String contentType,
            Account uploadedBy,
            Classroom classroom,
            ClassroomAccess accessLevel,
            String fileName) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(objectKey)
                .contentType(contentType)
                .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(bytes));

        FileObject fileObject = new FileObject();
        fileObject.setObjectKey(objectKey);
        fileObject.setUploadedByAccount(uploadedBy);
        fileObject.setClassroom(classroom);
        fileObject.setAccessLevel(accessLevel);
        fileObject.setFileName(fileName);

        try {
            return repository.save(fileObject);
        } catch (RuntimeException exception) {
            s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucket).key(objectKey).build());
            throw exception;
        }
    }

    /**
     * Deletes a file from both the object store and the database.
     * 
     * @param fileObjectId the ID of the file object to delete
     * @throws EntityNotFoundException if the file object is not found in the database
     */
    @Transactional
    public void delete(Long fileObjectId) {
        FileObject fileObject = repository.findById(fileObjectId)
                .orElseThrow(() -> new EntityNotFoundException("File object not found"));

        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(bucket)
                .key(fileObject.getObjectKey())
                .build());

        repository.delete(fileObject);
    }

    /**
     * Retrieves a file's content and metadata by its database ID.
     * 
     * @param fileObjectId the ID of the file object
     * @return a {@link StoredFile} containing bytes and metadata
     * @throws EntityNotFoundException if the file object is not found
     */
    @Transactional(readOnly = true)
    public StoredFile read(Long fileObjectId) {
        FileObject fileObject = repository.findById(fileObjectId)
                .orElseThrow(() -> new EntityNotFoundException("File object not found"));

        ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(bucket)
                .key(fileObject.getObjectKey())
                .build());

        String contentType = response.response().contentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        return new StoredFile(fileObject, contentType, response.asByteArray());
    }

    /**
     * Retrieves a file's content and metadata by its object key.
     * 
     * @param objectKey the unique key of the file in the object store
     * @return a {@link StoredFile} containing bytes and metadata
     * @throws EntityNotFoundException if the file object metadata is not found
     */
    @Transactional(readOnly = true)
    public StoredFile read(String objectKey) {
        FileObject fileObject = repository.findByObjectKey(objectKey)
                .orElseThrow(() -> new EntityNotFoundException("File object not found"));

        ResponseBytes<GetObjectResponse> response = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                .bucket(bucket)
                .key(fileObject.getObjectKey())
                .build());

        String contentType = response.response().contentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "application/octet-stream";
        }

        return new StoredFile(fileObject, contentType, response.asByteArray());
    }

    /**
     * Gets an existing file object by its object key, or creates a new one if it doesn't exist.
     * This is useful for production scenarios where files are pre-uploaded to MinIO and we need
     * to reference them without relying on deterministic IDs.
     *
     * @param objectKey the S3/MinIO object key
     * @param contentType the content type of the file (used if creating new)
     * @param uploadedBy the account uploading the file (can be null for system uploads)
     * @param classroom the classroom associated with the file (can be null)
     * @param accessLevel the access level for the file
     * @param fileName the original file name
     * @return a record containing the file object and whether it was newly created
     */
    @Transactional
    public FileObjectResult getOrCreateFileObject(
            String objectKey,
            String contentType,
            Account uploadedBy,
            Classroom classroom,
            ClassroomAccess accessLevel,
            String fileName) {

        return repository.findByObjectKey(objectKey)
                .map(fileObject -> new FileObjectResult(fileObject, false))
                .orElseGet(() -> {
                    byte[] bytes = s3Client.getObjectAsBytes(GetObjectRequest.builder()
                            .bucket(bucket)
                            .key(objectKey)
                            .build()).asByteArray();

                    FileObject fileObject = upload(
                            objectKey,
                            bytes,
                            contentType,
                            uploadedBy,
                            classroom,
                            accessLevel,
                            fileName);
                    return new FileObjectResult(fileObject, true);
                });
    }

    public record FileObjectResult(FileObject fileObject, boolean newlyCreated) {}

    @Transactional(readOnly = true)
    public java.util.Optional<FileObject> findByObjectKey(String objectKey) {
        return repository.findByObjectKey(objectKey);
    }
}
