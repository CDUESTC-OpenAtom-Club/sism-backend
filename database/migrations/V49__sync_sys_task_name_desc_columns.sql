-- Canonicalize sys_task naming columns.
-- Stage 1:
-- - name / "desc" become the canonical columns used by application code.
-- - task_name / task_desc remain as compatibility mirrors until a later drop-column migration.

UPDATE public.sys_task
SET
    name = COALESCE(NULLIF(name, ''), task_name),
    "desc" = COALESCE(NULLIF("desc", ''), task_desc)
WHERE name IS NULL
   OR name = ''
   OR "desc" IS NULL
   OR "desc" = '';

UPDATE public.sys_task
SET
    task_name = COALESCE(NULLIF(task_name, ''), name),
    task_desc = COALESCE(NULLIF(task_desc, ''), "desc")
WHERE task_name IS NULL
   OR task_name = ''
   OR task_desc IS NULL
   OR task_desc = '';
