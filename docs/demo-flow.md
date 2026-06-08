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

```bash
docker compose up -d
```

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

## 5. Demo Wallet

Use:

```text
0x00new-blacklist-bad
```

Expected rule hits:

- BLACKLIST_EXPOSURE
- HIGH_FREQUENCY_TRANSFER
- NEW_ADDRESS_LARGE_WITHDRAWAL

## 6. API Demo Script

After all services are running:

```bash
./scripts/demo-api.sh
```

The script will:

- Login and extract JWT.
- Call risk scoring through the gateway.
- Create a case in PostgreSQL.
- Generate an AI summary.

## 7. Interview Narration

### English

I will demo a crypto AML investigation flow. First, the analyst logs in and receives a JWT. Then the analyst evaluates a wallet address through the risk engine. The risk engine returns not only a score, but also explainable rule hits. Next, the analyst creates a compliance case, which is persisted in PostgreSQL. Finally, the AI Investigator service generates a draft summary based on the risk result and analyst notes.

### 中文

我会演示一个加密 AML 调查流程。首先分析师登录获取 JWT，然后输入钱包地址调用风险引擎。风险引擎不会只返回一个分数，而是返回触发的规则、严重等级和分数影响。接下来分析师把这个风险结果创建成合规案件，案件会落到 PostgreSQL。最后 AI Investigator 服务基于风险结果和分析师备注生成调查摘要草稿。

## 8. What To Emphasize

- JWT authentication and protected APIs.
- Spring Cloud Gateway as a single entry point.
- Explainable risk scoring instead of black-box scoring.
- PostgreSQL persistence for cases.
- Redis cache for repeated risk evaluation.
- Human-in-the-loop AI summary generation.
- React analyst console connected to real backend APIs.
