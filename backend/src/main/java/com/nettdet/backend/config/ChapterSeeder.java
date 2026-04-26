package com.nettdet.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(2)
public class ChapterSeeder implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(ChapterSeeder.class);

    private final JdbcTemplate jdbcTemplate;

    public ChapterSeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        upsertChapter(1L, "Nyhetskvartalet", 3);
        upsertChapter(2L, "Fotografen", 3);
        upsertChapter(3L, "Datasenteret", 5);
        jdbcTemplate.execute(
                "SELECT setval('chapter_id_seq', "
                        + "GREATEST(COALESCE((SELECT MAX(id) FROM chapter), 0), 3), true)");
        logger.info("Chapter seeding completed");
    }

    private void upsertChapter(long id, String name, int runsNeededForCompletion) {
        jdbcTemplate.update(
                "INSERT INTO chapter (id, chapter_name, successful_runs_needed_for_completion) "
                        + "VALUES (?, ?, ?) "
                        + "ON CONFLICT (id) DO UPDATE SET "
                        + "chapter_name = EXCLUDED.chapter_name, "
                        + "successful_runs_needed_for_completion = EXCLUDED.successful_runs_needed_for_completion",
                id, name, runsNeededForCompletion);
    }
}
