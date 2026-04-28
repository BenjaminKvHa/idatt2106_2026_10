package com.nettdet.backend.config;

import java.time.LocalDateTime;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.nettdet.backend.domain.PupilClassroomMembership;
import com.nettdet.backend.repository.PupilClassroomMembershipRepository;

@Component
@Order(5)
public class WeeklyMysterySeeder implements ApplicationRunner {

    private static final Logger logger = LoggerFactory.getLogger(WeeklyMysterySeeder.class);

    private record Seed(long fileObjectId, String comment, boolean approved, int daysAgo) {}

    // Reuses task-asset file_object ids seeded by TaskSeeder (1..5).
    // weekly_mystery.file_object_id is UNIQUE, so each id is used once.
    private static final List<Seed> SEEDS = List.of(
            new Seed(1L, "Bildet ser veldig ekte ut, men jeg er litt usikker på øynene.", true, 1),
            new Seed(2L, "Folkene i mengden ser nesten identiske ut – ser KI-generert ut.", true, 2),
            new Seed(3L, "Skarpheten på jorda er for perfekt. Jeg tror dette er KI.", false, 0),
            new Seed(4L, "Blomstene har for glatte teksturer.", true, 9),
            new Seed(5L, "Trærne ser duplisert ut og himmelen er rar.", true, 16));

    private final PupilClassroomMembershipRepository pupilClassroomMembershipRepository;
    private final JdbcTemplate jdbcTemplate;

    public WeeklyMysterySeeder(PupilClassroomMembershipRepository pupilClassroomMembershipRepository,
                               JdbcTemplate jdbcTemplate) {
        this.pupilClassroomMembershipRepository = pupilClassroomMembershipRepository;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        List<PupilClassroomMembership> confirmedMemberships =
                pupilClassroomMembershipRepository.findAllByMembershipConfirmedByTeacherTrue();
        if (confirmedMemberships.isEmpty()) {
            logger.info("Weekly mystery seeding skipped (no confirmed pupil memberships exist yet)");
            return;
        }

        PupilClassroomMembership membership = confirmedMemberships.get(0);
        String classroomCode = membership.getClassroom().getClassroomCode();
        String pupilEmail = membership.getAccount().getEmail();

        Integer existingForClass = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM weekly_mystery WHERE classroom_code = ?",
                Integer.class, classroomCode);
        if (existingForClass != null && existingForClass > 0) {
            logger.info("Weekly mystery seeding skipped (classroom {} already has rows)", classroomCode);
            return;
        }

        logger.info("Seeding weekly mysteries for pupil {} in classroom {}", pupilEmail, classroomCode);

        int seeded = 0;
        for (Seed seed : SEEDS) {
            try {
                Integer fileExists = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM file_object WHERE id = ?", Integer.class, seed.fileObjectId());
                if (fileExists == null || fileExists == 0) {
                    logger.warn("Weekly mystery seed file_object id {} not found, skipping", seed.fileObjectId());
                    continue;
                }
                Integer alreadyLinked = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM weekly_mystery WHERE file_object_id = ?",
                        Integer.class, seed.fileObjectId());
                if (alreadyLinked != null && alreadyLinked > 0) {
                    logger.warn("file_object id {} already linked to a weekly_mystery, skipping",
                            seed.fileObjectId());
                    continue;
                }

                LocalDateTime now = LocalDateTime.now();
                LocalDateTime createdAt = now.minusDays(seed.daysAgo());
                LocalDateTime approvedAt = seed.approved() ? createdAt.plusHours(2) : null;

                jdbcTemplate.update(
                        "INSERT INTO weekly_mystery "
                                + "(file_object_id, classroom_code, account_email, comment, approved, approved_at, created_at) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?)",
                        seed.fileObjectId(),
                        classroomCode,
                        pupilEmail,
                        seed.comment(),
                        seed.approved(),
                        approvedAt,
                        createdAt);
                seeded++;
            } catch (Exception e) {
                logger.warn("Failed to seed weekly mystery for file_object {}: {}",
                        seed.fileObjectId(), e.getMessage());
            }
        }
        logger.info("Weekly mystery seeding completed ({} rows for classroom {})", seeded, classroomCode);
    }
}
