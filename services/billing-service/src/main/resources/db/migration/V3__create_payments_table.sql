CREATE TABLE IF NOT EXISTS payments (
    id UUID PRIMARY KEY,
    payment_id VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    subscription_id UUID,
    status VARCHAR(255) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    description VARCHAR(128),
    payment_method_id VARCHAR(255),
    payment_method_type VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_payments_user_id ON payments(user_id);
CREATE INDEX IF NOT EXISTS idx_payments_payment_id ON payments(payment_id);
CREATE INDEX IF NOT EXISTS idx_payments_status ON payments(status);