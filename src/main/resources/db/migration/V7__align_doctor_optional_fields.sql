ALTER TABLE hospital_doctors
    MODIFY COLUMN name VARCHAR(255) NOT NULL COMMENT '의료진명',
    MODIFY COLUMN gender VARCHAR(20) NULL COMMENT '성별(남, 여)',
    MODIFY COLUMN position VARCHAR(50) NULL COMMENT '직책(대표원장, 원장)';
