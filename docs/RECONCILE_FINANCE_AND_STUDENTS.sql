-- READ-ONLY checks to compare CRM metrics against PostgreSQL after Flyway V22.
-- Run on a disposable / restored copy of the production database.
-- Never adjust historical cash transactions to force report totals to match.

-- 1. Invoiced/expected, paid against invoice period and outstanding (by billing period).
WITH receipt AS (
    SELECT payment_id, SUM(amount)::numeric(12,2) AS paid
    FROM payment_transactions
    GROUP BY payment_id
), invoice AS (
    SELECT p.payment_year, p.payment_month, p.id,
        CASE WHEN p.frozen OR p.status IN ('DONDURULUB','BAGISLANIB')
            THEN 0::numeric
            ELSE GREATEST(p.amount-COALESCE(p.discount_amount,0),0) END AS expected,
        CASE WHEN p.frozen OR p.status IN ('DONDURULUB','BAGISLANIB')
            THEN 0::numeric ELSE COALESCE(r.paid,0) END AS paid_against,
        CASE WHEN p.frozen OR p.status IN ('DONDURULUB','BAGISLANIB')
            THEN 0::numeric
            ELSE GREATEST(p.amount-COALESCE(p.discount_amount,0)-COALESCE(r.paid,0),0) END AS outstanding
    FROM payments p LEFT JOIN receipt r ON r.payment_id=p.id
)
SELECT payment_year, payment_month,
       SUM(expected) AS invoiced_azn,
       SUM(paid_against) AS paid_for_invoice_month_azn,
       SUM(outstanding) AS open_balance_azn,
       ROUND(100*SUM(paid_against)/NULLIF(SUM(expected),0),2) AS collection_rate_percent
FROM invoice GROUP BY payment_year,payment_month ORDER BY payment_year,payment_month;

-- 2. Actual receipts by cash transaction month, NOT invoice billing month.
SELECT EXTRACT(YEAR FROM paid_at)::integer AS cash_year,
       EXTRACT(MONTH FROM paid_at)::integer AS cash_month,
       COUNT(*) AS cash_transactions, SUM(amount) AS cash_received_azn
FROM payment_transactions
GROUP BY 1,2 ORDER BY 1,2;

-- 3. Actual teacher allocations by receipt month.
SELECT EXTRACT(YEAR FROM tx.paid_at)::integer AS cash_year,
       EXTRACT(MONTH FROM tx.paid_at)::integer AS cash_month,
       SUM(a.amount) AS allocated_teacher_share_azn
FROM teacher_share_allocations a
JOIN payment_transactions tx ON tx.id=a.payment_transaction_id
GROUP BY 1,2 ORDER BY 1,2;

-- 4. Actual expenses by accounting month. Cancelled records are excluded.
SELECT expense_year,expense_month,
       SUM(amount) AS expenses_azn,
       SUM(amount) FILTER (WHERE is_recurring) AS recurring_azn,
       SUM(amount) FILTER (WHERE NOT is_recurring) AS one_time_azn
FROM expenses WHERE NOT cancelled
GROUP BY expense_year,expense_month
ORDER BY expense_year,expense_month;

-- 5. All-period overdue as of now in Baku, using five full grace days.
WITH receipt AS (
    SELECT payment_id,SUM(amount) AS paid FROM payment_transactions GROUP BY payment_id
)
SELECT COUNT(*) AS overdue_invoices,
       COALESCE(SUM(GREATEST(p.amount-COALESCE(p.discount_amount,0)-COALESCE(r.paid,0),0)),0)
           AS overdue_debt_azn
FROM payments p LEFT JOIN receipt r ON r.payment_id=p.id
WHERE NOT p.frozen AND p.status NOT IN ('DONDURULUB','BAGISLANIB')
  AND COALESCE(p.due_date,make_date(p.payment_year,p.payment_month,1))+5
       < (now() AT TIME ZONE 'Asia/Baku')::date
  AND p.amount-COALESCE(p.discount_amount,0)-COALESCE(r.paid,0)>0;

-- 6. Distinct active students vs group membership (a student in two groups counts once).
SELECT COUNT(DISTINCT s.id) AS global_active_students
FROM group_students gs
JOIN students s ON s.id=gs.student_id
JOIN course_groups g ON g.id=gs.group_id
WHERE gs.left_at IS NULL AND NOT s.archived AND s.status='AKTIV'
  AND g.status IN ('AKTIV','BASLAYIB');

SELECT g.id,g.group_number,
       COUNT(*) FILTER (WHERE gs.left_at IS NULL AND s.id IS NOT NULL
           AND NOT s.archived AND s.status='AKTIV') AS active_students,
       COUNT(gs.id) AS historic_memberships
FROM course_groups g
LEFT JOIN group_students gs ON gs.group_id=g.id
LEFT JOIN students s ON s.id=gs.student_id
GROUP BY g.id,g.group_number ORDER BY g.group_number;

-- 7. Exceptions that should return zero rows.
-- Duplicate invoice per student/group/month.
SELECT student_id,group_id,payment_year,payment_month,COUNT(*) AS duplicates
FROM payments
GROUP BY student_id,group_id,payment_year,payment_month
HAVING COUNT(*)>1;

-- Overpaid or negative net invoice (source data should be investigated, NOT auto-rewritten).
WITH receipt AS (
  SELECT payment_id,SUM(amount) AS paid FROM payment_transactions GROUP BY payment_id
)
SELECT p.id,p.student_id,p.group_id,p.amount,p.discount_amount,
       COALESCE(r.paid,0) AS paid
FROM payments p LEFT JOIN receipt r ON r.payment_id=p.id
WHERE p.amount<COALESCE(p.discount_amount,0)
   OR COALESCE(r.paid,0)>p.amount-COALESCE(p.discount_amount,0);

-- Group teacher share larger than collected cash for a single receipt.
SELECT tx.id AS transaction_id,tx.amount AS cash_amount,
       SUM(a.amount) AS teacher_allocated
FROM payment_transactions tx
JOIN teacher_share_allocations a ON a.payment_transaction_id=tx.id
GROUP BY tx.id,tx.amount
HAVING SUM(a.amount)>tx.amount;

-- Repeated recurring-expense rule instance in same calendar month.
SELECT recurring_rule_id,expense_year,expense_month,COUNT(*)
FROM expenses WHERE recurring_rule_id IS NOT NULL
GROUP BY recurring_rule_id,expense_year,expense_month
HAVING COUNT(*)>1;
