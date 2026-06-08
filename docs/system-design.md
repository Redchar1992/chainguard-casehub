# System Design: ChainGuard CaseHub

## 1. Design Goals

ChainGuard CaseHub is designed as a microservice-oriented full-stack platform for crypto compliance workflows. The system emphasizes explainability, auditability, modular backend services, and practical AI integration.

Primary design goals:

- Separate authentication, case workflow, risk scoring, and AI investigation concerns.
- Provide clean RESTful APIs for React and Vue applications.
- Store transactional business data in PostgreSQL.
- Store flexible transaction and AI analysis documents in MongoDB.
- Cache repeated risk results in Redis.
- Keep all compliance decisions traceable.

## 2. High-Level Architecture

```text
Frontend Layer
  ├── Analyst Console: React + TypeScript
  └── Admin Console: Vue 3 + TypeScript

API Layer
  └── Spring Cloud Gateway

Service Layer
  ├── Auth Service
  ├── Case Service
  ├── Risk Engine Service
  └── AI Investigator Service

Data Layer
  ├── PostgreSQL
  ├── MongoDB
  └── Redis
```

## 3. Backend Services

### API Gateway

Responsibilities:

- Route requests to internal services (Spring Cloud Gateway).
- Apply a CORS policy for the two console origins.
- Provide a single API entry point for the frontend apps.

The gateway forwards the `Authorization` header; JWT verification and
per-endpoint role authorization are enforced inside each service (Spring
Security resource server + `@PreAuthorize`), not at the gateway. There is no
OpenFeign or Swagger/OpenAPI tooling in this build.

Example routes:

| Path | Target Service |
|---|---|
| `/api/auth/**` | Auth Service |
| `/api/cases/**` | Case Service |
| `/api/rules/**` | Case Service |
| `/api/risk/**` | Risk Engine Service |
| `/api/ai/**` | AI Investigator Service |

### Auth Service

Responsibilities:

- Authenticate a user against the `users` table.
- Verify the password against the stored BCrypt hash.
- Derive roles from `user_roles` (not from the username).
- Issue a signed HS256 JWT carrying the user id and roles.
- Write a `USER_LOGIN` entry to `audit_logs`.

Main tables:

- `users`
- `roles`
- `user_roles`
- `audit_logs`

Read-only here; user/role administration is seeded via SQL.

### Case Service

Responsibilities:

- Create and list compliance cases.
- Drive case status through a state machine (see below).
- Manage AML rules and their JSON thresholds (rule API).
- Record audit logs for status changes and rule toggles.

Main tables:

- `compliance_cases`
- `aml_rules`
- `audit_logs`

The case service owns the canonical schema via Flyway migrations (`V1` schema,
`V2` demo data). Case comments and evidence are a future extension and are not
modeled in this build.

#### Case status state machine

```text
OPEN ──▶ REVIEWING ──▶ ESCALATED ──▶ CLOSED
 │           │ ▲           │ │
 │           │ └───────────┘ │   (ESCALATED ⇄ REVIEWING de-escalation)
 └───────────┴──────────────┘   (any non-terminal state may go to CLOSED)
```

`CLOSED` is terminal. Illegal jumps (e.g. `OPEN → ESCALATED`, reopening a
`CLOSED` case) are rejected with HTTP 409.

### Risk Engine Service

Responsibilities:

- Evaluate wallet risk.
- Execute AML rules.
- Return risk score and explanations.
- Cache repeated risk results.

Data sources (all wired and used):

- PostgreSQL (JPA) for enabled rule definitions and JSON thresholds.
- MongoDB (Spring Data Mongo) for the wallet's transaction document.
- Redis for the risk result cache (graceful degradation if unavailable).

The engine derives deterministic features from the transactions — count,
totals, max outbound amount, sliding-window burst count, blacklist/sanctioned
hits, wallet age — then evaluates each enabled rule against its threshold. The
score is the capped sum of severity-weighted impacts of the rules that fired.

Implemented rules:

| Rule | Description | Severity |
|---|---|---|
| BLACKLIST_EXPOSURE | Transaction with a blacklisted/sanctioned counterparty | Critical |
| HIGH_FREQUENCY_TRANSFER | Burst count within the window exceeds threshold | High |
| NEW_ADDRESS_LARGE_WITHDRAWAL | New wallet moves a large outbound amount | High |
| LARGE_AGGREGATE_VOLUME | Aggregate USD volume exceeds threshold | Medium |

`MULTI_HOP_OBFUSCATION` is defined in `aml_rules` but ships **disabled** and has
no evaluator yet; it is a placeholder for graph-based analysis.

### AI Investigator Service

Responsibilities:

