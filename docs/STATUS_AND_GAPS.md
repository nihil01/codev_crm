# CRM V2 — implementation status (2026-11-16)

Branch: `feat/education-crm-v2-20261116`; base: `main`.
Full approved requirements: [CRM_TZ_AZ.md](CRM_TZ_AZ.md).

## Implemented in this draft branch
- Role ADMIN/MANAGER; administrator-only finance, expenses, aggregate dashboard, report, forecast, certificate and audit endpoints; manager student CRUD and existing full-invoice payment recording.
- Administrator manager account creation/activation/deactivation via React UI and API; password stored as BCrypt hash. Existing manager sessions checked for active user on protected APIs.
- Leads pipeline with 7 sources, 6 stages, consultation timestamp, free-form lost reason, admin assignment and a React Kanban-like view.
- Student source, parent details, address, note, status, exit reason; FİN and email optional; non-empty FİN unique after normalization.
- Flyway V5 for leads, audit events and student metadata.
- Administrator-only request-level activity feed (actor, time, method, endpoint, success status); **not full before/after change diffs**.
- Removes plaintext default credentials from main and Docker application configuration, requires explicit bootstrap admin environment credentials for empty databases.
- Hides admin navigation and aggregate payment KPI cards from managers.

## Not yet implemented — must not merge as completed TZ
- Payment transactions/partial payments, remaining debt, exact due dates and five-day grace, freeze/billing policy, admin-only discounts with reasons and receipts.
- New course catalog and group schedule/capacity/mode/status model.
- Teacher payroll reconciliation based on actual collected transactions.
- Automated and editable recurring monthly expenses, advertisement channel attribution.
- Full Excel export, debtors report, lead-source attribution, manager conversion report and CAC.
- Three forecast scenarios for student count, revenue and profit at 3–6 months.
- Certificates gated by completed group plus zero debt.
- Full field-level audit old/new values (no passwords or secrets), CSRF protection for session-backed SPA, login throttling, production security review.
- Automated integration tests, frontend build, PostgreSQL migration in a test DB and regression QA.

## Deployment warnings
1. **Do not deploy or merge this untested draft.** No CI status checks were reported for the current branch; Maven/TypeScript and database migration were not executed in a suitable runtime.
2. A previously committed admin password and other secrets require immediate rotation; changing latest source files cannot erase Git history or revoke leaked values.
3. For a fresh database set `CRM_BOOTSTRAP_ADMIN_EMAIL` and a strong `CRM_BOOTSTRAP_ADMIN_PASSWORD` (at least 12 characters), as well as `DB_PASSWORD` and `MAIL_PASSWORD` in the deployment environment.
4. For production rollout, back up the real PostgreSQL database and validate Flyway V5 on a disposable clone before any merge. The case-insensitive FİN unique index will reject preexisting case-variant duplicates.
5. Billing due policy, teacher login, and reminder approach remain undecided in the approved requirements.

## Acceptance checks
- MANAGER: 403 for `/api/dashboard`, `/api/expenses/recurring`, `/api/reports/**`, `/api/forecast`, `/api/audit`, `/api/groups/1/teachers`, `/api/payments/collected/**`.
- MANAGER: can create/edit students, update leads without reassigning ownership, and record an existing payment.
- ADMIN: can create/deactivate manager and only admin can assign lead to a different manager.
- Lost lead requires a reason; consultation stage requires a datetime; duplicates of supplied FİN are rejected.
- Disabled manager receives 401 when attempting previously authenticated protected endpoint access.
