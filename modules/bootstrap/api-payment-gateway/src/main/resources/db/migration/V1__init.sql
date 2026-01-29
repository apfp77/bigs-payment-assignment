-- Partner master
CREATE TABLE IF NOT EXISTS partner (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    code VARCHAR(64) NOT NULL UNIQUE,
    name VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE
);

-- Partner fee policy
CREATE TABLE IF NOT EXISTS partner_fee_policy (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    partner_id BIGINT NOT NULL,
    effective_from TIMESTAMP NOT NULL,
    percentage DECIMAL(10,6) NOT NULL,
    fixed_fee DECIMAL(15,0) NULL,
    INDEX idx_fee_partner_from (partner_id, effective_from DESC)
);

-- Payment history
CREATE TABLE IF NOT EXISTS payment (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    partner_id BIGINT NOT NULL,
    amount DECIMAL(15,0) NOT NULL,
    applied_fee_rate DECIMAL(10,6) NOT NULL,
    fee_amount DECIMAL(15,0) NOT NULL,
    net_amount DECIMAL(15,0) NOT NULL,
    card_bin VARCHAR(8) NULL,
    card_last4 VARCHAR(4) NULL,
    approval_code VARCHAR(32) NULL,
    approved_at TIMESTAMP NULL,
    status VARCHAR(20) NOT NULL,
    failure_code VARCHAR(64) NULL,
    failure_message VARCHAR(128) NULL,
    failed_at TIMESTAMP NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    INDEX idx_payment_created (created_at DESC, id DESC),
    INDEX idx_payment_partner_created (partner_id, created_at DESC),
    INDEX idx_payment_status (status)
);