- Build a structured system + user prompt from the wallet's risk signals.
- Call the Anthropic Messages API (`POST {baseUrl}/v1/messages`) through a
  provider abstraction, with the API key read from `AI_EXTERNAL_API_KEY`.
- Parse the model's JSON output into the response DTO.
- Return summary, risk factors, recommended actions, and confidence.

The provider abstraction has two implementations: a deterministic offline
`MockAiInvestigationProvider` (default, and the fallback on any external error)
and the real `ExternalAiInvestigationProvider` (active when
`ai.provider=external`). The compliance workflow never blocks on the AI layer.

AI output schema:

```json
{
  "summary": "Short investigation summary",
  "riskFactors": ["Reason 1", "Reason 2"],
  "recommendedActions": ["Action 1", "Action 2"],
  "confidence": "MEDIUM"
}
```

## 4. Data Model

### compliance_cases

| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| wallet_address | VARCHAR | Target wallet |
| title | VARCHAR | Case title |
| status | VARCHAR | OPEN, REVIEWING, ESCALATED, CLOSED |
| risk_level | VARCHAR | LOW, MEDIUM, HIGH, CRITICAL |
| risk_score | INTEGER | 0-100 |
| assignee_id | UUID | Analyst user ID |
| created_at | TIMESTAMP | Created time |
| updated_at | TIMESTAMP | Updated time |

### aml_rules

| Column | Type | Notes |
|---|---|---|
| id | UUID | Primary key |
| code | VARCHAR | Unique rule code |
| name | VARCHAR | Rule name |
| severity | VARCHAR | LOW, MEDIUM, HIGH, CRITICAL |
| threshold | JSONB | Rule-specific configuration |
| enabled | BOOLEAN | Active flag |
| version | INTEGER | Rule version |

### wallet_transactions MongoDB Document

```json
{
  "walletAddress": "0x123...",
  "transactions": [
    {
      "txHash": "0xabc...",
      "direction": "IN",
      "counterparty": "0xdef...",
      "amountUsd": 12000.50,
      "timestamp": "2026-06-08T10:00:00Z",
      "tags": ["new_address"]
    }
  ]
}
```

## 5. Key API Design

### Authentication

```http
POST /api/auth/login
```

Request:

```json
{
  "username": "analyst@chainguard.demo",
  "password": "Analyst123!"
}
```

Response:

```json
{
  "accessToken": "jwt-token",
  "tokenType": "Bearer",
  "expiresIn": 3600,
  "userId": "….",
  "roles": ["ANALYST"]
}
```

### Wallet Risk Evaluation

```http
GET /api/risk/wallets/{walletAddress}
```

Response:

```json
{
  "walletAddress": "0x123",
  "riskScore": 87,
  "riskLevel": "HIGH",
  "triggeredRules": [
    {
      "code": "BLACKLIST_EXPOSURE",
      "severity": "CRITICAL",
      "description": "Wallet interacted with a blacklisted address"
    }
  ]
}
```

### Create Case

```http
POST /api/cases
```

Request:

```json
{
  "walletAddress": "0x123",
  "title": "High-risk wallet investigation",
  "riskScore": 87,
  "riskLevel": "HIGH"
}
```

### Generate AI Summary

```http
POST /api/ai/cases/{caseId}/summary
```

Response:

```json
{
  "summary": "The wallet shows high-risk behavior due to blacklist exposure and rapid fund movement.",
  "riskFactors": ["Blacklist exposure", "High-frequency transfers"],
  "recommendedActions": ["Escalate for reviewer approval", "Request additional source-of-funds evidence"],
  "confidence": "HIGH"
}
```

## 6. Security Design

- Passwords are verified against stored BCrypt hashes.
- JWT (HS256) includes the persisted user id and roles.
- Each service is an OAuth2 resource server that verifies the JWT signature.
- Services enforce role permissions per endpoint with `@PreAuthorize`.
- Audit logs are append-only from the application perspective.
- AI prompts include only the risk signals needed for the summary.

## 7. Failure Handling

### AI Provider Failure

- Return a non-blocking error to the frontend.
- Allow analysts to continue manual investigation.
- Log provider error with request ID.

### Risk Engine Timeout

- Use cached risk result if available.
- Mark response as stale when cache fallback is used.

### Database Failure

- Return structured error response.
- Avoid partial case workflow updates by using transactions.

## 8. Future Improvements

CI already runs on GitHub Actions (backend build/test + both frontend builds).
Remaining ideas:

- Service Dockerfiles and Kubernetes deployment manifests.
- Kafka-based event pipeline for risk alerts.
- Real blockchain indexer integration and a graph-based MULTI_HOP evaluator.
- KYC provider integration.
- Graph visualization for wallet relationships.
- Fine-grained workflow approval engine.
