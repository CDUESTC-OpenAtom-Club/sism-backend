-- Finalize sys_task naming-column convergence.
-- The runtime now reads and writes only name / "desc".

UPDATE public.sys_task
SET
    name = COALESCE(NULLIF(name, ''), task_name),
    "desc" = COALESCE(NULLIF("desc", ''), task_desc)
WHERE name IS NULL
   OR name = ''
   OR "desc" IS NULL
   OR "desc" = '';

ALTER TABLE public.sys_task
    DROP COLUMN IF EXISTS task_name,
    DROP COLUMN IF EXISTS task_desc;
