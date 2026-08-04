CREATE TABLE category_usages (
    id BIGINT NOT NULL AUTO_INCREMENT,
    `usage` VARCHAR(60) NOT NULL,
    category_id BIGINT NOT NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY category_usages_usage_category_unique (`usage`, category_id),
    INDEX category_usages_usage_status_sort_id_idx (`usage`, status, sort_order, id),
    INDEX category_usages_usage_status_category_idx (`usage`, status, category_id),
    INDEX category_usages_category_id_index (category_id),
    CONSTRAINT fk_category_usages_category
        FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='카테고리 사용처별 노출 매핑';

INSERT INTO categories (
    domain, parent_id, depth, group_code, name, code,
    full_path, sort_order, status, is_menu_visible
) VALUES
    ('PARTNER', NULL, 1, 'TREATMENT', '속눈썹', 'KB_EYELASH', '속눈썹', 8, 'ACTIVE', TRUE),
    ('PARTNER', NULL, 1, 'TREATMENT', '메이크업', 'KB_MAKEUP', '메이크업', 9, 'ACTIVE', TRUE),
    ('PARTNER', NULL, 1, 'TREATMENT', '두피케어', 'KB_SCALP_CARE', '두피케어', 10, 'ACTIVE', TRUE),
    ('PARTNER', NULL, 1, 'TREATMENT', '기타', 'KB_OTHER', '기타', 99, 'ACTIVE', TRUE)
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    full_path = VALUES(full_path),
    sort_order = VALUES(sort_order),
    status = 'ACTIVE';

CREATE TEMPORARY TABLE partner_option_category_seed (
    parent_code VARCHAR(80) NOT NULL,
    name VARCHAR(120) NOT NULL,
    code VARCHAR(80) NOT NULL,
    sort_order INT NOT NULL,
    PRIMARY KEY (code)
);

INSERT INTO partner_option_category_seed (parent_code, name, code, sort_order) VALUES
    ('KB_SEMI_PERMANENT', '눈썹', 'KB_SEMI_PERMANENT_BROW', 1),
    ('KB_SEMI_PERMANENT', '아이라인', 'KB_SEMI_PERMANENT_EYELINE', 2),
    ('KB_SEMI_PERMANENT', '입술', 'KB_SEMI_PERMANENT_LIP', 3),
    ('KB_SEMI_PERMANENT', '헤어라인', 'KB_SEMI_PERMANENT_HAIRLINE', 4),
    ('KB_SEMI_PERMANENT', '두피', 'KB_SEMI_PERMANENT_SCALP', 5),
    ('KB_ESTHETIC', '피부관리', 'KB_ESTHETIC_SKIN', 1),
    ('KB_ESTHETIC', '여드름관리', 'KB_ESTHETIC_ACNE', 2),
    ('KB_ESTHETIC', '리프팅관리', 'KB_ESTHETIC_LIFTING', 3),
    ('KB_ESTHETIC', '바디관리', 'KB_ESTHETIC_BODY', 4),
    ('KB_ESTHETIC', '웨딩관리', 'KB_ESTHETIC_WEDDING', 5),
    ('KB_HAIR_SALON', '커트', 'KB_HAIR_CUT', 1),
    ('KB_HAIR_SALON', '펌', 'KB_HAIR_PERM', 2),
    ('KB_HAIR_SALON', '염색', 'KB_HAIR_COLOR', 3),
    ('KB_HAIR_SALON', '클리닉', 'KB_HAIR_CLINIC', 4),
    ('KB_HAIR_SALON', '스타일링', 'KB_HAIR_STYLING', 5),
    ('KB_WAXING', '페이스왁싱', 'KB_WAXING_FACE', 1),
    ('KB_WAXING', '바디왁싱', 'KB_WAXING_BODY', 2),
    ('KB_WAXING', '브라질리언왁싱', 'KB_WAXING_BRAZILIAN', 3),
    ('KB_WAXING', '남성왁싱', 'KB_WAXING_MEN', 4),
    ('KB_WAXING', '슈가링', 'KB_WAXING_SUGARING', 5),
    ('KB_TATTOO', '미니타투', 'KB_TATTOO_MINI', 1),
    ('KB_TATTOO', '레터링', 'KB_TATTOO_LETTERING', 2),
    ('KB_TATTOO', '일러스트', 'KB_TATTOO_ILLUSTRATION', 3),
    ('KB_TATTOO', '커버업', 'KB_TATTOO_COVER_UP', 4),
    ('KB_NAIL', '젤네일', 'KB_NAIL_GEL', 1),
    ('KB_NAIL', '네일아트', 'KB_NAIL_ART', 2),
    ('KB_NAIL', '연장', 'KB_NAIL_EXTENSION', 3),
    ('KB_NAIL', '페디큐어', 'KB_NAIL_PEDICURE', 4),
    ('KB_NAIL', '네일케어', 'KB_NAIL_CARE', 5),
    ('KB_MASSAGE', '아로마마사지', 'KB_MASSAGE_AROMA', 1),
    ('KB_MASSAGE', '스포츠마사지', 'KB_MASSAGE_SPORTS', 2),
    ('KB_MASSAGE', '림프마사지', 'KB_MASSAGE_LYMPH', 3),
    ('KB_MASSAGE', '산전·산후마사지', 'KB_MASSAGE_MATERNITY', 4),
    ('KB_EYELASH', '속눈썹연장', 'KB_EYELASH_EXTENSION', 1),
    ('KB_EYELASH', '속눈썹펌·리프트', 'KB_EYELASH_PERM', 2),
    ('KB_EYELASH', '속눈썹틴팅', 'KB_EYELASH_TINT', 3),
    ('KB_EYELASH', '속눈썹케어', 'KB_EYELASH_CARE', 4),
    ('KB_MAKEUP', '데일리메이크업', 'KB_MAKEUP_DAILY', 1),
    ('KB_MAKEUP', '웨딩메이크업', 'KB_MAKEUP_WEDDING', 2),
    ('KB_MAKEUP', '프로필메이크업', 'KB_MAKEUP_PROFILE', 3),
    ('KB_MAKEUP', '무대메이크업', 'KB_MAKEUP_STAGE', 4),
    ('KB_SCALP_CARE', '두피스케일링', 'KB_SCALP_SCALING', 1),
    ('KB_SCALP_CARE', '헤드스파', 'KB_SCALP_HEAD_SPA', 2),
    ('KB_SCALP_CARE', '탈모관리', 'KB_SCALP_HAIR_LOSS', 3),
    ('KB_OTHER', '기타', 'KB_OTHER_GENERAL', 1);

INSERT INTO categories (
    domain, parent_id, depth, group_code, name, code,
    full_path, sort_order, status, is_menu_visible
)
SELECT
    'PARTNER', parent.id, 2, parent.group_code, seed.name, seed.code,
    CONCAT(parent.name, ' > ', seed.name), seed.sort_order, 'ACTIVE', TRUE
FROM partner_option_category_seed seed
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
SELECT 'PARTNER_CATEGORY', category.id, category.sort_order, 'ACTIVE'
FROM categories category
WHERE category.domain = 'PARTNER'
  AND category.depth = 1
  AND category.status = 'ACTIVE'
ON DUPLICATE KEY UPDATE
    sort_order = VALUES(sort_order),
    status = 'ACTIVE';

INSERT INTO category_usages (`usage`, category_id, sort_order, status)
SELECT 'PARTNER_OPTION_CATEGORY', category.id, seed.sort_order, 'ACTIVE'
FROM partner_option_category_seed seed
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
    'PARTNER', partner.id, category.id, TRUE, CURRENT_TIMESTAMP(6), CURRENT_TIMESTAMP(6)
FROM partners partner
JOIN categories category
  ON category.domain = 'PARTNER'
 AND category.code = CASE partner.industry
    WHEN 'SEMI_PERMANENT' THEN 'KB_SEMI_PERMANENT'
    WHEN 'ESTHETIC' THEN 'KB_ESTHETIC'
    WHEN 'HAIR_SALON' THEN 'KB_HAIR_SALON'
    WHEN 'WAXING' THEN 'KB_WAXING'
    WHEN 'TATTOO' THEN 'KB_TATTOO'
    WHEN 'NAIL_SHOP' THEN 'KB_NAIL'
    WHEN 'MASSAGE' THEN 'KB_MASSAGE'
    ELSE 'KB_OTHER'
END
WHERE NOT EXISTS (
    SELECT 1
    FROM category_assignments assignment
    WHERE assignment.categorizable_type = 'PARTNER'
      AND assignment.categorizable_id = partner.id
);

DROP TEMPORARY TABLE partner_option_category_seed;

ALTER TABLE partners DROP COLUMN industry;
