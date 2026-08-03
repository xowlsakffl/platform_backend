CREATE TABLE account_staffs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    nickname VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NULL,
    email_verified_at TIMESTAMP(6) NULL,
    password VARCHAR(255) NOT NULL,
    department VARCHAR(100) NULL,
    job_title VARCHAR(100) NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    last_login_at TIMESTAMP(6) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY account_staffs_nickname_unique (nickname),
    UNIQUE KEY account_staffs_email_unique (email),
    INDEX account_staffs_status_index (status),
    INDEX account_staffs_login_status_idx (last_login_at, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='내부 운영자 계정 테이블';

CREATE TABLE staff_roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY staff_roles_name_unique (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='내부 운영자 역할 테이블';

CREATE TABLE staff_permissions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    code VARCHAR(120) NOT NULL,
    display_name VARCHAR(120) NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY staff_permissions_code_unique (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='내부 운영자 권한 테이블';

CREATE TABLE staff_role_permissions (
    id BIGINT NOT NULL AUTO_INCREMENT,
    staff_role_id BIGINT NOT NULL,
    staff_permission_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY staff_role_permissions_role_permission_unique (staff_role_id, staff_permission_id),
    INDEX staff_role_permissions_permission_id_index (staff_permission_id),
    CONSTRAINT fk_staff_role_permissions_role FOREIGN KEY (staff_role_id) REFERENCES staff_roles (id) ON DELETE CASCADE,
    CONSTRAINT fk_staff_role_permissions_permission FOREIGN KEY (staff_permission_id) REFERENCES staff_permissions (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='내부 운영자 역할 권한 연결 테이블';

CREATE TABLE account_staff_roles (
    id BIGINT NOT NULL AUTO_INCREMENT,
    account_staff_id BIGINT NOT NULL,
    staff_role_id BIGINT NOT NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    PRIMARY KEY (id),
    UNIQUE KEY account_staff_roles_staff_role_unique (account_staff_id, staff_role_id),
    INDEX account_staff_roles_role_id_index (staff_role_id),
    CONSTRAINT fk_account_staff_roles_staff FOREIGN KEY (account_staff_id) REFERENCES account_staffs (id) ON DELETE CASCADE,
    CONSTRAINT fk_account_staff_roles_role FOREIGN KEY (staff_role_id) REFERENCES staff_roles (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='내부 운영자 계정 역할 연결 테이블';

CREATE TABLE account_users (
    id BIGINT NOT NULL AUTO_INCREMENT,
    name VARCHAR(255) NOT NULL,
    nickname VARCHAR(255) NOT NULL,
    email VARCHAR(255) NOT NULL,
    phone VARCHAR(50) NULL,
    signup_channel VARCHAR(30) NOT NULL DEFAULT 'EMAIL',
    email_verified_at TIMESTAMP(6) NULL,
    password VARCHAR(255) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
    warning_count INT NOT NULL DEFAULT 0,
    blocked_at TIMESTAMP(6) NULL,
    withdrawal_reason VARCHAR(500) NULL,
    comment_notification_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    note_notification_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    marketing_sms_agreed BOOLEAN NOT NULL DEFAULT FALSE,
    marketing_email_agreed BOOLEAN NOT NULL DEFAULT FALSE,
    marketing_push_agreed BOOLEAN NOT NULL DEFAULT FALSE,
    marketing_night_push_agreed BOOLEAN NOT NULL DEFAULT FALSE,
    last_login_at TIMESTAMP(6) NULL,
    last_accessed_at TIMESTAMP(6) NULL,
    last_access_ip VARCHAR(45) NULL,
    created_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at TIMESTAMP(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    deleted_at TIMESTAMP(6) NULL,
    PRIMARY KEY (id),
    UNIQUE KEY account_users_nickname_unique (nickname),
    UNIQUE KEY account_users_email_unique (email),
    INDEX account_users_status_index (status),
    INDEX account_users_login_status_idx (last_login_at, status),
    INDEX account_users_last_accessed_idx (last_accessed_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='일반 사용자 계정 테이블';

INSERT INTO staff_roles (name, display_name) VALUES
    ('platform.super_admin', '최고 관리자'),
    ('platform.admin', '관리자'),
    ('platform.staff', '운영자'),
    ('platform.dev', '개발자');

INSERT INTO staff_permissions (code, display_name) VALUES
    ('common.access', '관리자 접근'),
    ('common.dashboard.show', '대시보드 조회'),
    ('common.profile.show', '내 프로필 조회'),
    ('common.profile.update', '내 프로필 수정'),
    ('platform.partner.show', '파트너 조회'),
    ('platform.partner.create', '파트너 등록'),
    ('platform.partner.update', '파트너 수정'),
    ('platform.partner.delete', '파트너 삭제'),
    ('platform.partner_entry.show', '입점신청 조회'),
    ('platform.partner_entry.update', '입점신청 수정'),
    ('platform.user.show', '회원 조회'),
    ('platform.user.status.update', '회원 상태 수정'),
    ('platform.staff.show', '직원 조회'),
    ('platform.staff.create', '직원 등록'),
    ('platform.staff.update', '직원 수정'),
    ('platform.staff.delete', '직원 삭제'),
    ('platform.specialist.show', '스페셜리스트 조회'),
    ('platform.specialist.create', '스페셜리스트 등록'),
    ('platform.specialist.update', '스페셜리스트 수정'),
    ('platform.specialist.delete', '스페셜리스트 삭제'),
    ('platform.expert.show', '전문가 조회'),
    ('platform.expert.create', '전문가 등록'),
    ('platform.expert.update', '전문가 수정'),
    ('platform.expert.delete', '전문가 삭제'),
    ('platform.video.show', '동영상 조회'),
    ('platform.video.create', '동영상 등록'),
    ('platform.video.update', '동영상 수정'),
    ('platform.video.delete', '동영상 삭제'),
    ('platform.partner_event.show', '파트너 이벤트 조회'),
    ('platform.partner_event.create', '파트너 이벤트 등록'),
    ('platform.partner_event.update', '파트너 이벤트 수정'),
    ('platform.partner_event.delete', '파트너 이벤트 삭제'),
    ('platform.partner_event_ad.show', '파트너 이벤트 광고 조회'),
    ('platform.partner_event_ad.create', '파트너 이벤트 광고 등록'),
    ('platform.partner_event_ad.update', '파트너 이벤트 광고 수정'),
    ('platform.partner_event_ad.delete', '파트너 이벤트 광고 삭제'),
    ('platform.partner_event_db.show', '이벤트 상담 DB 조회'),
    ('platform.partner_event_db.update', '이벤트 상담 DB 수정'),
    ('platform.partner_event_real_model_db.show', '리얼모델 DB 조회'),
    ('platform.partner_event_real_model_db.update', '리얼모델 DB 수정'),
    ('platform.partner_review.show', '파트너 후기 조회'),
    ('platform.partner_review.update', '파트너 후기 수정'),
    ('platform.partner_evaluation.show', '파트너 평가 조회'),
    ('platform.partner_evaluation.update', '파트너 평가 수정'),
    ('platform.reported_talk.show', '신고 토크 조회'),
    ('platform.reported_talk.update', '신고 토크 수정'),
    ('platform.reported_partner_review.show', '신고 후기 조회'),
    ('platform.reported_partner_review.update', '신고 후기 수정'),
    ('platform.reported_partner_evaluation.show', '신고 평가 조회'),
    ('platform.reported_partner_evaluation.update', '신고 평가 수정'),
    ('platform.reported_chat_message.show', '신고 채팅 조회'),
    ('platform.reported_chat_message.update', '신고 채팅 수정'),
    ('platform.reported_video.show', '신고 동영상 조회'),
    ('platform.reported_video.update', '신고 동영상 수정'),
    ('platform.category.manage', '카테고리 관리'),
    ('platform.hashtag.manage', '해시태그 관리'),
    ('platform.notice.show', '공지 조회'),
    ('platform.notice.create', '공지 등록'),
    ('platform.notice.update', '공지 수정'),
    ('platform.notice.delete', '공지 삭제'),
    ('platform.faq.show', 'FAQ 조회'),
    ('platform.faq.create', 'FAQ 등록'),
    ('platform.faq.update', 'FAQ 수정'),
    ('platform.faq.delete', 'FAQ 삭제');

INSERT INTO staff_role_permissions (staff_role_id, staff_permission_id)
SELECT r.id, p.id
FROM staff_roles r
CROSS JOIN staff_permissions p
WHERE r.name IN ('platform.super_admin', 'platform.dev');

INSERT INTO staff_role_permissions (staff_role_id, staff_permission_id)
SELECT r.id, p.id
FROM staff_roles r
CROSS JOIN staff_permissions p
WHERE r.name = 'platform.admin'
  AND p.code NOT IN ('platform.staff.delete');

INSERT INTO staff_role_permissions (staff_role_id, staff_permission_id)
SELECT r.id, p.id
FROM staff_roles r
CROSS JOIN staff_permissions p
WHERE r.name = 'platform.staff'
  AND p.code IN (
    'common.access',
    'common.dashboard.show',
    'common.profile.show',
    'common.profile.update',
    'platform.partner.show',
    'platform.partner.update',
    'platform.partner_entry.show',
    'platform.partner_entry.update',
    'platform.user.show',
    'platform.specialist.show',
    'platform.video.show',
    'platform.partner_event.show',
    'platform.partner_event_db.show',
    'platform.partner_event_real_model_db.show',
    'platform.partner_review.show',
    'platform.partner_evaluation.show',
    'platform.reported_talk.show',
    'platform.reported_partner_review.show',
    'platform.reported_partner_evaluation.show',
    'platform.reported_chat_message.show',
    'platform.reported_video.show',
    'platform.notice.show',
    'platform.faq.show'
  );
