ALTER TABLE categories
    ADD COLUMN group_code VARCHAR(30) NULL AFTER depth,
    ADD UNIQUE KEY categories_domain_code_unique (domain, code),
    ADD INDEX categories_domain_depth_sort_id_idx (domain, depth, sort_order, id),
    ADD INDEX categories_domain_group_status_sort_id_idx (domain, group_code, status, sort_order, id),
    ADD INDEX categories_domain_parent_sort_id_idx (domain, parent_id, sort_order, id),
    ADD INDEX categories_domain_parent_status_sort_id_idx (domain, parent_id, status, sort_order, id),
    ADD INDEX categories_domain_visible_sort_id_idx (domain, is_menu_visible, sort_order, id);

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
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='카테고리 사용처별 노출 매핑 테이블';
