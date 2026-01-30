-- Accounts table
CREATE TABLE accounts (
    id UUID PRIMARY KEY,
    account_number VARCHAR(50) NOT NULL UNIQUE,
    account_name VARCHAR(100) NOT NULL,
    account_type VARCHAR(20) NOT NULL,
    account_sub_type VARCHAR(50) NOT NULL,
    owner_id UUID NOT NULL,
    current_balance DECIMAL(19,2) NOT NULL DEFAULT 0,
    available_balance DECIMAL(19,2) NOT NULL DEFAULT 0,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    description VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_account_owner ON accounts(owner_id);
CREATE INDEX idx_account_type ON accounts(account_type);

-- Ledger entries table
CREATE TABLE ledger_entries (
    id UUID PRIMARY KEY,
    payment_id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    property_id UUID NOT NULL,
    lease_id UUID NOT NULL,

    -- Debit account
    debit_account_number VARCHAR(50) NOT NULL,
    debit_account_type VARCHAR(50) NOT NULL,
    debit_account_owner_id VARCHAR(50) NOT NULL,

    -- Credit account
    credit_account_number VARCHAR(50) NOT NULL,
    credit_account_type VARCHAR(50) NOT NULL,
    credit_account_owner_id VARCHAR(50) NOT NULL,

    amount DECIMAL(19,2) NOT NULL,
    currency VARCHAR(3) NOT NULL DEFAULT 'USD',
    entry_type VARCHAR(50) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',

    reference VARCHAR(100),
    description VARCHAR(500),

    entry_date TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    posted_date TIMESTAMP,

    transaction_id VARCHAR(100),
    batch_id VARCHAR(100),

    debit_balance DECIMAL(19,2),
    credit_balance DECIMAL(19,2),

    metadata VARCHAR(1000),

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version INTEGER NOT NULL DEFAULT 0
);

CREATE INDEX idx_entry_payment ON ledger_entries(payment_id);
CREATE INDEX idx_entry_tenant ON ledger_entries(tenant_id);
CREATE INDEX idx_entry_lease ON ledger_entries(lease_id);
CREATE INDEX idx_entry_date ON ledger_entries(entry_date);
CREATE INDEX idx_entry_status ON ledger_entries(status);

-- Function to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_ledger_updated_at()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = CURRENT_TIMESTAMP;
    RETURN NEW;
END;
$$ language 'plpgsql';

CREATE TRIGGER update_accounts_updated_at BEFORE UPDATE ON accounts
    FOR EACH ROW EXECUTE FUNCTION update_ledger_updated_at();

CREATE TRIGGER update_ledger_entries_updated_at BEFORE UPDATE ON ledger_entries
    FOR EACH ROW EXECUTE FUNCTION update_ledger_updated_at();
