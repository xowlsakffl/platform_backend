-- 기존 파트너 카테고리를 MVP용 K-파트너 카테고리로 교체한다.

DELETE category_assignment
FROM category_assignments category_assignment
JOIN categories category ON category.id = category_assignment.category_id
WHERE category.domain IN ('PARTNER_MEDICAL', 'MEDICAL', 'PARTNER');

DELETE media
FROM media
JOIN categories category
  ON media.owner_type = 'CATEGORY'
 AND media.owner_id = category.id
WHERE category.domain IN ('PARTNER_MEDICAL', 'MEDICAL', 'PARTNER');

DROP TABLE IF EXISTS category_usages;

DELETE FROM categories WHERE domain IN ('PARTNER_MEDICAL', 'MEDICAL', 'PARTNER') AND depth = 3;
DELETE FROM categories WHERE domain IN ('PARTNER_MEDICAL', 'MEDICAL', 'PARTNER') AND depth = 2;
DELETE FROM categories WHERE domain IN ('PARTNER_MEDICAL', 'MEDICAL', 'PARTNER') AND depth = 1;

CREATE TEMPORARY TABLE partner_category_seed (
    group_code VARCHAR(30) NOT NULL,
    name VARCHAR(120) NOT NULL,
    code VARCHAR(80) NOT NULL,
    parent_code VARCHAR(80) NULL,
    depth TINYINT NOT NULL,
    full_path VARCHAR(255) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (code)
);

INSERT INTO partner_category_seed (group_code, name, code, parent_code, depth, full_path, sort_order) VALUES
('TREATMENT', '반영구', 'KB_SEMI_PERMANENT', NULL, 1, '반영구', 1),
('TREATMENT', '에스테틱', 'KB_ESTHETIC', NULL, 1, '에스테틱', 2),
('TREATMENT', '미용실', 'KB_HAIR_SALON', NULL, 1, '미용실', 3),
('TREATMENT', '왁싱', 'KB_WAXING', NULL, 1, '왁싱', 4),
('TREATMENT', '타투', 'KB_TATTOO', NULL, 1, '타투', 5),
('TREATMENT', '네일아트', 'KB_NAIL', NULL, 1, '네일아트', 6),
('TREATMENT', '마사지', 'KB_MASSAGE', NULL, 1, '마사지', 7);

INSERT INTO categories (
    domain, parent_id, depth, group_code, name, code,
    full_path, sort_order, status, is_menu_visible
)
SELECT
    'PARTNER', NULL, seed.depth, seed.group_code, seed.name, seed.code,
    seed.full_path, seed.sort_order, 'ACTIVE', TRUE
FROM partner_category_seed seed
WHERE seed.depth = 1;

DROP TEMPORARY TABLE partner_category_seed;
