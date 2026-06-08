# ChainGuard CaseHub

A crypto compliance case-management platform for AML wallet risk scoring,
investigation case workflow, and AI-assisted investigation summaries. Built as a
Java Spring Cloud microservice backend with a React analyst console and a Vue
admin console.

This is a portfolio project. It is built to be honest: every claim below maps to
code you can read and run. See [docs/interview-notes.md](docs/interview-notes.md)
for the candid walkthrough and known limitations.

## What it actually does

- **Wallet risk scoring (real).** The risk engine loads enabled AML rules and
  their thresholds from PostgreSQL, reads the wallet's transaction history from
  MongoDB, computes deterministic signals (transaction count, totals, max
  outbound amount, sliding-window burst count, blacklist/sanctioned counterparty
  hits, wallet age), and evaluates each rule against its threshold. The score is
  the capped sum of severity-weighted impacts of the rules that fired — an
  explainable, defensible function of real data. Results are cached in Redis.
- **Authentication (real).** Users, roles, and assignments live in PostgreSQL.
  Login looks up the user, verifies the password against a stored BCrypt hash,
  derives roles from `user_roles`, and issues a signed HS256 JWT carrying the
  user id and roles. Downstream services validate the JWT and enforce roles with
  `@PreAuthorize`.
- **Case workflow (real).** Cases are persisted via JPA with Flyway-managed
  schema. Status changes go through an explicit state machine
  (`OPEN → REVIEWING → ESCALATED → CLOSED`, with de-escalation back to
  `REVIEWING`); illegal jumps are rejected with HTTP 409.
