ALTER TABLE subscription DROP CONSTRAINT IF EXISTS subscription_status_check;

ALTER TABLE subscription ADD CONSTRAINT subscription_status_check
    CHECK (status IN ('PENDING', 'ACTIVE', 'CANCELLED', 'PAST_DUE', 'TRIALING', 'FAILED'));