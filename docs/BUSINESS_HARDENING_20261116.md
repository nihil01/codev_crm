# CoDev CRM — business hardening status

Branch: `fix/crm-business-hardening-20261116` on top of `main` after PR #1 and PR #2.

## Changes committed
- Historical cash collections use payment transaction dates, independent of current group status.
- Teacher salary costs use immutable teacher share allocations, independent of current group status.
- Lead and manager conversions are now scoped to the selected reporting month. Acquisition cost ignores cancelled expenses.
- Audit before/after field diffs now recursively redact password/token/secret fields.
- TEACHER role with optional admin-created teacher account, isolated own-groups and own-accrued-share portal; no student details or aggregate finance.
- Client payment retries preserve a stable idempotency key for an identical invoice/amount/method.
- Unit/integration test sources added; not executed here.

## Remaining external and release dependencies
- Automatic WhatsApp/SMS sending needs a provider, consent policy and credentials. It is NOT activated; manual reminders remain.
- Flyway V20 adds a nullable, unique teacher_id reference on users. Validate on cloned PostgreSQL first.
- Run Maven backend tests, frontend npm build, Postgres migrations and role/security regression tests before merging.
- Rotate previously committed credentials and verify CSRF/session cookie behaviour behind the actual proxy.
- This is a code review branch, not a validated production deployment.
