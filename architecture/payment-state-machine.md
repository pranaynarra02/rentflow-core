# Payment State Machine

## State Transitions

```
                    ┌─────────────┐
                    │   PENDING   │
                    └──────┬──────┘
                           │
                           │ process()
                           ▼
                    ┌─────────────┐
                    │ PROCESSING  │
                    └──────┬──────┘
                           │
           ┌───────────────┼───────────────┐
           │               │               │
           │ success       │ failure       │ cancel
           ▼               ▼               ▼
    ┌─────────────┐ ┌─────────────┐ ┌─────────────┐
    │  COMPLETED  │ │   FAILED    │ │  CANCELLED  │
    └─────────────┘ └──────┬──────┘ └─────────────┘
                           │
                           │ retry
                           ▼
                    ┌─────────────┐
                    │ PROCESSING  │
                    └─────────────┘
```

## State Definitions

| State | Description | Transitions |
|-------|-------------|-------------|
| PENDING | Initial state, awaiting processing | PROCESSING, CANCELLED |
| PROCESSING | Payment being executed | COMPLETED, FAILED |
| COMPLETED | Payment successfully settled | REFUNDED |
| FAILED | Payment failed, may retry | PROCESSING, CANCELLED |
| CANCELLED | Payment cancelled | - |
| REFUNDED | Payment was refunded | - |
| PARTIALLY_SETTLED | Partial payment completed | COMPLETED |

## Retry Logic

### Exponential Backoff
```
Retry 1: 2 minutes
Retry 2: 4 minutes
Retry 3: 8 minutes
Max Retries: 3 (configurable)
```

### Retry Conditions
A payment is retryable if:
- Status is FAILED or PENDING
- `retryCount < maxRetries`
- `retryAfter` is null or in the past

### Non-Retryable Errors
- Invalid account
- Insufficient permissions
- Payment cancelled by user
- Account closed

## Idempotency

### Idempotency Key
- UUID generated on payment creation
- Stored in `payments.idempotency_key`
- Unique constraint ensures one payment per key

### Idempotent Operations
- `POST /api/v1/payments` with idempotency key
- `POST /api/v1/payments/{id}/process`
- All state transitions are idempotent

## State Machine Implementation

```java
public class Payment {
    public void markAsProcessing() {
        if (status != PaymentStatus.PENDING && status != PaymentStatus.FAILED) {
            throw new InvalidStateTransitionException(
                "Cannot process payment in state: " + status
            );
        }
        this.status = PaymentStatus.PROCESSING;
    }

    public void markAsCompleted(BigDecimal settledAmount, String transactionId) {
        if (status != PaymentStatus.PROCESSING) {
            throw new InvalidStateTransitionException(
                "Cannot complete payment in state: " + status
            );
        }
        this.status = PaymentStatus.COMPLETED;
        this.settledAmount = settledAmount;
        this.transactionId = transactionId;
        this.completedAt = Instant.now();
    }

    public void markAsFailed(String reason) {
        if (status != PaymentStatus.PROCESSING) {
            throw new InvalidStateTransitionException(
                "Cannot fail payment in state: " + status
            );
        }
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
        incrementRetry();
    }

    public boolean canRetry() {
        return retryCount < maxRetries &&
               (status == PaymentStatus.FAILED || status == PaymentStatus.PENDING) &&
               (retryAfter == null || retryAfter.isBefore(Instant.now()));
    }

    public void incrementRetry() {
        this.retryCount++;
        this.retryAfter = Instant.now().plusSeconds(calculateRetryDelay());
    }

    private long calculateRetryDelay() {
        return (long) Math.pow(2, retryCount) * 60;
    }
}
```

## Audit Trail

Every state transition is logged to `payment_audit_log`:

```sql
CREATE TABLE payment_audit_log (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    changed_by VARCHAR(100),
    change_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
```

## Event Publishing

State changes trigger Kafka events:

| State Change | Event Topic |
|--------------|-------------|
| PENDING → PROCESSING | payment.processing |
| PROCESSING → COMPLETED | payment.completed |
| PROCESSING → FAILED | payment.failed |
| ANY → CANCELLED | payment.cancelled |

## Handling Failures

### Transient Failures
- Network timeout
- Provider API rate limit
- Temporary bank downtime

**Action:** Retry with exponential backoff

### Permanent Failures
- Invalid credentials
- Account closed
- Insufficient permissions

**Action:** Mark as failed, no retry

### Compensation
- Ledger entries are created atomically
- Failed payments can be rolled back
- Dead letter queue for manual review
