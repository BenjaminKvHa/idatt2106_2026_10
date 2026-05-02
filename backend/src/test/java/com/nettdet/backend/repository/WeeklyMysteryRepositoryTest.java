package com.nettdet.backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.nettdet.backend.domain.WeeklyMystery;
import com.nettdet.backend.support.PostgresDataJpaTest;

@PostgresDataJpaTest
class WeeklyMysteryRepositoryTest {

    @Autowired
    private WeeklyMysteryRepository weeklyMysteryRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DisplayName("Finds pending weekly mysteries for one classroom ordered newest first")
    @Test
    void findAllByClassroomClassroomCodeAndApprovedFalseOrderByCreatedAtDesc_whenMultipleMysteriesExist_returnsPendingForClassroomNewestFirst() {
        insertBaseData();
        Long olderFileId = insertFile("older.png", "ABCABC", "ONLY_TEACHERS_IN_CLASS");
        Long approvedFileId = insertFile("approved.png", "ABCABC", "ONLY_CLASS");
        Long otherFileId = insertFile("other.png", "XYZXYZ", "ONLY_TEACHERS_IN_CLASS");
        insertMystery(olderFileId, "ABCABC", "pupil@example.com", "Older pending", false,
                LocalDateTime.of(2026, 4, 23, 9, 0));
        insertMystery(approvedFileId, "ABCABC", "pupil@example.com", "Approved", true,
                LocalDateTime.of(2026, 4, 23, 12, 0));
        insertMystery(otherFileId, "XYZXYZ", "other@example.com", "Other classroom", false,
                LocalDateTime.of(2026, 4, 23, 11, 0));
        Long newerFileId = insertFile("newer.png", "ABCABC", "ONLY_TEACHERS_IN_CLASS");
        insertMystery(newerFileId, "ABCABC", "pupil@example.com", "Newer pending", false,
                LocalDateTime.of(2026, 4, 23, 10, 0));

        List<WeeklyMystery> mysteries = weeklyMysteryRepository
                .findAllByClassroomClassroomCodeAndApprovedFalseOrderByCreatedAtDesc("ABCABC");

        assertEquals(List.of("Newer pending", "Older pending"), mysteries.stream()
                .map(WeeklyMystery::getComment)
                .toList());
        assertEquals("Pupil Name", mysteries.getFirst().getPupilClassroomMembership().getAccount().getLegalName());
    }

    @DisplayName("Finds approved weekly mysteries for one classroom ordered by approval time newest first")
    @Test
    void findAllByClassroomClassroomCodeAndApprovedTrueOrderByApprovedAtDescCreatedAtDesc_whenApprovedMysteriesExist_returnsApprovedForClassroomNewestFirst() {
        insertBaseData();
        Long olderFileId = insertFile("older-approved.png", "ABCABC", "ONLY_CLASS");
        Long pendingFileId = insertFile("pending.png", "ABCABC", "ONLY_TEACHERS_IN_CLASS");
        Long newerFileId = insertFile("newer-approved.png", "ABCABC", "ONLY_CLASS");
        LocalDateTime olderApprovedAt = LocalDateTime.of(2026, 4, 25, 12, 0);
        LocalDateTime newerApprovedAt = LocalDateTime.of(2026, 4, 26, 8, 0);
        insertMystery(olderFileId, "ABCABC", "pupil@example.com", "Older approved", true,
                LocalDateTime.of(2026, 4, 23, 12, 0), olderApprovedAt);
        insertMystery(pendingFileId, "ABCABC", "pupil@example.com", "Pending", false,
                LocalDateTime.of(2026, 4, 23, 10, 0));
        insertMystery(newerFileId, "ABCABC", "pupil@example.com", "Newer approved", true,
                LocalDateTime.of(2026, 4, 23, 9, 0), newerApprovedAt);

        List<WeeklyMystery> mysteries = weeklyMysteryRepository
                .findAllByClassroomClassroomCodeAndApprovedTrueOrderByApprovedAtDescCreatedAtDesc("ABCABC");

        assertEquals(List.of("Newer approved", "Older approved"), mysteries.stream()
                .map(WeeklyMystery::getComment)
                .toList());
        assertEquals(newerApprovedAt, mysteries.getFirst().getApprovedAt());
        assertEquals("Pupil Name", mysteries.getFirst().getPupilClassroomMembership().getAccount().getLegalName());
    }

