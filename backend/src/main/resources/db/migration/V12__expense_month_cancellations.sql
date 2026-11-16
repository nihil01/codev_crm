-- Canceled recurring month remains recorded so automatic generation cannot recreate it.
ALTER TABLE expenses ADD COLUMN cancelled BOOLEAN NOT NULL DEFAULT FALSE;
