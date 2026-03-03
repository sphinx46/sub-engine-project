CREATE TABLE IF NOT EXISTS subscription (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    plan_type VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    next_billing_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_subscription_next_billing_date ON subscription(next_billing_date);