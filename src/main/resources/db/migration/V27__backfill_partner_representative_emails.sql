UPDATE account_partners
SET login_id = REPLACE(login_id, 'medi_partner_', 'platform_partner_')
WHERE login_id LIKE 'medi_partner_%';

INSERT INTO partner_contacts (
    partner_id,
    contact_type,
    value,
    sort_order,
    is_primary,
    is_active,
    created_at,
    updated_at
)
SELECT
    partner.id,
    'REPRESENTATIVE_EMAIL',
    COALESCE(
        account_partner.email,
        (
            SELECT invitation.email
            FROM partner_account_invitations invitation
            WHERE invitation.partner_id = partner.id
            ORDER BY invitation.id DESC
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
    ),
    0,
    TRUE,
    TRUE,
    CURRENT_TIMESTAMP(6),
    CURRENT_TIMESTAMP(6)
FROM partners partner
LEFT JOIN account_partners account_partner
    ON account_partner.partner_id = partner.id
   AND account_partner.deleted_at IS NULL
WHERE partner.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1
      FROM partner_contacts representative_email
      WHERE representative_email.partner_id = partner.id
        AND representative_email.contact_type = 'REPRESENTATIVE_EMAIL'
        AND representative_email.is_active = TRUE
        AND representative_email.deleted_at IS NULL
  )
  AND COALESCE(
      account_partner.email,
      (
          SELECT invitation.email
          FROM partner_account_invitations invitation
          WHERE invitation.partner_id = partner.id
          ORDER BY invitation.id DESC
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
  ) IS NOT NULL;
