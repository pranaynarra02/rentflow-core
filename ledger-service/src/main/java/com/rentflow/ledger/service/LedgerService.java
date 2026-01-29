package com.rentflow.ledger.service;

import com.rentflow.ledger.dto.CreateEntryRequest;
import com.rentflow.ledger.model.*;
import com.rentflow.ledger.repository.AccountRepository;
import com.rentflow.ledger.repository.LedgerEntryRepository;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class LedgerService {

    private final LedgerEntryRepository entryRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public LedgerEntry createEntry(CreateEntryRequest request) {
        // Validate accounts exist
        var debitAccount = accountRepository.findByAccountNumber(request.debitAccount().accountNumber())
            .orElseThrow(() -> new AccountNotFoundException(request.debitAccount().accountNumber()));

        var creditAccount = accountRepository.findByAccountNumber(request.creditAccount().accountNumber())
            .orElseThrow(() -> new AccountNotFoundException(request.creditAccount().accountNumber()));

        // Create ledger entry
        var entry = LedgerEntry.builder()
            .id(UUID.randomUUID())
            .paymentId(request.paymentId())
            .tenantId(request.tenantId())
            .propertyId(request.propertyId())
            .leaseId(request.leaseId())
            .debitAccountNumber(request.debitAccount().accountNumber())
            .debitAccountType(request.debitAccount().accountType())
            .debitAccountOwnerId(request.debitAccount().ownerId())
            .creditAccountNumber(request.creditAccount().accountNumber())
            .creditAccountType(request.creditAccount().accountType())
            .creditAccountOwnerId(request.creditAccount().ownerId())
            .amount(request.amount())
            .currency(request.currency())
            .entryType(request.entryType())
            .reference(request.reference())
            .description(request.description())
            .entryDate(Instant.now())
            .status(EntryStatus.POSTED)
            .build();

        // Update account balances
        debitAccount.debit(request.amount());
        creditAccount.credit(request.amount());

        accountRepository.save(debitAccount);
        accountRepository.save(creditAccount);
        entry = entryRepository.save(entry);

        log.info("Created ledger entry: {} for payment: {}", entry.getId(), request.paymentId());
        return entry;
    }

    @Transactional(readOnly = true)
    public LedgerEntry getEntry(UUID id) {
        return entryRepository.findById(id)
            .orElseThrow(() -> new EntryNotFoundException(id));
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> getEntriesByTenant(UUID tenantId) {
        return entryRepository.findByTenantIdOrderByEntryDateDesc(tenantId);
    }

    @Transactional(readOnly = true)
    public List<LedgerEntry> getEntriesByLease(UUID leaseId) {
        return entryRepository.findByLeaseIdOrderByEntryDateDesc(leaseId);
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalRevenue(UUID tenantId) {
        return entryRepository.sumByTenantIdAndEntryType(
            tenantId,
            LedgerEntryType.RENT_PAYMENT
        );
    }

    @Transactional(readOnly = true)
    public BigDecimal getTotalPaidForLease(UUID leaseId) {
        return entryRepository.sumByLeaseIdAndEntryType(
            leaseId,
            LedgerEntryType.RENT_PAYMENT
        );
    }

    @Transactional
    @KafkaListener(topics = "payment-completed", groupId = "ledger-service")
    public void handlePaymentCompleted(Object event) {
        // Process payment completed event
        // This would create the corresponding ledger entry
        log.info("Received payment completed event: {}", event);
    }

    public static class AccountNotFoundException extends RuntimeException {
        public AccountNotFoundException(String accountNumber) {
            super("Account not found: " + accountNumber);
        }
    }

    public static class EntryNotFoundException extends RuntimeException {
        public EntryNotFoundException(UUID id) {
            super("Ledger entry not found: " + id);
        }
    }
}
