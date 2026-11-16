-- Stable recurring expense identity; legacy monthly overrides remain untouched.
CREATE TABLE recurring_expense_rules (
  id BIGSERIAL PRIMARY KEY,
  type VARCHAR(50) NOT NULL,
  amount NUMERIC(12,2) NOT NULL CHECK(amount > 0),
  description VARCHAR(255),
  ad_channel VARCHAR(255),
  start_year INTEGER NOT NULL,
  start_month INTEGER NOT NULL CHECK (start_month BETWEEN 1 AND 12),
  active BOOLEAN NOT NULL DEFAULT TRUE,
  legacy_key VARCHAR(255) UNIQUE,
  created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
  updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
ALTER TABLE expenses ADD COLUMN recurring_rule_id BIGINT REFERENCES recurring_expense_rules(id);
CREATE UNIQUE INDEX uq_expense_rule_month
  ON expenses(recurring_rule_id,expense_year,expense_month)
  WHERE recurring_rule_id IS NOT NULL;

-- Legacy expenses have no stable rule ID. Disambiguate repeated identical expenses
-- with a position within each month; do not delete or overwrite historical amounts.
CREATE TEMP TABLE _legacy_expense_rank AS
SELECT e.id,e.type,e.description,e.ad_channel,e.amount,e.expense_year,e.expense_month,
  row_number() OVER (
     PARTITION BY e.type,e.description,e.ad_channel,e.expense_year,e.expense_month
     ORDER BY e.id
  ) AS slot
FROM expenses e WHERE e.is_recurring = TRUE;

-- Last recorded month defines the forward-looking amount. Earliest defines start date.
WITH template AS (
 SELECT DISTINCT ON (type,description,ad_channel,slot)
   type,description,ad_channel,slot,amount
 FROM _legacy_expense_rank
 ORDER BY type,description NULLS FIRST,ad_channel NULLS FIRST,slot,expense_year DESC,expense_month DESC,id DESC
), first_period AS (
 SELECT type,description,ad_channel,slot,
   MIN(make_date(expense_year,expense_month,1)) AS first_date
 FROM _legacy_expense_rank
 GROUP BY type,description,ad_channel,slot
)
INSERT INTO recurring_expense_rules(type,amount,description,ad_channel,start_year,start_month,active,legacy_key)
SELECT t.type,t.amount,t.description,t.ad_channel,
       EXTRACT(YEAR FROM f.first_date)::INT,EXTRACT(MONTH FROM f.first_date)::INT,
       TRUE,md5(jsonb_build_array(t.type,t.description,t.ad_channel,t.slot)::TEXT)
FROM template t JOIN first_period f
  ON t.type=f.type AND t.description IS NOT DISTINCT FROM f.description
 AND t.ad_channel IS NOT DISTINCT FROM f.ad_channel AND t.slot=f.slot;

UPDATE expenses e SET recurring_rule_id=rule.id
FROM _legacy_expense_rank source
JOIN recurring_expense_rules rule
  ON rule.legacy_key=md5(jsonb_build_array(source.type,source.description,source.ad_channel,source.slot)::TEXT)
WHERE e.id=source.id;

DROP TABLE _legacy_expense_rank;
