INSERT INTO staff_permissions (code, display_name) VALUES
    ('platform.partner.account_status.update', '파트너 관리자 로그인 상태 변경'),
    ('platform.partner.allow_status.update', '파트너 승인상태 변경'),
    ('platform.partner.status.update', '파트너 운영상태 변경')
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name);

INSERT IGNORE INTO staff_role_permissions (staff_role_id, staff_permission_id)
SELECT r.id, p.id
FROM staff_roles r
CROSS JOIN staff_permissions p
WHERE r.name = 'platform.super_admin';
