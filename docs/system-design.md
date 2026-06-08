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

- Route requests to internal services.
- Validate JWT tokens.
- Apply basic CORS policy.
- Provide a single API entry point for frontend apps.

Example routes:

| Path | Target Service |
|---|---|
| `/api/auth/**` | Auth Service |
| `/api/cases/**` | Case Service |
| `/api/risk/**` | Risk Engine Service |
| `/api/ai/**` | AI Investigator Service |

### Auth Service

Responsibilities:

- User login.
- JWT issuance.
- User and role management.
- Password hashing.

Main tables:

- `users`
- `roles`
- `user_roles`

### Case Service

Responsibilities:

- Create and update compliance cases.
- Assign cases to analysts.
- Manage case comments and evidence.
- Record audit logs.

Main tables:

- `compliance_cases`
- `case_comments`
- `case_evidence`
- `audit_logs`

### Risk Engine Service

Responsibilities:

- Evaluate wallet risk.
- Execute AML rules.
- Return risk score and explanations.
- Cache repeated risk results.

Data sources:

- PostgreSQL for rule definitions.
- MongoDB for wallet transaction documents.
- Redis for risk result cache.

Example rules:

| Rule | Description | Severity |
|---|---|---|
| BLACKLIST_EXPOSURE | Wallet interacted with blacklisted address | Critical |
| HIGH_FREQUENCY_TRANSFER | Too many transfers in a short time window | High |
| NEW_ADDRESS_LARGE_WITHDRAWAL | New address receives large withdrawal | High |
| MULTI_HOP_OBFUSCATION | Funds moved across many hops quickly | Medium |

### AI Investigator Service

Responsibilities:

- Build structured prompts from case and risk data.
- Call AI model API through provider abstraction.
- Parse structured JSON response.
- Return summary, risk explanation, and next steps.

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
  "username": "analyst@example.com",
  "password": "password"
}
```

Response:

```json
{
  "accessToken": "jwt-token",
  "tokenType": "Bearer",
  "expiresIn": 3600
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

- Passwords are hashed with BCrypt.
- JWT includes user ID and roles.
- Gateway validates token format.
- Services validate role permissions for sensitive operations.
- Audit logs are append-only from the application perspective.
- AI prompts should not include unnecessary PII.

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

- Kafka-based event pipeline for risk alerts.
- Real blockchain indexer integration.
- KYC provider integration.
- Graph visualization for wallet relationships.
- Fine-grained workflow approval engine.
- CI/CD and Kubernetes deployment manifests.
