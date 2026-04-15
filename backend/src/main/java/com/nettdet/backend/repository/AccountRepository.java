package com.nettdet.backend.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.nettdet.backend.domain.Account;

public interface AccountRepository extends JpaRepository<Account, String> {
}
