ALTER TABLE account_staffs
    ADD COLUMN login_id VARCHAR(30) NULL AFTER id;

UPDATE account_staffs
SET login_id = LOWER(nickname)
WHERE login_id IS NULL;

ALTER TABLE account_staffs
    MODIFY COLUMN login_id VARCHAR(30) NOT NULL,
    ADD UNIQUE KEY account_staffs_login_id_unique (login_id);

ALTER TABLE account_partners
    ADD COLUMN login_id VARCHAR(30) NULL AFTER partner_id;

UPDATE account_partners
SET login_id = LOWER(nickname)
WHERE login_id IS NULL;

ALTER TABLE account_partners
    DROP INDEX account_partners_nickname_unique,
    MODIFY COLUMN login_id VARCHAR(30) NOT NULL,
    ADD UNIQUE KEY account_partners_login_id_unique (login_id),
    DROP COLUMN name,
    DROP COLUMN nickname;

ALTER TABLE partners
    DROP COLUMN account_invitation_email;

CREATE TABLE hashtags (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(30) NOT NULL,
    normalized_name VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY hashtags_normalized_name_unique (normalized_name),
    INDEX hashtags_status_name_index (status, name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE hashtag_relations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    hashtag_id BIGINT NOT NULL,
    target_type VARCHAR(30) NOT NULL,
    target_id BIGINT NOT NULL,
    sort_order TINYINT UNSIGNED NOT NULL DEFAULT 0,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY hashtag_relations_target_hashtag_unique (target_type, target_id, hashtag_id),
    UNIQUE KEY hashtag_relations_target_order_unique (target_type, target_id, sort_order),
    INDEX hashtag_relations_hashtag_index (hashtag_id),
    CONSTRAINT fk_hashtag_relations_hashtag
        FOREIGN KEY (hashtag_id) REFERENCES hashtags (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT INTO hashtags (name, normalized_name)
SELECT MIN(value), LOWER(TRIM(value))
FROM partner_hashtags
GROUP BY LOWER(TRIM(value));

INSERT INTO hashtag_relations (hashtag_id, target_type, target_id, sort_order, created_at, updated_at)
SELECT hashtag.id, 'PARTNER', partner_hashtag.partner_id, partner_hashtag.sort_order,
       partner_hashtag.created_at, partner_hashtag.updated_at
FROM partner_hashtags partner_hashtag
JOIN hashtags hashtag ON hashtag.normalized_name = LOWER(TRIM(partner_hashtag.value));

DROP TABLE partner_hashtags;
