DELETE category_assignment
FROM category_assignments category_assignment
JOIN categories category ON category.id = category_assignment.category_id
WHERE category.domain IN ('PARTNER_MEDICAL', 'MEDICAL', 'BEAUTY');

DELETE media
FROM media
JOIN categories category
  ON media.owner_type = 'CATEGORY'
 AND media.owner_id = category.id
WHERE category.domain IN ('PARTNER_MEDICAL', 'MEDICAL', 'BEAUTY');

DELETE FROM categories WHERE domain IN ('PARTNER_MEDICAL', 'MEDICAL', 'BEAUTY') AND depth = 3;
DELETE FROM categories WHERE domain IN ('PARTNER_MEDICAL', 'MEDICAL', 'BEAUTY') AND depth = 2;
DELETE FROM categories WHERE domain IN ('PARTNER_MEDICAL', 'MEDICAL', 'BEAUTY') AND depth = 1;

CREATE TEMPORARY TABLE beauty_category_seed (
    group_code VARCHAR(30) NOT NULL,
    name VARCHAR(120) NOT NULL,
    code VARCHAR(80) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (code)
);

INSERT INTO beauty_category_seed (group_code, name, code, sort_order) VALUES
('TREATMENT', '반영구', 'KB_SEMI_PERMANENT', 1),
('TREATMENT', '에스테틱', 'KB_ESTHETIC', 2),
('TREATMENT', '미용실', 'KB_HAIR_SALON', 3),
('TREATMENT', '왁싱', 'KB_WAXING', 4),
('TREATMENT', '타투', 'KB_TATTOO', 5),
('TREATMENT', '네일아트', 'KB_NAIL', 6),
('TREATMENT', '마사지', 'KB_MASSAGE', 7);

INSERT INTO categories (
    domain, parent_id, depth, group_code, name, code,
    full_path, sort_order, status, is_menu_visible
)
SELECT
    'BEAUTY', NULL, 1, seed.group_code, seed.name, seed.code,
    seed.name, seed.sort_order, 'ACTIVE', TRUE
FROM beauty_category_seed seed;

DROP TEMPORARY TABLE beauty_category_seed;

ALTER TABLE partner_specialists
    MODIFY COLUMN license_number VARCHAR(100) NULL COMMENT '자격 증빙 번호';

UPDATE partner_specialists
SET specialist_field = 'OTHER'
WHERE specialist_field NOT IN (
    'HAIR_DESIGNER',
    'NAIL_ARTIST',
    'ESTHETICIAN',
    'WAXING_SPECIALIST',
    'TATTOO_ARTIST',
    'SEMI_PERMANENT_ARTIST',
    'MASSAGE_THERAPIST',
    'MAKEUP_ARTIST',
    'OTHER'
);
