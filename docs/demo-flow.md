# Demo Flow

This document describes a short interview-friendly demo for ChainGuard CaseHub.

## 1. Demo Goal

Show an end-to-end compliance investigation workflow:

1. Login as a compliance analyst.
2. Evaluate a suspicious wallet address.
3. Review explainable AML rule hits.
4. Create a compliance case.
5. Generate an AI investigation draft.

## 2. Start Dependencies

Databases run in Docker; the services run on the host via `mvn spring-boot:run`.

```bash
docker compose up -d   # postgres + mongo + redis, seeded from infra/
```

Demo login: `analyst@chainguard.demo` / `Analyst123!` (analyst),
`admin@chainguard.demo` / `Admin123!` (admin, for rule management).

## 3. Start Backend Services

Use JDK 17.

```bash
cd backend/auth-service
mvn spring-boot:run
```

```bash
cd backend/risk-engine-service
mvn spring-boot:run
```

```bash
cd backend/case-service
mvn spring-boot:run
```

```bash
cd backend/ai-investigator-service
mvn spring-boot:run
```

```bash
cd backend/api-gateway
mvn spring-boot:run
```

## 4. Start Analyst Console

```bash
cd frontend/analyst-console
npm install
npm run dev
```

Open:

```text
http://localhost:5173
```

## 5. Demo Wallets

The risk engine evaluates real rules over the seeded MongoDB transactions, so
different demo wallets exercise different signals:

| Wallet | Triggers | Result |
|---|---|---|
| `0x00new-blacklist-bad0` | BLACKLIST_EXPOSURE + NEW_ADDRESS_LARGE_WITHDRAWAL | score 70, HIGH |
| `0xhotwallet-frequent` | HIGH_FREQUENCY_TRANSFER | score 25, LOW |
| `0xcleanwallet-lowrisk` | none | score 0, LOW |

The headline demo wallet is `0x00new-blacklist-bad0`. (It has only three
transactions, so the high-frequency rule does not fire — use
`0xhotwallet-frequent` to demonstrate burst detection.)

## 6. API Demo Script

After all services are running:

```bash
./scripts/demo-api.sh
```

The script will:

- Login and extract JWT.
- Call risk scoring through the gateway.
- List AML rules from the Case Service rule management API.
- Create a case in PostgreSQL.
- Generate an AI summary.

## 7. Interview Narration

### English

I will demo a crypto AML investigation flow. First, the analyst logs in and receives a JWT. Then the analyst evaluates a wallet address through the risk engine. The risk engine returns not only a score, but also explainable rule hits. Next, the analyst creates a compliance case, which is persisted in PostgreSQL. Finally, the AI Investigator service generates a draft summary based on the risk result and analyst notes.

### 中文

我会演示一个加密 AML 调查流程。首先分析师登录获取 JWT，然后输入钱包地址调用风险引擎。风险引擎不会只返回一个分数，而是返回触发的规则、严重等级和分数影响。接下来分析师把这个风险结果创建成合规案件，案件会落到 PostgreSQL。最后 AI Investigator 服务基于风险结果和分析师备注生成调查摘要草稿。

## 8. What To Emphasize

- JWT authentication against persisted users (BCrypt) and role-protected APIs.
- Spring Cloud Gateway as a single entry point.
- Explainable risk scoring over real Postgres rules + Mongo transactions.
- PostgreSQL persistence for cases, with a status state machine.
- Redis cache for repeated risk evaluation.
- Audit logging on login, status change, and rule toggle.
- Human-in-the-loop AI summary (Anthropic provider with offline mock fallback).
- React analyst console connected to real backend APIs.
- Vue admin console connected to AML rule management APIs.
