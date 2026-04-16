package com.nettdet.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nettdet.backend.domain.Classroom;

public interface ClassroomRepository extends JpaRepository<Classroom, String> {
}
