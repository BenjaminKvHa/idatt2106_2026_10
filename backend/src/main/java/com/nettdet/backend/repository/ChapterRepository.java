package com.nettdet.backend.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nettdet.backend.domain.Chapter;

public interface ChapterRepository extends JpaRepository<Chapter, Long> {

    List<Chapter> findAllByOrderByIdAsc();
}
