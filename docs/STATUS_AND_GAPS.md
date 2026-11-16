# CoDev CRM V2 — current implementation and release status
**As of 2026-11-16.** Approved requirements: [CRM_TZ_AZ.md](CRM_TZ_AZ.md).
Detailed testing and migration guide: [BUSINESS_VALIDATION.md](BUSINESS_VALIDATION.md).

## Source code implemented
- ADMIN / MANAGER access; admin-only financial reports, aggregate metrics and audit; manager student and receipt operations.
- Lead capture, manager assignment, consultation, lost reason and lead-to-student conversion.
- Courses and groups with schedule, classroom/online, room, max capacity, teachers/mentors and status transitions.
- Students with family details, optional unique FİN, per-group monthly fees, pause and departure reasons.
- Separate invoice and immutable receipt ledgers: partial payments, discounts (admin-only with reason), five-day grace, debt, receipt PDF, idempotency keys.
- Monthly student freezes and ongoing paused/departed billing protection, including exit effective date.
- Actual collected-revenue teacher percentages stored as historical transaction allocations; teacher assignment end dates do not recalculate past shares.
- Unique recurring-expense rules with month-by-month overrides, cancellation/restore, downtime recovery and legacy rule review.
- Monthly profit/loss, teacher and group earnings, debtors, lead and manager outcomes, acquisition cost, Excel export and three forecast scenarios.
- Certificates require a completed group, enrollment and zero debt. Issuance is registered and repeat downloads are idempotent.
- Admin-only audit with per-field before/after data, actor, time and secret-field filtering.

## Remaining decisions, not bugs
- **Teacher login panel:** original answers contradicted each other. Working policy: no teacher credentials yet; existing teacher records and commissions are managed by admin.
- **Due date:** first payment is due at start by default (`CRM_FIRST_PAYMENT_DELAY_MONTHS=0`); set `1` for payment a month later. Existing invoices are not automatically rewritten.
- **WhatsApp/SMS:** manual manager reminder list with prepared text, **not** automatic sending. A supported provider account, opt-in/consent and explicit selection are needed for automated delivery.
- Attendance was explicitly excluded from this development phase; monthly targets were excluded too.

## QA and deployment status
- GitHub PR #1 and #2 were merged to `main` by another actor during development.
- New hardening and tests are in draft PR #3: https://github.com/nihil01/codev_crm/pull/3
- **Not verified:** Maven compilation, npm compilation, Flyway migrations on old/new PostgreSQL, browser cookie/CSRF checks and end-to-end financial tests. Added JUnit tests have **not** been run here. No green CI report has been observed.
- Old committed credentials must be rotated and the database must be backed up before schema migration. Imported recurring rules require manual review; earlier generated legacy rows, if any, may also need reconciliation.
- Do not equate merged source code with a validated production release. See BUSINESS_VALIDATION.md for the acceptance gate.
