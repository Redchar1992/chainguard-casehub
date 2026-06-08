# Development Guide

## Prerequisites

- JDK 17
- Maven 3.9+
- Node.js 20+
- Docker Desktop

> This project uses Spring Boot 3.x, which requires Java 17 or later.

## Start Local Dependencies

```bash
docker compose up -d
```

This starts:

- PostgreSQL on `localhost:5432`
- MongoDB on `localhost:27017`
- Redis on `localhost:6379`

## Build Backend

```bash
cd backend
mvn -DskipTests package
```

## Run Backend Services

Open separate terminals:

```bash
cd backend/api-gateway
mvn spring-boot:run
```

```bash
cd backend/auth-service
mvn spring-boot:run
```

```bash
cd backend/case-service
mvn spring-boot:run
```

```bash
cd backend/risk-engine-service
mvn spring-boot:run
```

```bash
cd backend/ai-investigator-service
mvn spring-boot:run
```

## Run React Analyst Console

```bash
cd frontend/analyst-console
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```

## Run Vue Admin Console

```bash
cd frontend/admin-console
npm install
npm run dev
```

Open:

```text
http://localhost:5174
```

## Demo API Calls

The protected APIs require a JWT. The easiest way to exercise the flow is:

```bash
./scripts/demo-api.sh
```

Manual calls through API Gateway:

### Login

```bash
TOKEN=$(curl -sS -X POST http://localhost:8080/api/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"username":"analyst@chainguard.demo","password":"demo-password"}' \
  | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
```

### Evaluate Wallet Risk

```bash
curl http://localhost:8080/api/risk/wallets/0x00new-blacklist-bad \
  -H "Authorization: Bearer $TOKEN"
```

### Create Case

```bash
curl -X POST http://localhost:8080/api/cases \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "walletAddress":"0x00new-blacklist-bad",
    "title":"High-risk wallet investigation",
    "riskScore":90,
    "riskLevel":"CRITICAL"
  }'
```

### Generate AI Summary

```bash
curl -X POST http://localhost:8080/api/ai/cases/00000000-0000-0000-0000-000000000000/summary \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "walletAddress":"0x00new-blacklist-bad",
    "riskScore":90,
    "riskLevel":"CRITICAL",
    "triggeredRules":["BLACKLIST_EXPOSURE", "HIGH_FREQUENCY_TRANSFER"],
    "analystNotes":["Counterparty appears on blacklist dataset"]
  }'
```
