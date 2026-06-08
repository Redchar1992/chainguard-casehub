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
  -d '{"username":"analyst@chainguard.demo","password":"Analyst123!"}' \
  | sed -n 's/.*"accessToken":"\([^"]*\)".*/\1/p')
```

### Evaluate Wallet Risk

```bash
curl http://localhost:8080/api/risk/wallets/0x00new-blacklist-bad0 \
  -H "Authorization: Bearer $TOKEN"
```

### List AML Rules

```bash
curl http://localhost:8080/api/rules \
  -H "Authorization: Bearer $TOKEN"
```

### Create Case

```bash
curl -X POST http://localhost:8080/api/cases \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer $TOKEN" \
  -d '{
    "walletAddress":"0x00new-blacklist-bad0",
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
    "walletAddress":"0x00new-blacklist-bad0",
    "riskScore":90,
    "riskLevel":"CRITICAL",
    "triggeredRules":["BLACKLIST_EXPOSURE", "HIGH_FREQUENCY_TRANSFER"],
    "analystNotes":["Counterparty appears on blacklist dataset"]
  }'
```

## AI Provider Configuration

The AI Investigator service uses the deterministic offline mock provider by
default:

```bash
AI_PROVIDER=mock
```

To use the real Anthropic Messages API, set the provider and key via the
environment (the key is never hardcoded or committed):

```bash
export AI_PROVIDER=external
export AI_EXTERNAL_API_KEY=sk-ant-...
# optional overrides:
export AI_EXTERNAL_MODEL=claude-3-5-haiku-latest
export AI_EXTERNAL_BASE_URL=https://api.anthropic.com
```

On any failure (missing key, network error, malformed model output) the service
falls back to the mock provider so the workflow is never blocked.

## Run Tests

```bash
cd backend
mvn -q test
```

This runs the rule-engine unit tests, the auth login integration test, and the
case-workflow integration test (both use in-memory H2).
