# Interview Notes: ChainGuard CaseHub

## 1. 30-Second Pitch

ChainGuard CaseHub is an AI-powered crypto compliance case management platform. It helps compliance analysts evaluate wallet risk, manage AML investigation cases, configure risk rules, and generate AI-assisted investigation summaries. I built it as a full-stack microservice system using React, Vue, Java Spring Cloud, PostgreSQL, MongoDB, Redis, and REST APIs.

## 2. Chinese Pitch

ChainGuard CaseHub 是一个面向加密交易所合规团队的链上风险监控与案件管理平台。系统可以对钱包地址进行 AML 风险评分，把高风险地址转成调查案件，并通过 AI 自动生成调查摘要和风险解释。后端使用 Java Spring Cloud 拆分为 Gateway、Auth、Case、Risk Engine 和 AI Investigator 服务，前端分别用 React 做分析师工作台，用 Vue 做规则管理后台。

## 3. Why I Built It

This project was designed around a real compliance engineering scenario. Crypto exchanges need internal tools that combine transaction monitoring, AML rule evaluation, case workflow, audit logs, and AI-assisted investigation. The project demonstrates that I can build a product-oriented full-stack system, not just isolated frontend or backend features.

## 4. Architecture Talking Points

- I used Spring Cloud Gateway as a unified API entry point.
- I separated core domains into services: auth, cases, risk scoring, and AI investigation.
- PostgreSQL stores structured business data such as users, cases, rules, and audit logs.
- MongoDB stores flexible wallet transaction documents and future AI analysis artifacts.
- Redis is planned for repeated wallet risk result caching.
- React is used for the analyst console because it is suitable for interactive investigation workflows.
- Vue is used for the admin console to demonstrate cross-framework frontend capability.

## 5. Important Engineering Decisions

### Explainable Risk Scoring

Instead of returning only a numeric score, the risk engine returns triggered rules, severity, descriptions, and score impact. This is important for compliance because analysts need to explain why a case was escalated.

### Human-in-the-Loop AI

AI-generated summaries are treated as drafts. Analysts can review, edit, accept, or reject AI output. This avoids blindly relying on model responses in compliance workflows.

### Auditability

Sensitive actions such as case status changes and rule updates are designed to be written to audit logs. This makes investigations traceable.

### Service Separation

Risk scoring and AI investigation are separate services because they evolve differently. Risk scoring should be deterministic and rule-based, while AI investigation may use external model providers and require different failure handling.

## 6. Challenges and How I Would Handle Them

### Challenge: AI Output Reliability

Solution:

- Use structured JSON output.
- Validate model responses.
- Keep human review before saving final reports.
- Log prompts and generated results with sensitive data minimization.

### Challenge: Large Transaction Volumes

Solution:

- Store raw transaction documents in MongoDB or a dedicated analytics store.
- Use pagination and time-window queries.
- Cache repeated risk results in Redis.
- Move ingestion to Kafka in a future version.

### Challenge: Rule Versioning

Solution:

- Add version field to each rule.
- Keep rule publish history.
- Store which rule version generated each case risk result.

## 7. Resume Bullets

- Built an AI-powered crypto compliance case management platform using React, Vue, Java Spring Cloud, PostgreSQL, MongoDB, and Redis.
- Designed AML risk scoring APIs that return explainable rule hits, severity levels, score impact, and wallet risk classification.
- Implemented a compliance case workflow covering case creation, assignment-ready status management, auditability, and AI-assisted investigation drafts.
- Created separate React analyst console and Vue admin console to support wallet investigation and AML rule management workflows.
- Designed microservice architecture with Spring Cloud Gateway, Auth Service, Case Service, Risk Engine Service, and AI Investigator Service.

## 8. Possible Interview Questions

### Q: Why did you use both React and Vue?

A: The JD requires both React and Vue. I used React for the analyst console because the investigation workflow benefits from a highly interactive UI. I used Vue for the admin console because rule management is more form- and table-oriented, and Vue with Element Plus is efficient for that use case.

### Q: How do you ensure AI summaries are safe for compliance?

A: I treat AI output as an assistant draft, not an automatic decision. The system uses structured prompts, validates JSON output, requires analyst review, and keeps audit logs. I would also avoid unnecessary PII in prompts and track which source data was used.

### Q: Why split risk engine and AI investigator?

A: Risk scoring must be deterministic, explainable, and testable. AI generation is probabilistic and may depend on external providers. Separating them improves reliability, maintainability, and failure isolation.

### Q: What would you improve next?

A: I would add real persistence through Spring Data JPA and MongoDB repositories, implement JWT security, add Redis caching for risk results, add test coverage, and introduce Kafka for asynchronous alert ingestion.
