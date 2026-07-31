INSERT INTO staff_permissions (code, display_name)
VALUES ('platform.partner.assign_staff', '파트너 담당 직원 지정')
ON DUPLICATE KEY UPDATE display_name = VALUES(display_name);

INSERT IGNORE INTO staff_role_permissions (staff_role_id, staff_permission_id)
SELECT r.id, p.id
FROM staff_roles r
JOIN staff_permissions p
  ON p.code = 'platform.partner.assign_staff'
WHERE r.name IN ('platform.super_admin', 'platform.admin');
