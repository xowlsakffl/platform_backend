INSERT INTO partner_features (code, name, sort_order, status) VALUES
    ('PARKING', '주차 가능', 1, 'ACTIVE'),
    ('VALET_PARKING', '발렛 가능', 2, 'ACTIVE'),
    ('STATION_WITHIN_5_MINUTES', '역에서 도보 5분 이내', 3, 'ACTIVE'),
    ('RESERVATION_ONLY', '예약제', 4, 'ACTIVE'),
    ('PRIVATE_ROOM', '프라이빗룸', 5, 'ACTIVE'),
    ('WIFI', '와이파이', 6, 'ACTIVE'),
    ('LOCKER', '개인 락커', 7, 'ACTIVE'),
    ('SHOWER_ROOM', '샤워실', 8, 'ACTIVE'),
    ('NIGHT_OPERATION', '야간 운영', 9, 'ACTIVE'),
    ('WEEKEND_OPERATION', '주말 운영', 10, 'ACTIVE'),
    ('AFTERCARE', '시술 후 관리', 11, 'ACTIVE'),
    ('WOMEN_SPECIALIST', '여성 스페셜리스트', 12, 'ACTIVE')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    sort_order = VALUES(sort_order),
    status = VALUES(status),
    updated_at = CURRENT_TIMESTAMP(6);

UPDATE partner_features
SET status = 'INACTIVE', updated_at = CURRENT_TIMESTAMP(6)
WHERE code NOT IN (
    'STATION_WITHIN_5_MINUTES',
    'PARKING',
    'VALET_PARKING',
    'RESERVATION_ONLY',
    'PRIVATE_ROOM',
    'WIFI',
    'LOCKER',
    'SHOWER_ROOM',
    'NIGHT_OPERATION',
    'WEEKEND_OPERATION',
    'AFTERCARE',
    'WOMEN_SPECIALIST'
);
