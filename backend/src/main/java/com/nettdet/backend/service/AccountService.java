package com.nettdet.backend.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.nettdet.backend.domain.Account;
import com.nettdet.backend.domain.enums.AccountRole;
import com.nettdet.backend.dto.LoginRequest;
import com.nettdet.backend.dto.RegisterRequest;
import com.nettdet.backend.repository.AccountRepository;

@Service
public class AccountService {

    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountService(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

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
        account.setAccountRole(AccountRole.PUPIL);

        Account saved = accountRepository.save(account);
        logger.info("Account '{}' registered successfully", saved.getEmail());
        return saved;
    }

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
}
