UPDATE category_assignments
SET is_primary = FALSE, updated_at = CURRENT_TIMESTAMP(6)
WHERE categorizable_type = 'PARTNER'
  AND is_primary = TRUE;

CREATE INDEX partners_deleted_allow_created_id_idx
    ON partners (deleted_at, allow_status, created_at, id);

CREATE INDEX partners_deleted_status_created_id_idx
    ON partners (deleted_at, status, created_at, id);

CREATE INDEX account_partners_status_partner_idx
    ON account_partners (status, partner_id);
