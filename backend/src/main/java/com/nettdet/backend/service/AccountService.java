package com.nettdet.backend.service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.nettdet.backend.domain.Account;
import com.nettdet.backend.domain.FileObject;
import com.nettdet.backend.domain.PupilClassroomMembership;
import com.nettdet.backend.domain.enums.AccountRole;
import com.nettdet.backend.dto.AuthMeResponse;
import com.nettdet.backend.dto.LoginRequest;
import com.nettdet.backend.dto.RegisterRequest;
import com.nettdet.backend.dto.UpdateAvatarRequest;
import com.nettdet.backend.dto.UpdateMeRequest;
import com.nettdet.backend.repository.AccountRepository;
import com.nettdet.backend.repository.PupilClassroomMembershipRepository;
import com.nettdet.backend.security.JwtUserPrincipal;

/**
 * Service class responsible for managing user accounts and their profiles.
 * 
 * <p>Handles core authentication workflows such as registration and login,
 * as well as profile-related actions like updating usernames and avatars.
 * Implements rate-limiting for avatar updates to ensure system stability.
 */
@Service
public class AccountService {

    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);
    private static final String AVATAR_IMAGE_PREFIX = "avatar-images/";
    private static final String PNG_EXTENSION = ".png";

    private final AccountRepository accountRepository;
    private final PupilClassroomMembershipRepository pupilClassroomMembershipRepository;
    private final PasswordEncoder passwordEncoder;
    private final AvatarService avatarService;
    private static final Duration AVATAR_UPDATE_COOLDOWN = Duration.ofSeconds(2);
    private final ConcurrentHashMap<String, Instant> lastAvatarUpdateByEmail = new ConcurrentHashMap<>();

    public AccountService(
            AccountRepository accountRepository,
            PupilClassroomMembershipRepository pupilClassroomMembershipRepository,
            PasswordEncoder passwordEncoder,
            AvatarService avatarService) {
        this.accountRepository = accountRepository;
        this.pupilClassroomMembershipRepository = pupilClassroomMembershipRepository;
        this.passwordEncoder = passwordEncoder;
        this.avatarService = avatarService;
    }

    /**
     * Registers a new user account in the system.
     * Validates that the email is unique and encodes the provided password.
     *
     * @param request the registration details including email, password, legal name, and role
     * @return the newly created and persisted {@link Account}
     * @throws IllegalArgumentException if an account with the given email already exists
     */
    public Account register(RegisterRequest request) {
        logger.info("Attempting to register account: {}", request.email());

        if (accountRepository.findByEmail(request.email()).isPresent()) {
            logger.warn("Registration failed: email '{}' already exists", request.email());
            throw new IllegalArgumentException("Email already exists");
        }

        Account account = new Account();
        account.setEmail(request.email());
        account.setPasswordData(passwordEncoder.encode(request.password()));
        account.setLegalName(request.legalName());
        account.setAccountRole(request.role());

        Account saved = accountRepository.save(account);
        logger.info("Account '{}' registered successfully", saved.getEmail());
        return saved;
    }

    /**
     * Authenticates a user based on email and password.
     *
     * @param request the login credentials
     * @return the authenticated {@link Account}
     * @throws BadCredentialsException if the email is not found or the password doesn't match
     */
    public Account login(LoginRequest request) {
        logger.info("Login attempt for account: {}", request.email());

        Account account = accountRepository.findByEmail(request.email())
                .orElseThrow(() -> {
                    logger.warn("Login failed: account '{}' not found", request.email());
                    return new BadCredentialsException("Invalid email or password");
                });

        if (!passwordEncoder.matches(request.password(), account.getPasswordData())) {
            logger.warn("Login failed: incorrect password for account '{}'", request.email());
            throw new BadCredentialsException("Invalid email or password");
        }

        logger.info("Login successful for account: {}", account.getEmail());
        return account;
    }

    /**
     * Retrieves an account entity based on its email address.
     * 
     * @param email the email to search for
     * @return the found {@link Account}
     * @throws java.util.NoSuchElementException if no account is found
     */
    public Account getAccountFromEmail(String email ) {
        return accountRepository.findByEmail(email).get();
    }

    /**
     * Constructs a comprehensive profile response for the authenticated user.
     * Aggregates account data with pupil membership and classroom details if applicable.
     *
     * @param email the email of the authenticated account
     * @return an {@link AuthMeResponse} containing aggregated profile data
     */
    @Transactional(readOnly = true)
    public AuthMeResponse getAuthenticatedProfile(String email) {
        Account account = getAccountFromEmail(email);
        PupilClassroomMembership membership = pupilClassroomMembershipRepository.findByAccountEmail(email).orElse(null);
        String username = membership != null ? membership.getUsername() : null;
        String classroomCode = membership != null && membership.getClassroom() != null
                ? membership.getClassroom().getClassroomCode()
                : null;
        Long avatarFileObjectId = membership != null && membership.getAvatarFileObject() != null
                ? membership.getAvatarFileObject().getId()
                : null;
        String avatarSelection = membership != null && membership.getAvatarFileObject() != null
                ? extractAvatarSelection(membership.getAvatarFileObject().getObjectKey())
                : null;
        boolean membershipConfirmed = membership != null && membership.isMembershipConfirmedByTeacher();

        return AuthMeResponse.from(
                account,
                username,
                classroomCode,
                avatarFileObjectId,
                avatarSelection,
                membershipConfirmed);
    }

    static String extractAvatarSelection(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return null;
        }

        String normalizedObjectKey = objectKey.startsWith("/") ? objectKey.substring(1) : objectKey;
        if (!normalizedObjectKey.startsWith(AVATAR_IMAGE_PREFIX)
                || !normalizedObjectKey.endsWith(PNG_EXTENSION)) {
            return null;
        }

        String fileName = normalizedObjectKey.substring(AVATAR_IMAGE_PREFIX.length());
        if (fileName.contains("/")) {
            return null;
        }

        String selection = fileName.substring(0, fileName.length() - PNG_EXTENSION.length());
        String[] parts = selection.split("_", -1);
        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
            return null;
        }

        return selection;
    }

    /**
     * Updates the display username for a pupil's profile.
     * 
     * @param principal the authenticated user principal
     * @param request   the update request containing the new username
     * @return the updated {@link AuthMeResponse}
     * @throws AccessDeniedException    if the user is not authenticated or not a pupil
     * @throws IllegalArgumentException if the username is blank or the pupil is not in a classroom
     */
    @Transactional
    public AuthMeResponse updateAuthenticatedProfile(JwtUserPrincipal principal, UpdateMeRequest request) {
        if (principal == null || principal.email() == null) {
            throw new AccessDeniedException("Authenticated access is required");
        }

        if (!AccountRole.PUPIL.name().equals(principal.role())) {
            throw new AccessDeniedException("Pupil access is required");
        }

        if (request == null || request.username() == null || request.username().trim().isEmpty()) {
            throw new IllegalArgumentException("Username must not be blank");
        }

        PupilClassroomMembership membership = pupilClassroomMembershipRepository.findByAccountEmail(principal.email())
                .orElseThrow(() -> new IllegalArgumentException("You are not currently in a classroom"));

        membership.setUsername(request.username().trim());
        pupilClassroomMembershipRepository.save(membership);

        return getAuthenticatedProfile(principal.email());
    }

    /**
     * Updates the avatar for a pupil's profile with rate-limiting.
     * 
     * @param principal the authenticated user principal
     * @param request   the update request containing avatar data
     * @return the updated {@link AuthMeResponse}
     * @throws AccessDeniedException    if the user is not authenticated or not a pupil
     * @throws IllegalStateException    if the update request is made within the cooldown period
     * @throws IllegalArgumentException if the avatar data is blank or the pupil is not in a classroom
     */
    @Transactional
    public AuthMeResponse updateAvatar(JwtUserPrincipal principal, UpdateAvatarRequest request) {
        if (principal == null || principal.email() == null) {
            throw new AccessDeniedException("Authenticated access is required");
        }

        if (!AccountRole.PUPIL.name().equals(principal.role())) {
            throw new AccessDeniedException("Pupil access is required");
        }

        if (request == null || request.avatar() == null || request.avatar().isBlank()) {
            throw new IllegalArgumentException("Avatar must not be blank");
        }

        Instant now = Instant.now();
        Instant previous = lastAvatarUpdateByEmail.get(principal.email());
        if (previous != null && Duration.between(previous, now).compareTo(AVATAR_UPDATE_COOLDOWN) < 0) {
            throw new IllegalStateException("Avatar updates are rate-limited; please wait before retrying");
        }
        lastAvatarUpdateByEmail.put(principal.email(), now);

        PupilClassroomMembership membership = pupilClassroomMembershipRepository.findByAccountEmail(principal.email())
                .orElseThrow(() -> new IllegalArgumentException("You are not currently in a classroom"));

        FileObject avatarFileObject = avatarService.getOrCreateAvatar(request.avatar());
        membership.setAvatarFileObject(avatarFileObject);
        pupilClassroomMembershipRepository.save(membership);

        return getAuthenticatedProfile(principal.email());
    }
}
