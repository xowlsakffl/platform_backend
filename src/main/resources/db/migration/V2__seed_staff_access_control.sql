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
    ('platform.agency.create', '에이전시 등록'),
    ('platform.agency.delete', '에이전시 삭제'),
    ('platform.agency.show', '에이전시 조회'),
    ('platform.agency.update', '에이전시 수정'),
    ('platform.category.manage', '카테고리 관리'),
    ('platform.expert.create', '전문가 등록'),
    ('platform.expert.delete', '전문가 삭제'),
    ('platform.expert.show', '전문가 조회'),
    ('platform.expert.update', '전문가 수정'),
    ('platform.faq.create', 'FAQ 등록'),
    ('platform.faq.delete', 'FAQ 삭제'),
    ('platform.faq.show', 'FAQ 조회'),
    ('platform.faq.update', 'FAQ 수정'),
    ('platform.hashtag.manage', '해시태그 관리'),
    ('platform.notice.create', '공지 등록'),
    ('platform.notice.delete', '공지 삭제'),
    ('platform.notice.show', '공지 조회'),
    ('platform.notice.update', '공지 수정'),
    ('platform.partner_entry.show', '입점신청 조회'),
    ('platform.partner_entry.update', '입점신청 수정'),
    ('platform.partner_evaluation.show', '파트너 평가 조회'),
    ('platform.partner_evaluation.update', '파트너 평가 수정'),
    ('platform.partner_event_ad.create', '파트너 이벤트 광고 등록'),
    ('platform.partner_event_ad.delete', '파트너 이벤트 광고 삭제'),
    ('platform.partner_event_ad.show', '파트너 이벤트 광고 조회'),
    ('platform.partner_event_ad.update', '파트너 이벤트 광고 수정'),
    ('platform.partner_event_db.show', '이벤트 상담 DB 조회'),
    ('platform.partner_event_db.update', '이벤트 상담 DB 수정'),
    ('platform.partner_event_real_model_db.show', '리얼모델 DB 조회'),
    ('platform.partner_event_real_model_db.update', '리얼모델 DB 수정'),
    ('platform.partner_event.create', '파트너 이벤트 등록'),
    ('platform.partner_event.delete', '파트너 이벤트 삭제'),
    ('platform.partner_event.show', '파트너 이벤트 조회'),
    ('platform.partner_event.update', '파트너 이벤트 수정'),
    ('platform.partner_review.show', '파트너 후기 조회'),
    ('platform.partner_review.update', '파트너 후기 수정'),
    ('platform.partner.account_status.update', '파트너 관리자 로그인 상태 변경'),
    ('platform.partner.allow_status.update', '파트너 승인상태 변경'),
    ('platform.partner.assign_staff', '파트너 담당 직원 지정'),
    ('platform.partner.create', '파트너 등록'),
    ('platform.partner.delete', '파트너 삭제'),
    ('platform.partner.show', '파트너 조회'),
    ('platform.partner.status.update', '파트너 운영상태 변경'),
    ('platform.partner.update', '파트너 수정'),
    ('platform.reported_chat_message.show', '신고 채팅 조회'),
    ('platform.reported_chat_message.update', '신고 채팅 수정'),
    ('platform.reported_partner_evaluation.show', '신고 평가 조회'),
    ('platform.reported_partner_evaluation.update', '신고 평가 수정'),
    ('platform.reported_partner_review.show', '신고 후기 조회'),
    ('platform.reported_partner_review.update', '신고 후기 수정'),
    ('platform.reported_talk.show', '신고 토크 조회'),
    ('platform.reported_talk.update', '신고 토크 수정'),
    ('platform.reported_video.show', '신고 동영상 조회'),
    ('platform.reported_video.update', '신고 동영상 수정'),
    ('platform.specialist.create', '스페셜리스트 등록'),
    ('platform.specialist.delete', '스페셜리스트 삭제'),
    ('platform.specialist.show', '스페셜리스트 조회'),
    ('platform.specialist.update', '스페셜리스트 수정'),
    ('platform.staff.create', '직원 등록'),
    ('platform.staff.delete', '직원 삭제'),
    ('platform.staff.show', '직원 조회'),
    ('platform.staff.update', '직원 수정'),
    ('platform.talk.create', '토크 등록'),
    ('platform.talk.delete', '토크 삭제'),
    ('platform.talk.show', '토크 조회'),
    ('platform.talk.update', '토크 수정'),
    ('platform.user.show', '회원 조회'),
    ('platform.user.status.update', '회원 상태 수정'),
    ('platform.video.create', '동영상 등록'),
    ('platform.video.delete', '동영상 삭제'),
    ('platform.video.show', '동영상 조회'),
    ('platform.video.update', '동영상 수정');

INSERT INTO staff_role_permissions (staff_role_id, staff_permission_id)
SELECT role.id, permission.id
FROM staff_roles role
CROSS JOIN staff_permissions permission
WHERE role.name = 'platform.super_admin';

INSERT INTO staff_role_permissions (staff_role_id, staff_permission_id)
SELECT role.id, permission.id
FROM staff_roles role
CROSS JOIN staff_permissions permission
WHERE role.name = 'platform.admin'
  AND permission.code NOT LIKE 'platform.staff.%'
  AND permission.code NOT IN (
    'platform.partner.account_status.update',
    'platform.partner.allow_status.update',
    'platform.partner.status.update'
  );

INSERT INTO staff_role_permissions (staff_role_id, staff_permission_id)
SELECT role.id, permission.id
FROM staff_roles role
CROSS JOIN staff_permissions permission
WHERE role.name IN ('platform.staff', 'platform.dev')
  AND permission.code IN (
    'common.access',
    'common.dashboard.show',
    'common.profile.show',
    'common.profile.update',
    'platform.agency.show',
    'platform.expert.show',
    'platform.faq.show',
    'platform.notice.show',
    'platform.partner_entry.show',
    'platform.partner_evaluation.show',
    'platform.partner_event_db.show',
    'platform.partner_event_real_model_db.show',
    'platform.partner_review.show',
    'platform.partner.show',
    'platform.reported_chat_message.show',
    'platform.reported_partner_evaluation.show',
    'platform.reported_partner_review.show',
    'platform.reported_talk.show',
    'platform.reported_video.show',
    'platform.specialist.show',
    'platform.talk.show',
    'platform.user.show'
  );

