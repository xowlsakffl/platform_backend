ALTER TABLE partners
    ADD COLUMN account_invitation_email VARCHAR(255) NULL AFTER name,
    ADD COLUMN region_sort_key VARCHAR(255) NULL AFTER detail_address,
    ADD INDEX partners_region_sort_key_index (region_sort_key);

UPDATE partners partner
SET partner.account_invitation_email = COALESCE(
    (
        SELECT account.email
        FROM account_partners account
        WHERE account.partner_id = partner.id
          AND account.deleted_at IS NULL
        ORDER BY account.id ASC
        LIMIT 1
    ),
    (
        SELECT contact.value
        FROM partner_contacts contact
        WHERE contact.partner_id = partner.id
          AND contact.contact_type = 'NOTICE_MARKETING_EMAIL'
          AND contact.is_active = TRUE
          AND contact.deleted_at IS NULL
        ORDER BY contact.is_primary DESC, contact.sort_order ASC, contact.id ASC
        LIMIT 1
    )
);

UPDATE partners
SET region_sort_key = CASE
    WHEN TRIM(COALESCE(NULLIF(road_address, ''), NULLIF(jibun_address, ''))) = '' THEN NULL
    WHEN SUBSTRING_INDEX(
        SUBSTRING_INDEX(TRIM(COALESCE(NULLIF(road_address, ''), NULLIF(jibun_address, ''))), ' ', 2),
        ' ',
        -1
    ) LIKE '%시'
    AND SUBSTRING_INDEX(
        SUBSTRING_INDEX(TRIM(COALESCE(NULLIF(road_address, ''), NULLIF(jibun_address, ''))), ' ', 3),
        ' ',
        -1
    ) LIKE '%구'
    THEN SUBSTRING_INDEX(TRIM(COALESCE(NULLIF(road_address, ''), NULLIF(jibun_address, ''))), ' ', 3)
    ELSE SUBSTRING_INDEX(TRIM(COALESCE(NULLIF(road_address, ''), NULLIF(jibun_address, ''))), ' ', 2)
END;
