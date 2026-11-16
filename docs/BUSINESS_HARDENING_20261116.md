# CoDev CRM — business logic completion draft

Branch: `fix/crm-business-hardening-20261116`. Base: merged PR #1 and #2 in `main`.

## Delivered in this branch
- Historical cash and teacher shares use transaction/allocation dates, not a group's current status.
- Monthly lead attribution and marketing acquisition costs exclude unrelated periods and cancelled expenses.
- Recursive audit redaction of nested password/token/secret fields.
- Optional teacher accounts, `TEACHER` role, admin account activation, isolated own-group and own-earned-share page. Teacher cannot access student, lead, billing or admin endpoints.
- Safe repeat attempts on partial payments: identical invoice/method/amount reuse the same client idempotency key.
- Automated reminder infrastructure: per-student explicit consent (OFF by default); overdue message outbox, unique invoice+kind and stable idempotency key; retry up to 3 attempts, stale attempt recovery, and admin dispatch history.
- JUnit test sources for monthly accounting, audit redaction, teacher isolation, own-group access, missing consent and reminder deduplication.

## Config and activation
Default is **manual reminders only**. The new webhook transporter supports:
```env
CRM_REMINDERS_MODE=MANUAL
CRM_REMINDERS_WEBHOOK_URL=
CRM_REMINDERS_WEBHOOK_TOKEN=
```
To enable provider-bound sending, the administrator must deliberately set `CRM_REMINDERS_MODE=WEBHOOK`, configure an HTTPS provider adapter endpoint and bearer token, and obtain explicit permission from each recipient (student consent field).

The adapter must accept POST JSON:
```json
{"phone":"+994...", "message":"...", "channel":"PAYMENT_REMINDER"}
```
The CRM sends `Authorization: Bearer ...` and `Idempotency-Key: <stable UUID>`. The adapter must honor the idempotency key to avoid duplicate third-party delivery after network failure. A 2xx HTTP response is treated as accepted, **not** proof that a carrier delivered the message. This is a generic adapter; it is not a direct integration with any specific WhatsApp/SMS provider. Do not enable until the adapter contract, personal-data handling, opt-ins and delivery terms have been verified.

The daily job runs at 09:00 Asia/Baku. It queues the first overdue notice only after 5 full grace days and skips frozen debts, suspended groups, archives, absent phones and absent opt-in. Sending is disabled without valid HTTPS config and token.

## Schema
- Flyway `V20__teacher_accounts.sql`: nullable unique users.teacher_id.
- Flyway `V21__reminder_outbox.sql`: student consent default false and unique reminder outbox.

## Not production-ready
Backend tests, frontend compilation and database migrations were not executed in this tool environment. The PR must remain Draft. Run:
```bash
cd frontend && npm ci && npm run build
cd ../backend && mvn test && mvn package
```
Use a disposable PostgreSQL clone and verify all Flyway V1–V21 migrations. Review Spring Security, login cookies/CSRF, teacher account boundaries, historical finance, recurring expenses, consent and webhook retry semantics. Rotate previously committed secrets.

Automatic outreach is opt-in and still depends on a configured, verified third-party adapter. No message is dispatched by default.
