CREATE TABLE payments (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL,
    lease_id UUID NOT NULL,
    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    payment_type VARCHAR(20) NOT NULL,
    payment_method VARCHAR(20) NOT NULL,
    bank_account_id VARCHAR(100),
    external_payment_id VARCHAR(100),
    stripe_payment_intent_id VARCHAR(100),
    plaid_processor_token VARCHAR(100),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    settled_amount DECIMAL(19,2),
    fee_amount DECIMAL(19,2),
    transaction_id VARCHAR(100),
    failure_reason VARCHAR(500),
    retry_count INTEGER NOT NULL DEFAULT 0,
    max_retries INTEGER NOT NULL DEFAULT 3,
    retry_after TIMESTAMP,
    scheduled_for TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    completed_at TIMESTAMP,
    idempotency_key UUID,
    description VARCHAR(500),
    metadata VARCHAR(1000),
    partial_payment BOOLEAN DEFAULT FALSE,
    parent_payment_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0,

    CONSTRAINT fk_parent_payment FOREIGN KEY (parent_payment_id) REFERENCES payments(id)
);

CREATE INDEX idx_payments_tenant ON payments(tenant_id);
CREATE INDEX idx_payments_lease ON payments(lease_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_scheduled ON payments(scheduled_for);
CREATE INDEX idx_payments_idempotency ON payments(idempotency_key);

CREATE TABLE payment_audit_log (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    old_status VARCHAR(20),
    new_status VARCHAR(20) NOT NULL,
    changed_by VARCHAR(100),
    change_reason VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_audit_payment FOREIGN KEY (payment_id) REFERENCES payments(id) ON DELETE CASCADE
);

CREATE INDEX idx_audit_payment ON payment_audit_log(payment_id);
CREATE INDEX idx_audit_created ON payment_audit_log(created_at);

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_payments_updated_at BEFORE UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- Function to audit payment status changes
CREATE OR REPLACE FUNCTION audit_payment_status_change()
RETURNS TRIGGER AS $$
BEGIN
    IF OLD.status IS DISTINCT FROM NEW.status THEN
        INSERT INTO payment_audit_log (id, payment_id, old_status, new_status, created_at)
        VALUES (gen_random_uuid(), NEW.id, OLD.status, NEW.status, CURRENT_TIMESTAMP);
    END IF;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER audit_payment_status AFTER UPDATE ON payments
    FOR EACH ROW EXECUTE FUNCTION audit_payment_status_change();
