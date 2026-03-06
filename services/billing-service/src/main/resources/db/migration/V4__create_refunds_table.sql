CREATE TABLE IF NOT EXISTS refunds (
    id UUID PRIMARY KEY,
    refund_id VARCHAR(255) NOT NULL UNIQUE,
    payment_id VARCHAR(255) NOT NULL,
    user_id UUID NOT NULL,
    status VARCHAR(255) NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    description VARCHAR(256),
    refund_method_type VARCHAR(255),
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_refunds_payment_id ON refunds(payment_id);
CREATE INDEX IF NOT EXISTS idx_refunds_user_id ON refunds(user_id);
CREATE INDEX IF NOT EXISTS idx_refunds_refund_id ON refunds(refund_id);