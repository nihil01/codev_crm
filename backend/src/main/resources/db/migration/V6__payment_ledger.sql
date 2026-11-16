-- V6: invoice/payment separation. Existing paid invoices are backfilled as immutable transactions.
ALTER TABLE payments ADD COLUMN due_date DATE;
ALTER TABLE payments ADD COLUMN discount_amount NUMERIC(12,2) NOT NULL DEFAULT 0;
ALTER TABLE payments ADD COLUMN discount_reason TEXT;
ALTER TABLE payments ADD COLUMN frozen BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE payments p SET due_date = (
    make_date(p.payment_year, p.payment_month, 1) +
    (LEAST(EXTRACT(DAY FROM g.start_date)::INT,
      EXTRACT(DAY FROM (make_date(p.payment_year,p.payment_month,1) + INTERVAL '1 month - 1 day'))::INT)-1)
) FROM course_groups g WHERE g.id=p.group_id AND p.due_date IS NULL;

CREATE TABLE payment_transactions (
    id BIGSERIAL PRIMARY KEY,
    payment_id BIGINT NOT NULL REFERENCES payments(id) ON DELETE RESTRICT,
    amount NUMERIC(12,2) NOT NULL CHECK(amount > 0),
    method VARCHAR(50) NOT NULL,
    paid_at TIMESTAMP NOT NULL,
    recorded_by VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
CREATE INDEX idx_paytx_invoice ON payment_transactions(payment_id);
CREATE INDEX idx_paytx_paid_at ON payment_transactions(paid_at);

INSERT INTO payment_transactions(payment_id,amount,method,paid_at,recorded_by)
SELECT id, amount, COALESCE(payment_method,'NAGD'),
       COALESCE(payment_date::timestamp,created_at), 'migration'
FROM payments WHERE status='ODENILIB' AND amount>0;

CREATE INDEX idx_payment_due ON payments(due_date);
