# ChainGuard CaseHub

AI-powered crypto compliance case management platform for AML risk scoring, wallet investigation, and suspicious transaction review.

## Overview

ChainGuard CaseHub is a full-stack compliance platform designed for crypto exchanges and fintech teams. It helps compliance analysts detect suspicious wallet activities, investigate high-risk transactions, manage AML cases, and generate AI-assisted investigation summaries.

The project is built to demonstrate end-to-end ownership across frontend, backend, microservices, data modeling, API design, and AI model integration.

## Why This Project

Crypto compliance teams need to review large volumes of wallet activities, transaction alerts, blacklist exposure, abnormal withdrawals, and user risk signals. Traditional dashboards are often manual and fragmented.

ChainGuard CaseHub solves this by providing:

- Wallet risk scoring based on AML rules
- Case management workflow for compliance analysts
- AI-generated investigation summaries
- Role-based access control and audit logs
- React analyst console and Vue admin console
- Java Spring Cloud backend services
- SQL, NoSQL, Redis, and RESTful APIs

## Core Features

### 1. Wallet Risk Scoring

- Input a wallet address and retrieve transaction history
- Apply AML rules to detect suspicious behavior
- Generate risk score and risk level
- Highlight blacklist exposure and abnormal patterns

### 2. Compliance Case Management

- Convert risk alerts into investigation cases
- Assign cases to compliance analysts
- Track case status: Open, Reviewing, Escalated, Closed
- Add comments, evidence, and audit logs

### 3. AI Investigator Copilot

- Summarize suspicious wallet behavior
- Generate compliance investigation notes
- Explain risk factors in natural language
- Suggest next investigation steps

### 4. Rule Management

- Configure AML rules in an admin console
- Enable or disable individual risk rules
- Adjust severity weights and thresholds
- Preview rule effects before activation

### 5. User and Permission Management

- Admin, Compliance Analyst, Reviewer roles
- JWT-based authentication
- Role-based API authorization
- Full audit trail for sensitive actions

## Architecture

```text
                      ┌────────────────────────┐
                      │     Analyst Console    │
                      │        React UI        │
                      └───────────┬────────────┘
                                  │
                      ┌───────────▼────────────┐
                      │      Admin Console     │
                      │         Vue UI         │
                      └───────────┬────────────┘
                                  │
                         REST / JSON APIs
                                  │
                    ┌─────────────▼─────────────┐
                    │   Spring Cloud Gateway    │
                    └─────────────┬─────────────┘
                                  │
        ┌─────────────────────────┼─────────────────────────┐
        │                         │                         │
┌───────▼────────┐       ┌────────▼────────┐       ┌────────▼────────┐
│  Auth Service  │       │  Case Service   │       │ Risk Engine Svc │
└───────┬────────┘       └────────┬────────┘       └────────┬────────┘
        │                         │                         │
        │                ┌────────▼────────┐                │
        │                │ AI Investigator │                │
        │                └────────┬────────┘                │
        │                         │                         │
┌───────▼────────┐       ┌────────▼────────┐       ┌────────▼────────┐
│   PostgreSQL   │       │     MongoDB     │       │      Redis      │
└────────────────┘       └─────────────────┘       └─────────────────┘
```

## Tech Stack

### Frontend

- React + TypeScript + Ant Design for analyst console
- Vue 3 + TypeScript + Element Plus for admin console
- REST API integration
- Role-based routing

### Backend

- Java 17
- Spring Boot
- Spring Cloud Gateway
- Spring Security + JWT
- Spring Data JPA
- Spring Data MongoDB
- OpenFeign
- OpenAPI / Swagger

### Data and Infrastructure

- PostgreSQL for users, cases, rules, permissions
- MongoDB for wallet transactions and AI summaries
- Redis for risk cache and token/session metadata
- Docker Compose for local development

### AI Integration

- AI model API abstraction
- Risk summary generation
- Investigation report drafting
- Prompt templates with structured JSON output

## Repository Structure

```text
.
├── README.md
├── docker-compose.yml
├── docs/
│   ├── product-requirements.md
│   ├── system-design.md
│   ├── development.md
│   ├── demo-flow.md
│   └── interview-notes.md
├── backend/
│   ├── pom.xml
│   ├── api-gateway/
│   ├── auth-service/
│   ├── case-service/
│   ├── risk-engine-service/
│   └── ai-investigator-service/
├── frontend/
│   ├── analyst-console/
│   └── admin-console/
└── infra/
    ├── postgres/
    ├── mongo/
    └── redis/
```

## Local Development Roadmap

- [ ] Initialize Spring Cloud multi-module backend
- [x] Add PostgreSQL, MongoDB, and Redis with Docker Compose
- [x] Implement JWT authentication and role-protected APIs
- [x] Build wallet risk scoring APIs
- [x] Build compliance case workflow APIs
- [x] Integrate AI investigation summary API mock/provider abstraction
- [x] Build React analyst console
- [ ] Build Vue admin console
- [x] Add test data and demo scripts
- [ ] Add CI workflow

## Demo

See [docs/demo-flow.md](docs/demo-flow.md) for an end-to-end interview demo.

Quick API demo after starting local services:

```bash
./scripts/demo-api.sh
```

## Interview Positioning

This project demonstrates the ability to build a compliance-oriented, AI-powered full-stack system from scratch using React, Vue, Java Spring Cloud, REST APIs, SQL/NoSQL databases, and production-style engineering practices.
