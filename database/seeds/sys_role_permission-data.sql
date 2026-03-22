-- sys_role_permission clean seed
-- Scope:
-- - Keep only the reviewed role-permission matrix.
-- - Rebuild by business relation (role_id, perm_id), not by dirty relation-table id.
-- - Merged roles inherit the union of their original permissions:
--   role 9 absorbs former role 7, role 8 absorbs former role 10.

BEGIN;

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

    (8, 1, NOW()),
    (8, 2, NOW()),
    (8, 3, NOW()),
    (8, 4, NOW()),
    (8, 6, NOW()),
    (8, 7, NOW()),
    (8, 9, NOW()),
    (8, 11, NOW()),
    (8, 12, NOW()),

    (9, 1, NOW()),
    (9, 2, NOW()),
    (9, 3, NOW()),
    (9, 4, NOW()),
    (9, 6, NOW()),
    (9, 7, NOW()),
    (9, 9, NOW()),
    (9, 11, NOW()),
    (9, 12, NOW());

DELETE FROM public.sys_role_permission
WHERE role_id NOT IN (5, 6, 8, 9);

COMMIT;
