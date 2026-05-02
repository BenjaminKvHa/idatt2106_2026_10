package com.nettdet.backend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;

import com.nettdet.backend.domain.AccountClassroomBridgeTeacher;
import com.nettdet.backend.domain.PupilClassroomMembership;
import com.nettdet.backend.support.PostgresDataJpaTest;

@PostgresDataJpaTest
class ClassroomRepositoryTest {

    @Autowired
    private AccountClassroomBridgeTeacherRepository teacherBridgeRepository;

    @Autowired
    private PupilClassroomMembershipRepository membershipRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @DisplayName("Finds teacher bridge membership and counts teachers per classroom")
    @Test
    void teacherBridgeQueries_whenTeachersAreBridged_returnMembershipAndCounts() {
        insertClassroomData();
        insertTeacherBridge("teacher@example.com", "ABCABC");
        insertTeacherBridge("other.teacher@example.com", "ABCABC");
        insertTeacherBridge("outside.teacher@example.com", "XYZXYZ");

        boolean teacherIsInClassroom = teacherBridgeRepository
                .existsByIdAccountEmailAndIdClassroomCode("teacher@example.com", "ABCABC");
        long teacherCount = teacherBridgeRepository.countByIdClassroomCode("ABCABC");
        List<AccountClassroomBridgeTeacher> teacherClassrooms = teacherBridgeRepository
                .findAllByIdAccountEmail("teacher@example.com");

        assertTrue(teacherIsInClassroom);
        assertEquals(2L, teacherCount);
        assertEquals(List.of("ABCABC"), teacherClassrooms.stream()
                .map(bridge -> bridge.getId().getClassroomCode())
                .toList());
    }

    @DisplayName("Deletes one teacher bridge without removing other teachers from the classroom")
    @Test
    void deleteByIdAccountEmailAndIdClassroomCode_whenMultipleTeachersExist_removesOnlyRequestedTeacherBridge() {
        insertClassroomData();
        insertTeacherBridge("teacher@example.com", "ABCABC");
        insertTeacherBridge("other.teacher@example.com", "ABCABC");

        teacherBridgeRepository.deleteByIdAccountEmailAndIdClassroomCode("teacher@example.com", "ABCABC");
        teacherBridgeRepository.flush();

        assertFalse(teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode("teacher@example.com", "ABCABC"));
        assertTrue(teacherBridgeRepository.existsByIdAccountEmailAndIdClassroomCode("other.teacher@example.com", "ABCABC"));
        assertEquals(1L, teacherBridgeRepository.countByIdClassroomCode("ABCABC"));
    }

    @DisplayName("Finds accepted pupils for one classroom and excludes pending pupils")
    @Test
    void findAllByClassroomClassroomCodeAndMembershipConfirmedByTeacherTrue_whenMixedMembershipsExist_returnsOnlyAcceptedPupilsForClassroom() {
        insertClassroomData();
        insertPupilAccount("accepted@example.com", "Accepted Pupil");
        insertPupilAccount("pending@example.com", "Pending Pupil");
        insertPupilAccount("other@example.com", "Other Pupil");
        insertMembership("accepted@example.com", "ABCABC", "accepted", true);
        insertMembership("pending@example.com", "ABCABC", "pending", false);
        insertMembership("other@example.com", "XYZXYZ", "other", true);

        List<PupilClassroomMembership> acceptedMemberships = membershipRepository
                .findAllByClassroomClassroomCodeAndMembershipConfirmedByTeacherTrue("ABCABC");

        assertEquals(1, acceptedMemberships.size());
        assertEquals("accepted@example.com", acceptedMemberships.getFirst().getAccountEmail());
        assertEquals("Accepted Pupil", acceptedMemberships.getFirst().getAccount().getLegalName());
    }

    @DisplayName("Finds pending pupil membership by email and classroom")
    @Test
    void findByAccountEmailAndClassroomClassroomCodeAndMembershipConfirmedByTeacherFalse_whenPendingMembershipExists_returnsMembership() {
        insertClassroomData();
        insertPupilAccount("pending@example.com", "Pending Pupil");
        insertMembership("pending@example.com", "ABCABC", "pending", false);

        Optional<PupilClassroomMembership> membership = membershipRepository
                .findByAccountEmailAndClassroomClassroomCodeAndMembershipConfirmedByTeacherFalse(
                        "pending@example.com",
                        "ABCABC");

        assertTrue(membership.isPresent());
        assertEquals("Pending Pupil", membership.orElseThrow().getAccount().getLegalName());
    }

