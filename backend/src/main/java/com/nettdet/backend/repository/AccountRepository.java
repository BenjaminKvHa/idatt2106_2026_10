package com.nettdet.backend.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.nettdet.backend.domain.Account;

/**
 * Repository interface for managing {@link Account} entities.
 * 
 * <p>Provides standard CRUD operations and custom lookup by email for user accounts.
 * The account's email address serves as its primary identifier.
 */
@Repository
public interface AccountRepository extends JpaRepository<Account, String> {

    /**
     * Finds an account by its unique email address.
     *
     * @param email the email address to search for
     * @return an {@link Optional} containing the account if found, or empty otherwise
     */
    Optional<Account> findByEmail(String email);
}
