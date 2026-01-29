package com.rentflow.ledger.repository;

import com.rentflow.ledger.model.Account;
import com.rentflow.ledger.model.AccountType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountRepository extends JpaRepository<Account, UUID> {

    Optional<Account> findByAccountNumber(String accountNumber);

    List<Account> findByOwnerId(UUID ownerId);

    List<Account> findByAccountType(AccountType accountType);

    @Query("SELECT a FROM Account a WHERE a.active = true")
    List<Account> findActiveAccounts();

    boolean existsByAccountNumber(String accountNumber);
}