    @DisplayName("New weekly mystery rows default to pending when approved is omitted")
    @Test
    void weeklyMysteryDefault_whenApprovedIsOmitted_defaultsToFalse() {
        insertBaseData();
        Long fileId = insertFile("default-pending.png", "ABCABC", "ONLY_TEACHERS_IN_CLASS");
        jdbcTemplate.update(
                "INSERT INTO weekly_mystery (file_object_id, classroom_code, account_email, comment, alt_text) VALUES (?, ?, ?, ?, ?)",
                fileId,
                "ABCABC",
                "pupil@example.com",
                "Default pending",
                "Alt text");

        WeeklyMystery mystery = weeklyMysteryRepository
                .findAllByClassroomClassroomCodeAndApprovedFalseOrderByCreatedAtDesc("ABCABC")
                .getFirst();

        assertFalse(mystery.isApproved());
        assertNull(mystery.getApprovedAt());
    }

    private void insertBaseData() {
        insertClassroom("ABCABC", "Main classroom");
        insertClassroom("XYZXYZ", "Other classroom");
        insertPupilAccount("pupil@example.com", "Pupil Name");
        insertPupilAccount("other@example.com", "Other Pupil");
        insertMembership("pupil@example.com", "ABCABC", "detektiv1");
        insertMembership("other@example.com", "XYZXYZ", "detektiv2");
    }

    private void insertClassroom(String classroomCode, String classroomName) {
        jdbcTemplate.update(
                "INSERT INTO classroom (classroom_code, classroom_name, classroom_description) VALUES (?, ?, ?)",
                classroomCode,
                classroomName,
                "Weekly mystery repository test classroom");
    }

    private void insertPupilAccount(String email, String legalName) {
        jdbcTemplate.update(
                "INSERT INTO account (email, password_data, legal_name, account_role) VALUES (?, ?, ?, ?::account_role_enum)",
                email,
                "encoded-password",
                legalName,
                "PUPIL");
    }

    private void insertMembership(String email, String classroomCode, String username) {
        jdbcTemplate.update(
                "INSERT INTO pupil_classroom_membership (account_email, classroom_code, username, membership_confirmed_by_teacher) VALUES (?, ?, ?, true)",
                email,
                classroomCode,
                username);
    }

    private Long insertFile(String fileName, String classroomCode, String accessLevel) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO file_object (object_key, classroom_code, access_level, file_name) "
                        + "VALUES (?, ?, ?::classroom_access_enum, ?) RETURNING id",
                Long.class,
                "weekly/" + fileName,
                classroomCode,
                accessLevel,
                fileName);
    }

    private void insertMystery(Long fileId,
                               String classroomCode,
                               String pupilEmail,
                               String comment,
                               boolean approved,
                               LocalDateTime createdAt) {
        insertMystery(fileId, classroomCode, pupilEmail, comment, approved, createdAt, null);
    }

    private void insertMystery(Long fileId,
                               String classroomCode,
                               String pupilEmail,
                               String comment,
                               boolean approved,
                               LocalDateTime createdAt,
                               LocalDateTime approvedAt) {
        jdbcTemplate.update(
                "INSERT INTO weekly_mystery (file_object_id, classroom_code, account_email, comment, alt_text, approved, created_at, approved_at) "
                        + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                fileId,
                classroomCode,
                pupilEmail,
                comment,
                "Alt for " + comment,
                approved,
                createdAt,
                approvedAt);
    }
}
