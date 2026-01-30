-- Payment initiations table
CREATE TABLE payment_initiations (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    lease_id UUID NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    provider VARCHAR(20) NOT NULL,
    bank_account_id VARCHAR(100),
    processor_token VARCHAR(100),
    payment_method_id VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    external_transaction_id VARCHAR(100),
    stripe_payment_intent_id VARCHAR(100),
    plaid_payment_id VARCHAR(100),
    settled_amount DECIMAL(19,2),
    fee_amount DECIMAL(19,2),
    failure_reason VARCHAR(500),
    metadata VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_initiation_payment ON payment_initiations(payment_id);
CREATE INDEX idx_initiation_tenant ON payment_initiations(tenant_id);
CREATE INDEX idx_initiation_lease ON payment_initiations(lease_id);
CREATE INDEX idx_initiation_status ON payment_initiations(status);

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_gateway_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_payment_initiations_updated_at BEFORE UPDATE ON payment_initiations
    FOR EACH ROW EXECUTE FUNCTION update_gateway_updated_at();
