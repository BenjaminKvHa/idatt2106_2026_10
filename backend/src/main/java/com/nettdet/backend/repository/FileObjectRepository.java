package com.nettdet.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nettdet.backend.domain.FileObject;
import com.nettdet.backend.domain.enums.ClassroomAccess;

@Repository
public interface FileObjectRepository extends JpaRepository<FileObject, Long> {
    Optional<FileObject> findByObjectKey(String objectKey);

    Optional<FileObject> findByObjectKeyAndAccessLevel(String objectKey, ClassroomAccess accessLevel);

    java.util.List<FileObject> findAllByObjectKeyStartingWith(String objectKeyPrefix);
}
