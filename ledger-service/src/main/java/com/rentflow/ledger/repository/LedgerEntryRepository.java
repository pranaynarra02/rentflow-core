package com.rentflow.ledger.repository;

import com.rentflow.ledger.model.EntryStatus;
import com.rentflow.ledger.model.LedgerEntry;
import com.rentflow.ledger.model.LedgerEntryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface LedgerEntryRepository extends JpaRepository<LedgerEntry, UUID> {

    Optional<LedgerEntry> findByPaymentId(UUID paymentId);

    List<LedgerEntry> findByTenantIdOrderByEntryDateDesc(UUID tenantId);

    List<LedgerEntry> findByLeaseIdOrderByEntryDateDesc(UUID leaseId);

    List<LedgerEntry> findByDebitAccountNumberOrCreditAccountNumberOrderByEntryDateDesc(
        String debitAccountNumber, String creditAccountNumber
    );

    List<LedgerEntry> findByStatusAndEntryDateBefore(EntryStatus status, Instant date);

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0) FROM LedgerEntry e
        WHERE e.tenantId = :tenantId
        AND e.entryType = :entryType
        AND e.status = 'SETTLED'
        """)
    BigDecimal sumByTenantIdAndEntryType(
        UUID tenantId,
        LedgerEntryType entryType
    );

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0) FROM LedgerEntry e
        WHERE e.leaseId = :leaseId
        AND e.entryType = :entryType
        AND e.status = 'SETTLED'
        """)
    BigDecimal sumByLeaseIdAndEntryType(
        UUID leaseId,
        LedgerEntryType entryType
    );

    @Query("""
        SELECT e FROM LedgerEntry e
        WHERE e.status = 'PENDING'
        ORDER BY e.entryDate ASC
        """)
    List<LedgerEntry> findPendingEntries();

    List<LedgerEntry> findByTransactionId(String transactionId);
}
