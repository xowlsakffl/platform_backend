ALTER TABLE partners
    ADD COLUMN industry VARCHAR(40) NULL AFTER description,
    ADD COLUMN detail_address VARCHAR(255) NULL AFTER jibun_address;

UPDATE partners partner
LEFT JOIN (
    SELECT assignment.categorizable_id AS partner_id, MIN(category.code) AS category_code
    FROM category_assignments assignment
    JOIN categories category ON category.id = assignment.category_id
    WHERE assignment.categorizable_type = 'PARTNER'
      AND category.domain = 'PARTNER'
    GROUP BY assignment.categorizable_id
) assigned_category ON assigned_category.partner_id = partner.id
SET partner.industry = CASE assigned_category.category_code
    WHEN 'KB_SEMI_PERMANENT' THEN 'SEMI_PERMANENT'
    WHEN 'KB_ESTHETIC' THEN 'ESTHETIC'
    WHEN 'KB_HAIR_SALON' THEN 'HAIR_SALON'
    WHEN 'KB_WAXING' THEN 'WAXING'
    WHEN 'KB_TATTOO' THEN 'TATTOO'
    WHEN 'KB_NAIL' THEN 'NAIL_SHOP'
    WHEN 'KB_MASSAGE' THEN 'MASSAGE'
    ELSE 'OTHER'
END
WHERE partner.industry IS NULL;

DELETE FROM category_assignments WHERE categorizable_type = 'PARTNER';

ALTER TABLE partner_business_registrations
    MODIFY COLUMN business_number VARCHAR(20) NULL,
    MODIFY COLUMN company_name VARCHAR(255) NULL,
    MODIFY COLUMN ceo_name VARCHAR(100) NULL;

CREATE TABLE partner_hashtags (
    id BIGINT NOT NULL AUTO_INCREMENT,
    partner_id BIGINT NOT NULL,
    value VARCHAR(30) NOT NULL,
    sort_order TINYINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY partner_hashtags_partner_value_unique (partner_id, value),
    INDEX partner_hashtags_value_index (value),
    CONSTRAINT fk_partner_hashtags_partner
        FOREIGN KEY (partner_id) REFERENCES partners (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE partner_links (
    id BIGINT NOT NULL AUTO_INCREMENT,
    partner_id BIGINT NOT NULL,
    link_type VARCHAR(40) NOT NULL,
    url VARCHAR(1000) NOT NULL,
    sort_order TINYINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY partner_links_partner_type_unique (partner_id, link_type),
    CONSTRAINT fk_partner_links_partner
        FOREIGN KEY (partner_id) REFERENCES partners (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO partner_links (partner_id, link_type, url, sort_order)
SELECT id, 'INSTAGRAM', instagram_link, 0
FROM partners
WHERE instagram_link IS NOT NULL AND TRIM(instagram_link) <> '';

INSERT INTO partner_links (partner_id, link_type, url, sort_order)
SELECT id, 'KAKAO', kakao_link, 1
FROM partners
WHERE kakao_link IS NOT NULL AND TRIM(kakao_link) <> '';

CREATE TABLE partner_options (
    id BIGINT NOT NULL AUTO_INCREMENT,
    partner_id BIGINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    description VARCHAR(1000) NULL,
    price DECIMAL(12, 2) NULL,
    price_type VARCHAR(20) NOT NULL,
    duration_minutes INT NULL,
    is_visible BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    INDEX partner_options_partner_visible_order_index (partner_id, deleted_at, is_visible, sort_order),
    CONSTRAINT fk_partner_options_partner
        FOREIGN KEY (partner_id) REFERENCES partners (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE specialist_options (
    id BIGINT NOT NULL AUTO_INCREMENT,
    specialist_id BIGINT NOT NULL,
    partner_option_id BIGINT NOT NULL,
    price_override DECIMAL(12, 2) NULL,
    price_type_override VARCHAR(20) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY specialist_options_specialist_option_unique (specialist_id, partner_option_id),
    INDEX specialist_options_partner_option_index (partner_option_id),
    CONSTRAINT fk_specialist_options_specialist
        FOREIGN KEY (specialist_id) REFERENCES partner_specialists (id) ON DELETE CASCADE,
    CONSTRAINT fk_specialist_options_partner_option
        FOREIGN KEY (partner_option_id) REFERENCES partner_options (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
