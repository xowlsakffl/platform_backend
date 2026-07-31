ALTER TABLE partners
    ADD COLUMN assigned_staff_id BIGINT NULL AFTER status,
    ADD INDEX partners_assigned_staff_id_index (assigned_staff_id),
    ADD CONSTRAINT fk_partners_assigned_staff
        FOREIGN KEY (assigned_staff_id) REFERENCES account_staffs (id) ON DELETE SET NULL;
