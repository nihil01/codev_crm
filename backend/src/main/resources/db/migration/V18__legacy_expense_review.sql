-- Legacy recurring records had no reliable start/end dates or stable identities.
-- Do not auto-generate potentially fictitious historical charges from migration guesses.
-- Administrator can inspect and explicitly reactivate each inferred rule.
UPDATE recurring_expense_rules
   SET active=FALSE
 WHERE legacy_key IS NOT NULL AND active=TRUE;
