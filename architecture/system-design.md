# RentFlow Core - System Design

## Overview

RentFlow Core is a production-grade, event-driven microservices architecture designed to handle rent payment processing at scale. The system supports scheduled payments, partial payments, partner bank integrations, and fault-tolerant transaction processing.

## Architecture Principles

1. **Event-Driven**: Services communicate asynchronously via Kafka events
2. **Fault Tolerance**: Circuit breakers, retries, and dead letter queues
3. **Idempotency**: Safe retry without duplicate processing
4. **Audit Trail**: Complete transaction history in the ledger
5. **Horizontal Scalability**: Stateless services can scale independently

## System Components

```
┌─────────────────────────────────────────────────────────────────────┐
│                           Client Layer                              │
│  (Mobile App, Web Portal, Property Manager Dashboard)              │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
                                ▼
┌─────────────────────────────────────────────────────────────────────┐
│                        API Gateway (Future)                         │
│                    Authentication, Rate Limiting                    │
└───────────────────────────────┬─────────────────────────────────────┘
                                │
        ┌───────────────────────┼───────────────────────┐
        ▼                       ▼                       ▼
┌───────────────┐    ┌───────────────┐    ┌───────────────┐
│Payment Service│    │Scheduling Svc │    │Partner Gateway│
│   (Port 8081) │    │  (Port 8082)  │    │  (Port 8083)  │
└───────┬───────┘    └───────┬───────┘    └───────┬───────┘
        │                    │                    │
        └────────────────────┼────────────────────┘
                             ▼
                   ┌─────────────────┐
                   │  Kafka Topics   │
                   │  - payment.*    │
                   │  - ledger.*     │
                   │  - schedule.*   │
                   └────────┬────────┘
                            │
                            ▼
                   ┌───────────────┐
                   │Ledger Service │
                   │  (Port 8084)  │
                   └───────────────┘
```

## Service Details

### Payment Service (8081)
**Responsibilities:**
- Payment orchestration and state management
- Idempotency handling
- Integration with Partner Gateway and Ledger Service
- API for payment CRUD operations

**Key Features:**
- State machine for payment lifecycle
- Automatic retry with exponential backoff
- Partial payment support
- Idempotency key handling

**Database:** PostgreSQL (rentflow_payment)
**Cache:** Redis

### Scheduling Service (8082)
**Responsibilities:**
- Recurring payment schedule management
- Cron-based execution
- Schedule lifecycle (pause/resume/delete)

**Key Features:**
- Multiple recurrence patterns (daily, weekly, monthly, etc.)
- Quartz-based job scheduling
- Independent execution tracking

**Database:** PostgreSQL (rentflow_scheduling)

### Partner Gateway (8083)
**Responsibilities:**
- Bank API integrations (Plaid, Stripe)
- Payment initiation
- Webhook handling

**Key Features:**
- Circuit breakers for external APIs
- Multiple payment provider support
- Provider-agnostic interface

### Ledger Service (8084)
**Responsibilities:**
- Double-entry accounting
- Balance tracking
- Financial reporting

**Key Features:**
- Debit/credit account management
- Transaction posting and settlement
- Audit trail

**Database:** PostgreSQL (rentflow_ledger)

## Data Flow

### Payment Creation Flow

```
1. Client → Payment Service: POST /api/v1/payments
2. Payment Service → Database: Create payment record (PENDING)
3. Payment Service → Kafka: Publish payment.created event
4. Payment Service → Client: Return payment response
```

### Payment Processing Flow

```
1. Scheduled Job or API Call: POST /api/v1/payments/{id}/process
2. Payment Service → Partner Gateway: Initiate payment
3. Partner Gateway → Stripe/Plaid: Execute transaction
4. Partner Gateway → Payment Service: Return transaction result
5. Payment Service → Ledger Service: Create ledger entry
6. Payment Service → Kafka: Publish payment.completed event
7. Payment Service → Database: Update payment status
```

### Recurring Payment Flow

```
1. Admin creates schedule via API
2. Scheduling Service stores schedule with next execution time
3. Cron job checks every 5 minutes for due schedules
4. For each due schedule:
   a. Publish payment.created event to Kafka
   b. Update schedule with execution details
   c. Calculate next execution time
5. Payment Service consumes event and creates payment
```

## Event Schema

### Payment Events

**payment.created**
```json
{
  "paymentId": "uuid",
  "tenantId": "uuid",
  "propertyId": "uuid",
  "leaseId": "uuid",
  "amount": 1500.00,
  "currency": "USD",
  "paymentMethod": "BANK_TRANSFER",
  "paymentType": "RECURRING",
  "scheduledFor": "2024-01-15T00:00:00Z"
}
```

**payment.completed**
```json
{
  "paymentId": "uuid",
  "transactionId": "txn_abc123",
  "settledAmount": 1500.00,
  "feeAmount": 15.00,
  "settlementMethod": "ACH",
  "settledAt": "2024-01-15T10:30:00Z"
}
```

**payment.failed**
```json
{
  "paymentId": "uuid",
  "errorCode": "INSUFFICIENT_FUNDS",
  "errorMessage": "Insufficient funds in account",
  "retryable": true,
  "retryAfter": "2024-01-15T11:00:00Z"
}
```

## Scaling Considerations

### Horizontal Scaling
- All services are stateless (except databases)
- Use Kubernetes for orchestration
- Configure Kafka partitions for parallelism

### Database Scaling
- Read replicas for reporting queries
- Connection pooling (HikariCP)
- Database per service pattern

### Kafka Configuration
- Partition strategy: By tenantId for ordering
- Replication factor: 3 for production
- Retention: 7 days
