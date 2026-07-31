ALTER TABLE partners
    DROP INDEX partners_deleted_department_id_idx,
    DROP INDEX partners_department_index,
    DROP COLUMN department;
