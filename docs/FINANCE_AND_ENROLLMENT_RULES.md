# CoDev CRM — Finance / enrollment accounting specification

## Definitions (do not mix periods)
| Metric | Basis | Computation |
|---|---|---|
| **Cash received in selected month** | `payment_transactions.paid_at` | SUM(amount), regardless of invoice month / current group status |
| **Billed/expected for selected month** | `payments.payment_year/payment_month` | SUM(amount - discount) for invoices not frozen or waived |
| **Paid against this month's invoices** | Invoice month, transactions from ALL dates | SUM(receipts linked to those invoices) |
| **Open balance for this month's invoices** | Invoice month | Billed - paid against those invoices, floored at 0 per invoice |
| **Collection rate** | Invoice month | Paid against invoices / Billed, NOT calendar-month cash / billed |
| **Overall overdue debt** | As of today, Asia/Baku | SUM(open balance on invoices 6+ calendar days after due date) |
| **Profit for month** | Cash-basis operating estimate | Cash received - actual recorded expenses - earned teacher shares |

**Group lifecycle:** Changing today's status to stopped/completed NEVER removes historical cash/teacher shares. An invoice remains a real debt until paid, discounted, frozen or explicitly waived. Group stopped alone does not rewrite historical debts.

**Student counts:**
- Total non-archived students = unique student records, regardless of group.
- Active students = unique, non-archived students with status `AKTIV`, who still belong to at least one group with status `AKTIV` or `BASLAYIB` and membership `left_at IS NULL`.
- Active members within group = active, non-archived students whose membership `left_at IS NULL`. Historical exited memberships remain visible but are excluded from active counts.
- Students enrolled in several active groups count ONCE in global active students but once in each individual group.

## Withdrawal / removal
- Delete group membership and its unpaid future invoices only if there are no cash transactions and no already-matured unpaid debts.
- Otherwise set group membership `left_at` / reason, leave historical cash and past due debt intact, waive only future unpaid invoices with due dates after the exit date.
- Never automatically mark the student globally `AYRILIB` when they leave just one group; they may be in other groups.
- Existing rows are not silently migrated to exited; the user explicitly invokes the operation.
- A departed student cannot create a new invoice on that membership. Re-enrollment into the same historical membership currently requires a dedicated admin workflow; creating a duplicate membership is blocked.

## Reconciliation example
Student owes 300 AZN for November, discount 20 AZN, receives 100 AZN in December:
- November expected: 280 AZN; paid against November invoice: 100 AZN; outstanding: 180 AZN; collection rate: 35.71%.
- November cash: 0 AZN (assuming no November receipt).
- December cash: 100 AZN.
- November's active-invoice totals still show the 100 AZN paid regardless of cash date.

**Important:** Financial correctness of live data cannot be certified from source review alone.
Restore a disposable copy of the PostgreSQL database and reconcile invoices, payments,
discounts, freezes, waivers, recurring expenses, teacher allocations and unique students.
Do not run new Flyway V22 against the only copy of production data.
