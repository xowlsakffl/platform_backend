UPDATE account_partners
SET status = 'BLOCKED', updated_at = CURRENT_TIMESTAMP(6)
WHERE status IN ('SUSPENDED', 'WITHDRAWN');

ALTER TABLE account_partners
    MODIFY status VARCHAR(255) NOT NULL DEFAULT 'ACTIVE';
