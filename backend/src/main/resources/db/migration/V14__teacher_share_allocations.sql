-- Commission is assigned at time of a real payment, not retroactively recalculated.
CREATE TABLE teacher_share_allocations(
    id BIGSERIAL PRIMARY KEY,
    payment_transaction_id BIGINT NOT NULL REFERENCES payment_transactions(id) ON DELETE RESTRICT,
    group_teacher_id BIGINT NOT NULL REFERENCES group_teachers(id) ON DELETE RESTRICT,
    teacher_id BIGINT NOT NULL REFERENCES teachers(id) ON DELETE RESTRICT,
    group_id BIGINT NOT NULL REFERENCES course_groups(id) ON DELETE RESTRICT,
    percentage NUMERIC(5,2) NOT NULL CHECK(percentage>0 AND percentage<=100),
    amount NUMERIC(12,2) NOT NULL CHECK(amount>=0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE(payment_transaction_id,group_teacher_id)
);
CREATE INDEX idx_teacher_share_group ON teacher_share_allocations(group_id);
CREATE INDEX idx_teacher_share_teacher ON teacher_share_allocations(teacher_id);

-- Backfill existing receipts with rates that would have applied in their payment month.
-- Historical changes to rates cannot be recovered if never stored; review migrated amounts.
INSERT INTO teacher_share_allocations(payment_transaction_id,group_teacher_id,teacher_id,group_id,
                                      percentage,amount,created_at)
SELECT tx.id,gt.id,gt.teacher_id,p.group_id,gt.salary_percentage,
       ROUND(tx.amount * gt.salary_percentage / 100,2),tx.paid_at
FROM payment_transactions tx
JOIN payments p ON p.id=tx.payment_id
JOIN group_teachers gt ON gt.group_id=p.group_id
WHERE gt.salary_percentage>0 AND gt.salary_percentage<=100
  AND (gt.effective_year<EXTRACT(YEAR FROM tx.paid_at)::INTEGER
       OR (gt.effective_year=EXTRACT(YEAR FROM tx.paid_at)::INTEGER
           AND gt.effective_month<=EXTRACT(MONTH FROM tx.paid_at)::INTEGER))
ON CONFLICT(payment_transaction_id,group_teacher_id) DO NOTHING;
