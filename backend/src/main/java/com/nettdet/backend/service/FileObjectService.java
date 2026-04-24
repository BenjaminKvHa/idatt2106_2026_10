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

@Service
public class FileObjectService {

    private final S3Client s3Client;
    private final FileObjectRepository repository;
    private final String bucket;

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
