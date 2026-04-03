# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

FISCO BCOS 供应链金融平台 (Supply Chain Finance Platform) - A microservices architecture built on FISCO BCOS blockchain for supply chain finance operations.

**Tech Stack:** Spring Boot 2.7.18, Java 11, Spring Cloud 2021.0.8, FISCO BCOS Java SDK 3.8.0, MySQL 8.4.0, Nginx

**Security:** Actuator endpoints removed (HIGH severity vulnerability remediation). All services use MySQL Connector 8.4.0 to address CVE-2024-21752.

## Build Commands

### Maven Build
```bash
# Build all modules
mvn clean package -DskipTests

# Build specific service (with dependencies)
mvn clean package -DskipTests -pl services/fisco-gateway-service -am

# Build with Docker image tag
IMAGE_TAG=latest mvn clean package -DskipTests

# Run tests for a specific service
mvn test -pl services/auth-service

# Run tests for all modules
mvn test
```

### Docker Commands
```bash
# Build and start all services (rebuild if code changed)
docker compose down && docker compose up -d --build

# Start all services (excluding nginx for blockchain-only functionality)
docker compose up -d

# Start specific profile (api-gateway adds nginx)
docker compose --profile api-gateway up -d

# Start full stack including nginx
docker compose --profile full up -d

# View container status
docker ps --format "table {{.Names}}\t{{.Status}}"

# View logs
docker logs fisco-gateway-service --tail 50
docker logs fisco-mysql --tail 20

# Restart specific service
docker compose restart fisco-gateway-service

# Stop all services
docker compose down
```

## Architecture

### Service Communication Pattern
```
Client → Nginx (API Gateway) → Microservices → FISCO Gateway Service → FISCO BCOS Blockchain
```

**Direct Communication (without nginx):**
- Each microservice exposes ports 8081-8087 directly
- Business services use Feign Client to call fisco-gateway-service
- FISCO Gateway Service (8087) is the ONLY service that directly connects to FISCO blockchain

### Service Roles

| Service | Port | Purpose | Blockchain Access |
|---------|------|---------|-------------------|
| auth-service | 8081 | JWT authentication, user management | No |
| enterprise-service | 8082 | Enterprise management | Via Gateway (FISCO_ENABLED=false) |
| warehouse-service | 8083 | Warehouse receipts (仓单) | Via Gateway |
| logistics-service | 8084 | Logistics tracking | Via Gateway (FISCO_ENABLED=false) |
| finance-service | 8085 | Finance (loan, receivable) | Via Gateway (FISCO_ENABLED=false) |
| credit-service | 8086 | Credit scoring | Via Gateway (FISCO_ENABLED=false) |
| fisco-gateway-service | 8087 | Blockchain client (ONLY service with FISCO SDK) | Direct (FISCO_ENABLED=true) |
| nginx | 80/443 | API Gateway, rate limiting | N/A |

### Network Configuration
- Custom bridge network: `fisco-app-net` (172.26.0.0/16), defined in docker-compose.yml
- MySQL: 172.26.0.100 (container name: `fisco-mysql`)
- FISCO nodes: 172.26.0.20-23

### FISCO SDK Integration Pattern

Business services use `@ConditionalOnProperty` to control blockchain SDK initialization:
```java
@ConditionalOnProperty(name = "fisco.enabled", havingValue = "true", matchIfMissing = true)
```
- `matchIfMissing = true` means when the property is absent, the condition is satisfied
- `havingValue = "true"` requires the value to be "true" for SDK initialization
- **Result**: `FISCO_ENABLED=true` → SDK initialized; `FISCO_ENABLED=false` or unset → SDK disabled

Four services explicitly set `FISCO_ENABLED=false` (enterprise, logistics, finance, credit) because they use Feign Client to call fisco-gateway-service instead of connecting to blockchain directly. The `WARN BlockchainConfig: FISCO BCOS 功能已禁用` log is **expected** for these services.

## Key Configuration Files

### Blockchain Contract Addresses
All contract addresses are defined in `.env` file and passed to fisco-gateway-service:
```bash
# Enterprise
CONTRACT_ENTERPRISE=0xe3fffb217e885578f75e1ac07f1fbff859171fe3
CONTRACT_ENTERPRISE_AUTH=0xc860ab27901b3c2b810165a6096c64d88763617f

# Credit
CONTRACT_CREDIT_CORE=0xafcdafa5be0a0e2c34328adf10d893a591b5e774
CONTRACT_CREDIT_SCORE=0x6ea6907f036ff456d2f0f0a858afa9807ff4b788

# Warehouse
CONTRACT_WAREHOUSE_CORE=0x5e0aa2793a9db58513610d8ff35aa877cee75b8e
CONTRACT_WAREHOUSE_OPS=0xa26565f61568353af17f8ce9beeb8e685140d6fe
CONTRACT_WAREHOUSE_CORE_EXT=0x55b63f96d81f094729af702adfc7af72bd75c54e

# Receivable
CONTRACT_RECEIVABLE_CORE=0xb31661caf079ddd45d5ed8af7becc220199fab29
CONTRACT_RECEIVABLE_REPAYMENT=0x1d38f5d0c8c1ae7ed63a2d0ec905b9e9a17e70cf

# Bill
CONTRACT_BILL_CORE=0x2af6160e266f763652f433a80b94fc13f4065303
CONTRACT_BILL_OPS=0xa6cdc4fed96bbf8c9c013cc19200b3c8ba95c93e

# Logistics
CONTRACT_LOGISTICS_CORE=0x3d06c6a1df7d56effec855f813f797a64ea1cee5
CONTRACT_LOGISTICS_OPS=0x525f4e5362d8b15e5d4aa0335b2d153c70aa5eca

# Loan
CONTRACT_LOAN_CORE=0xbf14744175b48ac9a2e1fc4ebc6c0a5f4afd0ad2
CONTRACT_LOAN_REPAYMENT=0xd688eabe4597d2d23180045a4444d0c3450b6ab9
```

