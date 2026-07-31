CREATE TABLE categories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    domain VARCHAR(40) NOT NULL,
    parent_id BIGINT NULL,
    depth TINYINT NOT NULL,
    name VARCHAR(120) NOT NULL,
    code VARCHAR(80) NULL,
    full_path VARCHAR(255) NULL,
    sort_order INT NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    is_menu_visible BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX categories_parent_id_index (parent_id),
    INDEX categories_domain_status_sort_index (domain, status, sort_order),
    CONSTRAINT fk_categories_parent FOREIGN KEY (parent_id) REFERENCES categories (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='카테고리 테이블';

CREATE TABLE partners (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    department VARCHAR(30) NOT NULL DEFAULT 'OTHER',
    description TEXT NULL,
    instagram_link VARCHAR(500) NULL,
    kakao_link VARCHAR(500) NULL,
    road_address VARCHAR(255) NULL,
    jibun_address VARCHAR(255) NULL,
    latitude VARCHAR(50) NULL,
    longitude VARCHAR(50) NULL,
    operating_hours_notice TEXT NULL,
    operation_hours JSON NULL,
    direction TEXT NULL,
    view_count BIGINT UNSIGNED NOT NULL DEFAULT 0,
    evaluation_count INT UNSIGNED NOT NULL DEFAULT 0,
    evaluation_average_rating DECIMAL(2, 1) NOT NULL DEFAULT 0.0,
    allow_status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY partners_name_unique (name),
    INDEX partners_allow_status_index (allow_status),
    INDEX partners_status_index (status),
    INDEX partners_department_index (department),
    INDEX partners_view_count_index (view_count),
    INDEX partners_evaluation_count_index (evaluation_count),
    INDEX partners_evaluation_average_rating_index (evaluation_average_rating),
    INDEX partners_deleted_id_idx (deleted_at, id),
    INDEX partners_deleted_name_id_idx (deleted_at, name, id),
    INDEX partners_deleted_created_id_idx (deleted_at, created_at, id),
    INDEX partners_deleted_updated_id_idx (deleted_at, updated_at, id),
    INDEX partners_deleted_status_id_idx (deleted_at, status, id),
    INDEX partners_deleted_allow_status_id_idx (deleted_at, allow_status, id),
    INDEX partners_deleted_department_id_idx (deleted_at, department, id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='파트너 테이블';

CREATE TABLE partner_contacts (
    id BIGINT NOT NULL AUTO_INCREMENT,
    partner_id BIGINT NOT NULL,
    contact_type VARCHAR(40) NOT NULL,
    value VARCHAR(255) NOT NULL,
    sort_order TINYINT NOT NULL DEFAULT 0,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    verified_at TIMESTAMP(6) NULL,
    memo VARCHAR(500) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    INDEX partner_contacts_type_order_index (partner_id, contact_type, sort_order),
    CONSTRAINT fk_partner_contacts_partner FOREIGN KEY (partner_id) REFERENCES partners (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='파트너 연락처 테이블';

CREATE TABLE partner_business_registrations (
    id BIGINT NOT NULL AUTO_INCREMENT,
    partner_id BIGINT NOT NULL,
    business_number VARCHAR(20) NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    ceo_name VARCHAR(100) NOT NULL,
    business_type VARCHAR(100) NULL,
    business_item VARCHAR(100) NULL,
    business_address VARCHAR(255) NULL,
    business_address_detail VARCHAR(255) NULL,
    settlement_bank_name VARCHAR(50) NULL,
    settlement_account_number VARCHAR(50) NULL,
    settlement_account_holder VARCHAR(100) NULL,
    tax_invoice_email VARCHAR(255) NULL,
    issued_at DATE NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY partner_business_registrations_business_number_unique (business_number),
    INDEX partner_business_registrations_partner_id_index (partner_id),
    INDEX partner_business_registrations_status_index (status),
    CONSTRAINT fk_partner_business_registrations_partner FOREIGN KEY (partner_id) REFERENCES partners (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='파트너 사업자 등록 테이블';

CREATE TABLE partner_features (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(100) NOT NULL,
    name VARCHAR(100) NOT NULL,
    sort_order INT UNSIGNED NOT NULL DEFAULT 0,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY partner_features_code_unique (code),
    INDEX partner_features_status_sort_order_index (status, sort_order)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='파트너 정보 마스터 테이블';

CREATE TABLE partner_feature_assignments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    partner_id BIGINT NOT NULL,
    partner_feature_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY partner_feature_assignments_partner_feature_unique (partner_id, partner_feature_id),
    INDEX partner_feature_assignments_partner_feature_id_index (partner_feature_id),
    CONSTRAINT fk_partner_feature_assignments_partner FOREIGN KEY (partner_id) REFERENCES partners (id) ON DELETE CASCADE,
    CONSTRAINT fk_partner_feature_assignments_feature FOREIGN KEY (partner_feature_id) REFERENCES partner_features (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='파트너 특징 연결 테이블';

CREATE TABLE category_assignments (
    id BIGINT NOT NULL AUTO_INCREMENT,
    categorizable_type VARCHAR(191) NOT NULL,
    categorizable_id BIGINT NOT NULL,
    category_id BIGINT NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY category_assignments_target_category_unique (categorizable_type, categorizable_id, category_id),
    INDEX category_assignments_category_id_index (category_id),
    INDEX category_assignments_target_index (categorizable_type, categorizable_id),
    CONSTRAINT fk_category_assignments_category FOREIGN KEY (category_id) REFERENCES categories (id) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='카테고리 연결 테이블';

CREATE TABLE account_partners (
    id BIGINT NOT NULL AUTO_INCREMENT,
    partner_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    nickname VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NULL,
    email_verified_at TIMESTAMP(6) NULL,
    password VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL DEFAULT 'SUSPENDED',
    last_login_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY account_partners_partner_id_unique (partner_id),
    UNIQUE KEY account_partners_nickname_unique (nickname),
    UNIQUE KEY account_partners_email_unique (email),
    INDEX account_partners_status_index (status),
    INDEX account_partners_login_status_partner_idx (last_login_at, status, partner_id),
    CONSTRAINT fk_account_partners_partner FOREIGN KEY (partner_id) REFERENCES partners (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='파트너 관리자 계정 테이블';

CREATE TABLE operation_histories (
    id BIGINT NOT NULL AUTO_INCREMENT,
    target_type VARCHAR(80) NOT NULL,
    target_id BIGINT NOT NULL,
    actor_type VARCHAR(20) NOT NULL DEFAULT 'STAFF',
    actor_id BIGINT NULL,
    action VARCHAR(60) NOT NULL,
    reason VARCHAR(500) NULL,
    memo VARCHAR(500) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX operation_histories_target_index (target_type, target_id, created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='운영 처리 이력 테이블';

CREATE TABLE operation_history_changes (
    id BIGINT NOT NULL AUTO_INCREMENT,
    operation_history_id BIGINT NOT NULL,
    field_key VARCHAR(80) NOT NULL,
    before_value TEXT NULL,
    after_value TEXT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    INDEX operation_history_changes_history_index (operation_history_id),
    CONSTRAINT fk_operation_history_changes_history FOREIGN KEY (operation_history_id) REFERENCES operation_histories (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='운영 처리 이력 변경 상세 테이블';
