ALTER TABLE partners
    ADD COLUMN registration_source VARCHAR(30) NOT NULL DEFAULT 'STAFF_CREATED' AFTER status,
    ADD COLUMN created_by_staff_id BIGINT NULL AFTER registration_source,
    ADD INDEX partners_registration_source_index (registration_source),
    ADD INDEX partners_created_by_staff_index (created_by_staff_id),
    ADD CONSTRAINT fk_partners_created_by_staff
        FOREIGN KEY (created_by_staff_id) REFERENCES account_staffs (id) ON DELETE SET NULL;

UPDATE partners partner
SET partner.registration_source = 'SELF_ONBOARDING'
WHERE EXISTS (
    SELECT 1
    FROM operation_histories history
    WHERE history.target_type = 'PARTNER'
      AND history.target_id = partner.id
      AND history.action = 'ONBOARDING_SIGNED_UP'
);

CREATE TABLE partner_account_invitations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    partner_id BIGINT NOT NULL,
    email VARCHAR(255) NOT NULL,
    recipient_name VARCHAR(255) NULL,
    token_hash VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    expires_at TIMESTAMP(6) NOT NULL,
    sent_at TIMESTAMP(6) NULL,
    accepted_at TIMESTAMP(6) NULL,
    canceled_at TIMESTAMP(6) NULL,
    created_by_staff_id BIGINT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY partner_account_invitations_token_hash_unique (token_hash),
    INDEX partner_account_invitations_partner_created_index (partner_id, created_at, id),
    INDEX partner_account_invitations_email_status_index (email, status, expires_at),
    INDEX partner_account_invitations_staff_index (created_by_staff_id),
    CONSTRAINT fk_partner_account_invitations_partner
        FOREIGN KEY (partner_id) REFERENCES partners (id) ON DELETE CASCADE,
    CONSTRAINT fk_partner_account_invitations_staff
        FOREIGN KEY (created_by_staff_id) REFERENCES account_staffs (id) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
