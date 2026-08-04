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

DELETE FROM categories WHERE domain IN ('PARTNER_MEDICAL', 'MEDICAL', 'PARTNER') AND depth = 3;
DELETE FROM categories WHERE domain IN ('PARTNER_MEDICAL', 'MEDICAL', 'PARTNER') AND depth = 2;
DELETE FROM categories WHERE domain IN ('PARTNER_MEDICAL', 'MEDICAL', 'PARTNER') AND depth = 1;

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
