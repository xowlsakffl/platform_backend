INSERT INTO staff_permissions (code, display_name) VALUES
    ('platform.agency.show', '에이전시 조회'),
    ('platform.agency.create', '에이전시 등록'),
    ('platform.agency.update', '에이전시 수정'),
    ('platform.agency.delete', '에이전시 삭제'),
    ('platform.talk.show', '토크 조회'),
    ('platform.talk.create', '토크 등록'),
    ('platform.talk.update', '토크 수정'),
    ('platform.talk.delete', '토크 삭제')
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name);

DELETE rp
FROM staff_role_permissions rp
JOIN staff_roles r ON r.id = rp.staff_role_id
WHERE r.name IN (
    'platform.super_admin',
    'platform.admin',
    'platform.staff',
    'platform.dev'
);

INSERT INTO staff_role_permissions (staff_role_id, staff_permission_id)
SELECT r.id, p.id
FROM staff_roles r
CROSS JOIN staff_permissions p
WHERE r.name = 'platform.super_admin';

INSERT INTO staff_role_permissions (staff_role_id, staff_permission_id)
SELECT r.id, p.id
FROM staff_roles r
CROSS JOIN staff_permissions p
WHERE r.name = 'platform.admin'
  AND p.code NOT LIKE 'platform.staff.%';

INSERT INTO staff_role_permissions (staff_role_id, staff_permission_id)
SELECT r.id, p.id
FROM staff_roles r
CROSS JOIN staff_permissions p
WHERE r.name IN ('platform.staff', 'platform.dev')
  AND p.code IN (
    'common.access',
    'common.dashboard.show',
    'common.profile.show',
    'common.profile.update',
    'platform.hospital.show',
    'platform.hospital_entry.show',
    'platform.beauty.show',
    'platform.agency.show',
    'platform.user.show',
    'platform.doctor.show',
    'platform.expert.show',
    'platform.hospital_event_db.show',
    'platform.hospital_event_real_model_db.show',
    'platform.hospital_review.show',
    'platform.hospital_evaluation.show',
    'platform.talk.show',
    'platform.reported_talk.show',
    'platform.reported_hospital_review.show',
    'platform.reported_hospital_evaluation.show',
    'platform.reported_chat_message.show',
    'platform.reported_video.show',
    'platform.notice.show',
    'platform.faq.show'
  );
