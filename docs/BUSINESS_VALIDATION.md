# CoDev CRM — Business logic release checklist
**Date:** 2026-11-16. **Target:** React / Spring Boot / PostgreSQL CRM.

## Implemented in source
- Roles ADMIN / MANAGER, manager accounts, protected finance APIs, leads, course/group schedules and capacity, student registration/exit data
- Student freeze periods; monthly automatic freezing while status is DONDURULUB, and from the month **after** departure for AYRILIB. Paid invoice months are not automatically waived
- Real receipt ledger with part payments, remaining balances, five-day grace, admin-only discounts with mandatory reasons, downloadable PDF receipts, request-level idempotency keys
- Teacher commissions snapshotted from real receipts, effective assignment periods and historical teacher share allocations; prevents retroactive payroll rewriting
- Persistent certificate issuance registry, completed-group + enrolled-student + zero-debt checks and idempotent reprints
- Durable recurring-expense rules with unique ID and monthly unique constraint; independent monthly adjustments, cancellations, restores and catch-up after downtime
- Migrated legacy recurring rules are deactivated until reviewed to avoid generating fictitious charges
- Admin-only audit showing actor, timestamp, entity, old/new field values, excluding passwords/secrets; HTTP-only request log remains secondary evidence
- Financial/lead/manager/group/teacher reports, Excel export, 3- or 6-month pessimistic/base/optimistic forecast

## Requires business-owner decision (not automatically enabled)
- Teacher panel login: original questionnaire contradicted itself. Teachers are managed as records, but are **not** user accounts. Current choice: no teacher login yet.
- First payment default: month 0 (enrolment/course-start day). Set `CRM_FIRST_PAYMENT_DELAY_MONTHS=1` for payment after a month. Changing the config **does not** retroactively rewrite invoices.
- WhatsApp/SMS: manager reminder list and copy-ready messages exist. Automatic sending has NOT been implemented because it requires a provider, payment account, opt-in, consent and a clear user decision.

## Mandatory validation before production
1. Back up PostgreSQL and test restore.
2. On a disposable copy of the existing database, run all Flyway migrations through V19. Check V11 legacy migration for descriptions over 255 characters and V14 teacher percentage/rate history assumptions.
3. On a fresh isolated database test V1–V19. Verify unique constraints: recurring rule per month, payment idempotency key per invoice, certificate per student and group.
4. Build frontend: `cd frontend && npm ci && npm run build`.
5. Build/test backend: `cd backend && mvn test && mvn package -DskipTests`. Set `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `CRM_BOOTSTRAP_ADMIN_EMAIL`, and `CRM_BOOTSTRAP_ADMIN_PASSWORD` for test DB; never run against production during testing.
6. Verify admin/manager authorization with real HTTP sessions; CSRF headers and cookies must work behind the production reverse proxy.
7. Run the accounting scenarios below on isolated fixtures.
8. Review historical recurring-rule templates and explicitly enable only valid subscriptions.
9. Rotate all previously committed passwords and keys: removing a secret from the current source does not revoke it.
10. Only merge/deploy after migration, security and accounting QA has passed.

## Acceptance scenarios
| Scenario | Expected |
|---|---|
| Invoice 300 AZN; paid 100 + 80 | 180 collected, 120 outstanding |
| Retransmit POST with the same Idempotency-Key | Only one receipt / teacher allocation |
| Due 3 Nov; date 8 Nov | Not overdue until after 8 Nov |
| Frozen month | No debt; payments blocked; manager cannot bypass student freeze by unfreezing invoice |
| DONDURULUB spans month boundary | Next unpaid month frozen automatically |
| AYRILIB on 16 Nov | Existing November dues retained; December+ unpaid months frozen |
| Two teacher assignments | Total percentages <= 100; pay allocated per actual receipt |
| Remove teacher after receipt | Historic share allocations retained; end assignment |
| Recurring expenses with same label | Each has a distinct rule ID and monthly record |
| Cancel one recurring month | Excluded from expenses, not recreated; may restore |
| Inactive legacy recurring template | No automatic historical catch-up |
| Group suspended | Excluded from active-group counts and group reporting |
| Certificate incomplete/indebted | Server refuses |
| Certificate repeat download | No duplicate issuance row or repeated email |
| Manager attempts /reports, /expenses, /audit | 403 |
| Edit a lead/student/discount | Admin audit has old and new values |
| Update manager password | Password is not written to audit |

## Known verification limits
The code was authored and pushed using the GitHub connection. **No local Maven build, npm build, Flyway execution on a test database or end-to-end tests were run in this session.** Added JUnit tests are source files, not passing results. GitHub branch status checks have not reported a successful build; please run the validation commands above. Previous PRs were merged to main during development by another actor, so do not assume main is production-ready.

## Deployment settings
- `CRM_FIRST_PAYMENT_DELAY_MONTHS=0` or `1`
- `CRM_BOOTSTRAP_ADMIN_EMAIL`, `CRM_BOOTSTRAP_ADMIN_PASSWORD`
- `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`
- `MAIL_USERNAME`, `MAIL_PASSWORD`
- `CRM_CORS_ALLOWED_ORIGINS`

This document separates code presence from tested readiness.
