package com.nettdet.backend.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * {@code ChapterSeeder} is a Spring Boot {@link ApplicationRunner} responsible for
 * ensuring that the core educational chapters exist in the database.
 * <p>
 * This component uses {@link JdbcTemplate} to perform raw SQL {@code UPSERT} operations,
 * ensuring that chapter definitions (ID, name, and completion requirements) are
 * synchronized with the application's configuration on startup.
 * </p>
 * <p>
 * After seeding the fixed set of chapters, the seeder manually synchronizes the
 * {@code chapter_id_seq} database sequence to prevent ID collision during subsequent
 * runtime insertions.
 * </p>
 * <p>
 * Execution is marked with {@code @Order(2)}, following {@link AvatarAssetInitializer},
 * to maintain a predictable bootstrapping sequence.
 * </p>
 */
@Component
@Order(2)
public class ChapterSeeder implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(ChapterSeeder.class);

    private final JdbcTemplate jdbcTemplate;

    /**
     * Constructs a new {@code ChapterSeeder} with the required JDBC template.
     *
     * @param jdbcTemplate the JDBC template used for direct database interaction
     */
    public ChapterSeeder(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Executes the chapter seeding logic within a transaction.
     * <p>
     * This method upserts the predefined chapters and resets the auto-increment
     * sequence for the {@code chapter} table to the maximum ID present.
     * </p>
     *
     * @param args incoming application arguments (unused)
     */
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

    /**
     * Performs an idempotent upsert of a single chapter record.
     * <p>
     * Uses the {@code ON CONFLICT (id)} clause to update the chapter name and
     * completion requirements if a record with the given ID already exists.
     * </p>
     *
     * @param id the unique identifier for the chapter
     * @param name the display name of the chapter
     * @param runsNeededForCompletion the number of successful tasks required to complete the chapter
     */
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
