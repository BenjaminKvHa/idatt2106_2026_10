package com.nettdet.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nettdet.backend.domain.FileObject;
import com.nettdet.backend.domain.enums.ClassroomAccess;

/**
 * Repository interface for managing {@link FileObject} metadata.
 * 
 * <p>Provides specialized methods to retrieve file information based on their 
 * unique object keys and access levels. Supports prefix-based searches for 
 * organized object retrieval.
 */
@Repository
public interface FileObjectRepository extends JpaRepository<FileObject, Long> {
    
    /**
     * Finds file metadata by its unique storage object key.
     *
     * @param objectKey the unique S3/storage key
     * @return an {@link Optional} containing the file object if found
     */
    Optional<FileObject> findByObjectKey(String objectKey);

    /**
     * Finds file metadata by its object key and required access level.
     * This ensures that the retrieved file matches the expected visibility context.
     *
     * @param objectKey   the unique storage key
     * @param accessLevel the designated {@link ClassroomAccess} level
     * @return an {@link Optional} containing the file object if a matching pair is found
     */
    Optional<FileObject> findByObjectKeyAndAccessLevel(String objectKey, ClassroomAccess accessLevel);

    /**
     * Retrieves all file objects whose keys begin with the specified prefix.
     * Useful for directory-like traversal or batch operations on specific paths.
     *
     * @param objectKeyPrefix the prefix string to match against keys
     * @return a list of file objects matching the prefix
     */
    java.util.List<FileObject> findAllByObjectKeyStartingWith(String objectKeyPrefix);
}
