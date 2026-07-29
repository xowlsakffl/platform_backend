CREATE TABLE hospital_interpretation_languages (
    hospital_id BIGINT NOT NULL,
    language VARCHAR(30) NOT NULL,
    PRIMARY KEY (hospital_id, language),
    CONSTRAINT fk_hospital_interpretation_languages_hospital
        FOREIGN KEY (hospital_id) REFERENCES hospitals (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='병원별 통역 가능 언어 연결 테이블';

INSERT INTO hospital_features (code, name, sort_order, status) VALUES
    ('ANESTHESIOLOGIST', '마취과 전문의', 1, 'ACTIVE'),
    ('INPATIENT_ROOM', '입원 시설', 2, 'ACTIVE'),
    ('REAL_NAME_SYSTEM', '수술실명제', 3, 'ACTIVE'),
    ('NIGHT_COUNSELING', '야간상담/진료', 4, 'ACTIVE'),
    ('EMERGENCY_SYSTEM', '응급 대응 체계', 5, 'ACTIVE'),
    ('MULTIDISCIPLINARY_CARE', '분야별 공동 진료', 6, 'ACTIVE'),
    ('DEDICATED_REST_AREA', '전용 휴식 공간', 7, 'ACTIVE'),
    ('AFTERCARE', '시술 후 관리', 8, 'ACTIVE'),
    ('FEMALE_DOCTOR_CARE', '여성 의사 진료', 9, 'ACTIVE'),
    ('STATION_WITHIN_5_MINUTES', '역에서 도보 5분 이내', 10, 'ACTIVE'),
    ('PLASTIC_SURGERY_SPECIALIST', '성형외과 전문의 진료', 11, 'ACTIVE'),
    ('PARKING', '주차가능', 12, 'ACTIVE')
ON DUPLICATE KEY UPDATE
    name = VALUES(name),
    sort_order = VALUES(sort_order),
    status = VALUES(status),
    updated_at = CURRENT_TIMESTAMP(6);

UPDATE hospital_features
SET status = 'INACTIVE', updated_at = CURRENT_TIMESTAMP(6)
WHERE code NOT IN (
    'ANESTHESIOLOGIST',
    'INPATIENT_ROOM',
    'REAL_NAME_SYSTEM',
    'NIGHT_COUNSELING',
    'EMERGENCY_SYSTEM',
    'MULTIDISCIPLINARY_CARE',
    'DEDICATED_REST_AREA',
    'AFTERCARE',
    'FEMALE_DOCTOR_CARE',
    'STATION_WITHIN_5_MINUTES',
    'PLASTIC_SURGERY_SPECIALIST',
    'PARKING'
);
