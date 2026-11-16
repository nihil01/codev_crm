ALTER TABLE expenses ADD COLUMN ad_channel VARCHAR(255);
CREATE INDEX idx_expenses_ad_channel ON expenses(ad_channel);