    @DisplayName("Finds pending pupils with account and avatar object data")
    @Test
    void findAllByClassroomClassroomCodeAndMembershipConfirmedByTeacherFalse_whenPendingPupilsExist_loadsAccountAndAvatar() {
        insertClassroomData();
        insertPupilAccount("pending.avatar@example.com", "Pending Avatar");
        insertPupilAccount("pending.noavatar@example.com", "Pending No Avatar");
        Long avatarId = insertAvatarFile("avatar-images/pending.png");
        insertMembership("pending.avatar@example.com", "ABCABC", "avatar-user", false, avatarId);
        insertMembership("pending.noavatar@example.com", "ABCABC", "no-avatar-user", false);

        List<PupilClassroomMembership> memberships = membershipRepository
                .findAllByClassroomClassroomCodeAndMembershipConfirmedByTeacherFalse("ABCABC");

        assertEquals(2, memberships.size());
        PupilClassroomMembership avatarMembership = memberships.stream()
                .filter(membership -> membership.getAccountEmail().equals("pending.avatar@example.com"))
                .findFirst()
                .orElseThrow();
        assertEquals("Pending Avatar", avatarMembership.getAccount().getLegalName());
        assertEquals("avatar-user", avatarMembership.getUsername());
        assertEquals("avatar-images/pending.png", avatarMembership.getAvatarFileObject().getObjectKey());
    }

    @DisplayName("Rejects classroom codes that contain numbers")
    @Test
    void classroomCodeConstraint_whenCodeContainsNumbers_rejectsInsert() {
        assertThrows(DataIntegrityViolationException.class,
                () -> insertClassroom("ABC123", "Invalid classroom"));
    }

    private void insertClassroomData() {
        insertClassroom("ABCABC", "Main classroom");
        insertClassroom("XYZXYZ", "Other classroom");
        insertTeacherAccount("teacher@example.com", "Teacher Name");
        insertTeacherAccount("other.teacher@example.com", "Other Teacher");
        insertTeacherAccount("outside.teacher@example.com", "Outside Teacher");
    }

    private void insertClassroom(String classroomCode, String classroomName) {
        jdbcTemplate.update(
                "INSERT INTO classroom (classroom_code, classroom_name, classroom_description) VALUES (?, ?, ?)",
                classroomCode,
                classroomName,
                "Classroom repository test classroom");
    }

    private void insertTeacherAccount(String email, String legalName) {
        jdbcTemplate.update(
                "INSERT INTO account (email, password_data, legal_name, account_role) VALUES (?, ?, ?, ?::account_role_enum)",
                email,
                "encoded-password",
                legalName,
                "TEACHER");
    }

    private void insertPupilAccount(String email, String legalName) {
        jdbcTemplate.update(
                "INSERT INTO account (email, password_data, legal_name, account_role) VALUES (?, ?, ?, ?::account_role_enum)",
                email,
                "encoded-password",
                legalName,
                "PUPIL");
    }

    private void insertTeacherBridge(String teacherEmail, String classroomCode) {
        jdbcTemplate.update(
                "INSERT INTO account_classroom_bridge_teacher (account_email, classroom_code) VALUES (?, ?)",
                teacherEmail,
                classroomCode);
    }

    private void insertMembership(String email, String classroomCode, String username, boolean confirmed) {
        jdbcTemplate.update(
                "INSERT INTO pupil_classroom_membership (account_email, classroom_code, username, membership_confirmed_by_teacher) VALUES (?, ?, ?, ?)",
                email,
                classroomCode,
                username,
                confirmed);
    }

    private void insertMembership(String email, String classroomCode, String username, boolean confirmed, Long avatarFileObjectId) {
        jdbcTemplate.update(
                "INSERT INTO pupil_classroom_membership (account_email, classroom_code, username, membership_confirmed_by_teacher, avatar_file_object_id) VALUES (?, ?, ?, ?, ?)",
                email,
                classroomCode,
                username,
                confirmed,
                avatarFileObjectId);
    }

    private Long insertAvatarFile(String objectKey) {
        return jdbcTemplate.queryForObject(
                "INSERT INTO file_object (object_key, access_level, file_name) VALUES (?, ?::classroom_access_enum, ?) RETURNING id",
                Long.class,
                objectKey,
                "PUBLIC",
                objectKey.substring(objectKey.lastIndexOf('/') + 1));
    }
}