### FISCO SDK Configuration
- SDK config mounted at `/app/sdk` in fisco-gateway-service container
- Source: `/home/llm_rca/fisco/my-bcos-app/fisco/nodes/127.0.0.1/sdk`
- Account keys at `/app/sdk/account` with symlink at `/app/account`

### JWT Configuration
- JWT_SECRET must be set in environment (minimum 32 characters)
- Access token expiration: 2 hours (7200000ms)
- Refresh token expiration: 7 days (604800000ms)

## Important Implementation Details

### Symlink Fix for FISCO SDK
The Dockerfile for fisco-gateway-service includes a symlink to resolve SDK path issues:
```dockerfile
ENTRYPOINT ["sh", "-c", "ln -sf /app/sdk/account /app/account 2>/dev/null || true && java $JAVA_OPTS -Dfile.encoding=UTF-8 -jar app.jar"]
```
This creates `/app/account` → `/app/sdk/account` because the SDK looks for keys relative to working directory `/app`, not `/app/sdk/`.

### Health Check Endpoints
Each service implements a `/health` endpoint via `HealthController` that returns `{"status":"UP"}`. This is required because:
- JwtAuthenticationFilter blocks unauthenticated requests to `/`
- Docker healthchecks depend on these endpoints

**Note:** Spring Boot Actuator was removed during security hardening, so `/health` is implemented as a custom controller endpoint rather than via Actuator's auto-configured endpoint.

### API Routing via Nginx
```
/api/v1/auth/*        → auth-service:8081
/api/v1/enterprise/*  → enterprise-service:8082
/api/v1/warehouse/*   → warehouse-service:8083
/api/v1/logistics/*   → logistics-service:8084
/api/v1/finance/*     → finance-service:8085
/api/v1/credit/*      → credit-service:8086
/api/v1/blockchain/*  → fisco-gateway-service:8087
```

### Database Migration (Single Owner Principle)
**auth-service is the ONLY service with Flyway enabled** and owns ALL schema changes to the shared `fisco_data` database. All other services (warehouse, logistics, finance, etc.) have Flyway disabled.

Migration files live in `services/auth-service/src/main/resources/db/migration/` with version prefix `V{number}__`. When adding schema changes:
1. Increment the version number (current max: V23)
2. Create the migration file in auth-service's migration directory
3. Restart auth-service to apply

Example: `services/auth-service/src/main/resources/db/migration/V22__add_warehouse_receipt_remark.sql`

## Docker Profiles
- `api-gateway`: Adds nginx container for production-like setup
- `full`: Enables all services including nginx

## Common Issues and Fixes

1. **Blockchain client initialization fails**: Check SDK path and ensure `/app/account` symlink exists
2. **Healthcheck 401 on auth-service**: Ensure `/health` is in JwtAuthenticationFilter EXCLUDE_PATTERNS
3. **Contract address warnings**: Verify CONTRACT_* environment variables are set in docker-compose.yml

## Module Structure

### common-api (`common-api/`)
Shared module containing Feign clients, DTOs, enums, and annotations used across all services:
- `feign/` - Feign client interfaces for inter-service communication (BlockchainFeignClient, WarehouseFeignClient, etc.)
- `dto/` - Shared DTOs (LoginRequestDTO, RegisterRequestDTO, TokenResponseDTO, etc.)
- `enums/` - Shared enums (UserRoleEnum, UserStatusEnum, EnterpriseStatusEnum, etc.)
- `annotation/` - Shared annotations (@RequireRole)
- `constant/` - Shared constants (EntRoleConstant)

All business services depend on common-api.

### contracts (`contracts/`)
Solidity smart contracts for FISCO BCOS blockchain. Each domain has its own contract set:
- Enterprise contracts
- Warehouse receipts contracts
- Logistics contracts
- Loan contracts
- Receivable contracts

### console (`console/`)
Admin console application for interacting with the platform.

## Known SDK Limitations (FISCO BCOS Java SDK 3.8.0)

- `getBalance()` is not supported - use contract calls instead
- Dynamic contract invocation (`callContract`/`sendContractTransaction`) requires generated contract classes, not reflection

## Ongoing Work

See `REFACTORING_PLAN.md` for the full prioritized list of planned features and fixes.

### Recently Completed (2026-03-29)
- **A1-A6 (High)**: Loan repayment unlocking, blockchain rollback compensation, enterprise state machine, cancelLoan blockchain call, @RequireRole on credit-service, LoanOverdueCheckTask
- **M1-M26 (Medium)**: Various service-level fixes including idempotency, invitation code generation, user status state machine
- **L1-L3 (Low)**: Void receipt endpoint, split/merge cancellation, logistics deviation alerting

### Known Issues
- **L4 (Deferred)**: GPS anti-counterfeiting - high complexity, existing geofence provides partial mitigation
