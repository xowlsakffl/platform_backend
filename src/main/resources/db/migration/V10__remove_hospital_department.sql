ALTER TABLE hospitals
    DROP INDEX hospitals_deleted_department_id_idx,
    DROP INDEX hospitals_department_index,
    DROP COLUMN department;
