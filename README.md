# RentFlow Core

A production-grade backend system for rent payment processing with scheduled payments, partial payments, partner bank integrations, and fault-tolerant transaction processing.

## Architecture

```
┌─────────────────┐      ┌──────────────────┐
│  Partner Gateway│─────▶│ Payment Service  │
│   (Bank APIs)   │      │  (Orchestrator)  │
└─────────────────┘      └────────┬─────────┘
                                  │
                          ┌───────┴────────┐
                          ▼                ▼
                   ┌──────────┐    ┌──────────────┐
                   │  Kafka   │────▶│ Ledger Svc   │
                   │  Events  │    │ (Accounting) │
                   └────┬─────┘    └──────────────┘
                        │
                        ▼
                   ┌──────────────┐
                   │Scheduling Svc│
                   │ (Cron Jobs)  │
                   └──────────────┘
```

## Services

| Service | Port | Description |
|---------|------|-------------|
| payment-service | 8081 | Payment orchestration, state machine |
| scheduling-service | 8082 | Recurring payment scheduling |
| partner-gateway | 8083 | Bank API integrations (Plaid, Stripe) |
| ledger-service | 8084 | Double-entry accounting |
| Kafka | 9092 | Event streaming |
| PostgreSQL | 5432 | Transactional data |
| Redis | 6379 | Caching & rate limiting |

## Features

- **Scheduled Payments** - Daily/weekly/monthly rent collection
- **Partial Payments** - Split payments across multiple transactions
- **Bank Integration** - Connect to 10,000+ banks via Plaid
- **Payment State Machine** - Track payment lifecycle (pending → processing → settled/failed)
- **Idempotency** - Safe retry without duplicate processing
- **Audit Trail** - Complete transaction history
- **Fault Tolerance** - Circuit breakers, retries, dead letter queues

## Quick Start

```bash
# Start all services
docker-compose up -d

# Run database migrations
cd payment-service && ./gradlew flywayMigrate

# Build all services
./gradlew build
```

## API Documentation

- [Payment Service API](./payment-service/README.md)
- [Scheduling Service API](./scheduling-service/README.md)
- [Partner Gateway API](./partner-gateway/README.md)
- [Ledger Service API](./ledger-service/README.md)

## Architecture Documentation

- [System Design](./architecture/system-design.md)
- [Payment State Machine](./architecture/payment-state-machine.md)

## Development

```bash
# Run individual service
cd payment-service && ./gradlew bootRun

# Run tests
./gradlew test

# Generate docs
./gradlew dokka
```

## Stack

- **Java 21** with Spring Boot 3.2
- **Event-Driven** with Apache Kafka
- **Database**: PostgreSQL + Flyway migrations
- **Caching**: Redis
- **Monitoring**: Actuator + Prometheus + Grafana
- **CI/CD**: GitHub Actions
- **API Docs**: OpenAPI 3.0

## License

MIT License - see LICENSE file
