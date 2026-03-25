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
| enterprise-service | 8082 | Enterprise management | Via Gateway |
| warehouse-service | 8083 | Warehouse receipts (仓单) | Via Gateway |
| logistics-service | 8084 | Logistics tracking | Via Gateway |
| finance-service | 8085 | Finance (loan, receivable) | Via Gateway |
| credit-service | 8086 | Credit scoring | Via Gateway |
| fisco-gateway-service | 8087 | Blockchain client (ONLY service with FISCO SDK) | Direct |
| nginx | 80/443 | API Gateway, rate limiting | N/A |

### Network Configuration
- Custom bridge network: `fisco-app-net` (172.26.0.0/16), defined in docker-compose.yml
- MySQL: 172.26.0.100 (container name: `fisco-mysql`)
- FISCO nodes: 172.26.0.20-23

## Key Configuration Files

### Blockchain Contract Addresses (in docker-compose.yml)
All contract addresses are passed as environment variables to fisco-gateway-service:
```yaml
- CONTRACT_ENTERPRISE=0x7a9b6d564d5d191093a29b7c760dd6af931cae73
- CONTRACT_WAREHOUSE_CORE=0xeb000acf2e358cae769d308390145d9222b5577c
- CONTRACT_WAREHOUSE_OPS=0xc02dc5133c5e635c16adf4ffda4b7a2e8c5ada96
- CONTRACT_LOGISTICS_CORE=0x69ef4c5eca7bc099c2e8a8336c97af765d60dbf1
- CONTRACT_LOGISTICS_OPS=0x41a1281dba209614f2ada8ecc75fd957ad179d7b
- CONTRACT_RECEIVABLE_CORE=0xe46925ca51074d3b83ba993a4e88d0156eca6a06
- CONTRACT_RECEIVABLE_REPAYMENT=0x1d38f5d0c8c1ae7ed63a2d0ec905b9e9a17e70cf
- CONTRACT_LOAN_CORE=0xbeada4d89feb3440285de55c94ebc1a2e93639f9
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

### Database Migration
Flyway manages database migrations at `classpath:db/migration`. Migrations run automatically on service startup.

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

See `REFACTORING_PLAN.md` in the project root for detailed planned features and fixes including:

### High Priority
- User cancellation/audit workflow APIs (registration approval, account cancellation)
- Password change APIs
- Enterprise user management APIs (list users, disable users, audit)
- Blockchain query APIs for enterprise verification (6 missing endpoints)
- @RequireRole authorization on Credit service sensitive endpoints

### Medium Priority
- System administrator login for Enterprise service
- Geofencing for logistics (500m warehouse radius validation)
- Financial institution verification in Finance service
- StockOrder Hash calculation for blockchain traceability

### Known Issues
- Logistics blockchain failures do not rollback local transactions (data consistency risk)
