-- sys_role_permission clean seed
-- Scope:
-- - Keep only the reviewed role-permission matrix.
-- - Rebuild by business relation (role_id, perm_id), not by dirty relation-table id.

BEGIN;

DELETE FROM public.sys_role_permission
WHERE role_id IN (5, 6, 7, 8, 9, 10, 11, 12, 13);

INSERT INTO public.sys_role_permission (role_id, perm_id, created_at) VALUES
    (5, 1, NOW()),
    (5, 3, NOW()),
    (5, 4, NOW()),
    (5, 8, NOW()),
    (5, 10, NOW()),

    (6, 1, NOW()),
    (6, 3, NOW()),
    (6, 4, NOW()),
    (6, 9, NOW()),
    (6, 10, NOW()),
    (6, 11, NOW()),
    (6, 12, NOW()),

    (7, 1, NOW()),
    (7, 2, NOW()),
    (7, 3, NOW()),
    (7, 4, NOW()),
    (7, 5, NOW()),
    (7, 6, NOW()),
    (7, 7, NOW()),
    (7, 9, NOW()),
    (7, 11, NOW()),
    (7, 12, NOW()),

    (8, 1, NOW()),
    (8, 2, NOW()),
    (8, 3, NOW()),
    (8, 4, NOW()),
    (8, 6, NOW()),
    (8, 7, NOW()),
    (8, 9, NOW()),
    (8, 11, NOW()),
    (8, 12, NOW());

COMMIT;
