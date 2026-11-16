-- Safe retry of partial payment POSTs.
ALTER TABLE payment_transactions ADD COLUMN idempotency_key VARCHAR(128);
CREATE UNIQUE INDEX uq_payment_transaction_request
    ON payment_transactions(payment_id,idempotency_key)
    WHERE idempotency_key IS NOT NULL;
