# Product Requirements: ChainGuard CaseHub

## 1. Product Vision

ChainGuard CaseHub is a crypto compliance case management platform that helps compliance teams detect risky wallets, investigate suspicious transactions, and create explainable AML review records with AI assistance.

The product is designed for exchange compliance analysts who need to process large volumes of alerts, reduce repetitive manual review, and maintain a traceable audit trail for each investigation.

## 2. Target Users

### Compliance Analyst

- Reviews suspicious wallet and transaction alerts
- Opens and updates investigation cases
- Writes investigation notes
- Escalates high-risk cases

### Compliance Reviewer

- Reviews escalated cases
- Approves case closure
- Validates investigation quality
- Checks audit logs and evidence

### Compliance Admin

- Manages AML rules
- Configures risk thresholds
- Manages analyst access
- Monitors platform health

## 3. Main Use Cases

### UC-01: Investigate a Wallet

1. Analyst enters a wallet address.
2. System retrieves transaction history and known risk signals.
3. Risk engine applies AML rules.
4. System returns a risk score, risk level, and triggered rules.
5. Analyst opens a case if the wallet is suspicious.

### UC-02: Manage an AML Case

1. Alert creates a case automatically or analyst creates one manually.
2. Case is assigned to an analyst.
3. Analyst adds notes and evidence.
4. Analyst requests AI investigation summary.
5. Analyst escalates or closes the case.
6. Reviewer validates and approves final action.

### UC-03: Configure AML Rules

1. Admin opens the rule management console.
2. Admin creates or edits rule thresholds.
3. System validates the rule configuration.
4. Admin previews rule impact.
5. Admin publishes the rule.
6. System records the change in audit logs.

### UC-04: Generate AI Investigation Summary

1. Analyst selects a case.
2. System collects wallet transactions, triggered rules, notes, and risk score.
3. AI Investigator service sends a structured prompt to an AI model API.
4. AI returns a summary, risk explanation, and recommended next steps.
5. Analyst reviews and edits the generated content before saving.

## 4. Functional Requirements

### Wallet Risk Scoring

- Search wallet by address.
- Display wallet transaction timeline.
- Calculate risk score from rule results.
- Classify risk as Low, Medium, High, Critical.
- Show rule explanations and evidence.

### Case Management

- Create cases from risk alerts.
- Assign case owner.
- Update case status.
- Add comments, labels, and evidence links.
- Record all updates in audit logs.
- Search and filter cases by status, risk level, assignee, wallet, and date.

### AI Investigation

- Generate case summary.
- Generate wallet behavior explanation.
- Suggest next investigation steps.
- Return structured JSON response.
- Allow analyst to accept, edit, or reject AI output.

### Rule Management

- Create, update, enable, and disable rules.
- Configure rule severity and threshold.
- Version rule changes.
- Record rule publishing history.

### Authentication and Authorization

- Login with username and password.
- Issue JWT access token.
- Support Admin, Analyst, and Reviewer roles.
- Protect APIs with role-based access control.

## 5. Non-Functional Requirements

### Security

- JWT authentication for all protected APIs.
- Role-based permission checks.
- Audit logs for sensitive operations.
- No secrets committed to repository.

### Reliability

- Services should return structured error responses.
- AI failures should not block manual investigation.
- Risk scoring should be deterministic and explainable.

### Performance

- Wallet risk lookup should return within 2 seconds for demo datasets.
- Case list APIs should support pagination and filtering.
- Redis cache should be used for repeated wallet risk queries.

### Observability

- Each service should expose a health endpoint.
- Logs should include request ID and user ID where possible.
- Important business events should be auditable.

## 6. MVP Scope

### In Scope

- JWT login
- Wallet risk scoring with demo transaction data
- Case CRUD and status workflow
- AML rule configuration
- AI summary mock/integration abstraction
- React analyst console
- Vue admin console
- Docker Compose local dependencies

### Out of Scope

- Real exchange account integration
- Real KYC provider integration
- Real blockchain indexer integration
- Production deployment pipeline
- Enterprise SSO

## 7. Success Metrics

- Analyst can investigate a wallet and open a case in under 3 minutes.
- AI summary reduces manual report drafting time.
- All rule hits are explainable with evidence.
- Case actions are fully traceable through audit logs.
