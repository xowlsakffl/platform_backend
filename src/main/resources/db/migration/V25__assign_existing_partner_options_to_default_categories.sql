CREATE TEMPORARY TABLE partner_option_default_category_seed (
    parent_code VARCHAR(80) NOT NULL,
    name VARCHAR(120) NOT NULL,
    code VARCHAR(80) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (code)
);

INSERT INTO partner_option_default_category_seed (parent_code, name, code, sort_order) VALUES
    ('KB_SEMI_PERMANENT', '기타', 'KB_SEMI_PERMANENT_OTHER', 99),
    ('KB_ESTHETIC', '기타', 'KB_ESTHETIC_OTHER', 99),
    ('KB_HAIR_SALON', '기타', 'KB_HAIR_SALON_OTHER', 99),
    ('KB_WAXING', '기타', 'KB_WAXING_OTHER', 99),
    ('KB_TATTOO', '기타', 'KB_TATTOO_OTHER', 99),
    ('KB_NAIL', '기타', 'KB_NAIL_OTHER', 99),
    ('KB_MASSAGE', '기타', 'KB_MASSAGE_OTHER', 99),
    ('KB_EYELASH', '기타', 'KB_EYELASH_OTHER', 99),
    ('KB_MAKEUP', '기타', 'KB_MAKEUP_OTHER', 99),
    ('KB_SCALP_CARE', '기타', 'KB_SCALP_CARE_OTHER', 99),
    ('KB_OTHER', '기타', 'KB_OTHER_GENERAL', 99);

INSERT INTO categories (
    domain, parent_id, depth, group_code, name, code,
    full_path, sort_order, status, is_menu_visible
)
SELECT
    'PARTNER', parent.id, 2, parent.group_code, seed.name, seed.code,
    CONCAT(parent.name, ' > ', seed.name), seed.sort_order, 'ACTIVE', TRUE
FROM partner_option_default_category_seed seed
JOIN categories parent
  ON parent.domain = 'PARTNER'
 AND parent.code = seed.parent_code
ON DUPLICATE KEY UPDATE
    parent_id = VALUES(parent_id),
    name = VALUES(name),
    full_path = VALUES(full_path),
    sort_order = VALUES(sort_order),
    status = 'ACTIVE';

INSERT INTO category_usages (`usage`, category_id, sort_order, status)
SELECT 'PARTNER_OPTION_CATEGORY', category.id, seed.sort_order, 'ACTIVE'
FROM partner_option_default_category_seed seed
JOIN categories category
  ON category.domain = 'PARTNER'
 AND category.code = seed.code
ON DUPLICATE KEY UPDATE
    sort_order = VALUES(sort_order),
    status = 'ACTIVE';

INSERT INTO category_assignments (
    categorizable_type, categorizable_id, category_id, is_primary, created_at, updated_at
)
SELECT
    'PARTNER_OPTION', partner_option.id, option_category.id, TRUE,
    CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM partner_options partner_option
JOIN category_assignments partner_assignment
  ON partner_assignment.categorizable_type = 'PARTNER'
 AND partner_assignment.categorizable_id = partner_option.partner_id
 AND partner_assignment.is_primary = TRUE
JOIN categories partner_category
  ON partner_category.id = partner_assignment.category_id
JOIN partner_option_default_category_seed seed
  ON seed.parent_code = partner_category.code
JOIN categories option_category
  ON option_category.parent_id = partner_category.id
 AND option_category.code = seed.code
WHERE partner_option.deleted_at IS NULL
  AND NOT EXISTS (
    SELECT 1
    FROM category_assignments option_assignment
    WHERE option_assignment.categorizable_type = 'PARTNER_OPTION'
      AND option_assignment.categorizable_id = partner_option.id
);

DROP TEMPORARY TABLE partner_option_default_category_seed;