- **AI investigation (real, optional).** The AI service builds a structured
  prompt from the wallet's risk signals and calls the Anthropic Messages API,
  parsing strict JSON into the response DTO. Without an API key it falls back to
  a deterministic offline mock so the workflow never blocks. See
  [AI provider configuration](#ai-provider-configuration).
- **AML rule management (real).** Admins can create, edit, enable, and disable
  AML rules and their JSON thresholds through the admin console / rule API.
- **Audit logging (real).** Login, case status changes, and rule toggles are
  written to an append-only `audit_logs` table, with the acting user resolved
  from the JWT.

## Architecture

```text
            ┌────────────────────┐      ┌────────────────────┐
            │  Analyst Console   │      │   Admin Console    │
            │  React + Antd      │      │   Vue 3 + Element  │
            └─────────┬──────────┘      └─────────┬──────────┘
                      └─────────────┬─────────────┘
                            REST / JSON (JWT)
                                    │
                       ┌────────────▼────────────┐
                       │   Spring Cloud Gateway  │  (routing + CORS)
                       └────────────┬────────────┘
          ┌──────────────┬──────────┼───────────┬──────────────┐
   ┌──────▼─────┐ ┌──────▼─────┐ ┌──▼─────────┐ ┌▼─────────────┐
   │   Auth     │ │   Case +   │ │   Risk     │ │ AI           │
   │  Service   │ │   Rules    │ │  Engine    │ │ Investigator │
   └──────┬─────┘ └──────┬─────┘ └──┬───────┬─┘ └──────────────┘
          │              │          │       │
     ┌────▼──────────────▼──────────▼─┐  ┌──▼────┐   ┌─────────────────┐
     │           PostgreSQL           │  │ Redis │   │ Anthropic API   │
     │ users/roles/cases/rules/audit  │  │ cache │   │ (optional)      │
     └────────────────────────────────┘  └───────┘   └─────────────────┘
                                    ┌──────────────┐
        Risk Engine also reads ───▶ │   MongoDB    │  wallet_transactions
                                    └──────────────┘
```

The gateway routes `/api/auth/**`, `/api/cases/**`, `/api/rules/**`,
`/api/risk/**`, and `/api/ai/**` to the matching service.

## Tech stack

**Backend** — Java 17, Spring Boot 3.3, Spring Cloud Gateway, Spring Security +
OAuth2 resource server (HS256 JWT), Spring Data JPA, Spring Data MongoDB,
Flyway, PostgreSQL, MongoDB, Redis.

**Frontend** — React + TypeScript + Ant Design (analyst console);
Vue 3 + TypeScript + Element Plus (admin console); Vite.

**Infra** — Docker Compose for PostgreSQL, MongoDB, and Redis. CI on GitHub
Actions builds and tests the backend and builds both frontends.

> Scope note: the gateway forwards the JWT and applies CORS; per-endpoint role
> authorization is enforced inside each service via `@PreAuthorize`. The two
> consoles are focused demo UIs (a single investigation workflow / rule list),
> not full multi-page applications. There is no OpenFeign or Swagger in this
> build.

## Repository structure

```text
.
├── README.md
├── docker-compose.yml            # postgres + mongo + redis
├── docs/
│   ├── product-requirements.md
│   ├── system-design.md
│   ├── development.md
│   ├── demo-flow.md
│   └── interview-notes.md
├── backend/
│   ├── pom.xml                   # multi-module parent
│   ├── api-gateway/
│   ├── auth-service/
│   ├── case-service/             # cases + AML rule management + audit
│   ├── risk-engine-service/
│   └── ai-investigator-service/
├── frontend/
│   ├── analyst-console/          # React
│   └── admin-console/            # Vue
└── infra/
    ├── postgres/init.sql         # schema + demo users + rules
    ├── mongo/init.js             # seeded wallet_transactions
    └── redis/
```

## Running locally

Databases run in Docker; the five Spring Boot services run on the host with
`mvn spring-boot:run`. This split is intentional and consistent — there are no
service Dockerfiles in this build.

1. Start the data stores:

   ```bash
   docker compose up -d   # postgres:5432, mongo:27017, redis:6379
   ```

   `infra/postgres/init.sql` seeds the schema, demo users, and AML rules;
   `infra/mongo/init.js` seeds wallet transaction documents. The case-service
   also owns the canonical schema via Flyway migrations.

2. Run each service in its own terminal (JDK 17 required):

   ```bash
   (cd backend/auth-service && mvn spring-boot:run)          # :8081
   (cd backend/case-service && mvn spring-boot:run)          # :8082
   (cd backend/risk-engine-service && mvn spring-boot:run)   # :8083
   (cd backend/ai-investigator-service && mvn spring-boot:run) # :8084
   (cd backend/api-gateway && mvn spring-boot:run)           # :8080
   ```

3. Run a console:

   ```bash
   (cd frontend/analyst-console && npm install && npm run dev)  # :5173
   (cd frontend/admin-console && npm install && npm run dev)    # :5174
   ```

### Demo credentials

Seeded users (BCrypt hashes in `infra/postgres/init.sql`):

| Username                    | Password       | Roles                     |
|-----------------------------|----------------|---------------------------|
| `admin@chainguard.demo`     | `Admin123!`    | ADMIN, ANALYST, REVIEWER  |
| `analyst@chainguard.demo`   | `Analyst123!`  | ANALYST                   |
| `reviewer@chainguard.demo`  | `Reviewer123!` | REVIEWER                  |

### AI provider configuration

The AI service defaults to the offline mock (`AI_PROVIDER=mock`). To use the
real Anthropic Messages API, set environment variables before starting the
AI service:

```bash
export AI_PROVIDER=external
export AI_EXTERNAL_API_KEY=sk-ant-...        # never commit this
# optional overrides:
export AI_EXTERNAL_MODEL=claude-3-5-haiku-latest
```

The key is read from the environment only; on any error (missing key, network,
malformed output) the service falls back to the mock.

## Demo

See [docs/demo-flow.md](docs/demo-flow.md). After the services are up:

```bash
./scripts/demo-api.sh
```

It logs in, evaluates `0x00new-blacklist-bad0`, lists AML rules, creates a case,
and generates an AI summary — all through the gateway.

## Build and test

```bash
cd backend && mvn -q package         # compiles + runs all unit/integration tests
cd frontend/analyst-console && npm run build
cd frontend/admin-console && npm run build
```

Backend tests include rule-logic unit tests, an auth login integration test
(H2), and a case-workflow integration test that drives JWT → security →
`@PreAuthorize` → JPA → state machine → audit end to end.

## Roadmap

- [x] Spring Cloud multi-module backend (gateway + four services)
- [x] PostgreSQL, MongoDB, and Redis via Docker Compose
- [x] JWT authentication with persisted users and role-protected APIs
- [x] Wallet risk scoring over real Postgres rules + Mongo transactions
- [x] Compliance case workflow with a status state machine
- [x] AML rule management API and admin console
- [x] Audit logging for sensitive actions
- [x] AI investigation summary with mock fallback + real Anthropic provider
- [x] React analyst console and Vue admin console
- [x] Demo data and demo script
- [x] CI workflow (backend build/test + frontend builds)
- [ ] Service Dockerfiles / Kubernetes manifests
- [ ] Kafka-based alert ingestion and a real chain indexer

## Positioning

ChainGuard CaseHub demonstrates end-to-end ownership of a compliance-oriented
full-stack system: cross-framework frontends, a Spring Cloud microservice
backend, SQL + NoSQL + cache data modeling, JWT security with role-based
authorization, an explainable rule engine, and a real (but safely optional) LLM
integration.
