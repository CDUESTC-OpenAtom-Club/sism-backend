-- sys_role_permission clean seed
-- Scope:
-- - Keep only the reviewed role-permission matrix.
-- - Rebuild by business relation (role_id, perm_id), not by dirty relation-table id.
-- - Merged roles inherit the union of their original permissions:
--   role 4 absorbs former college-leader seat, role 3 absorbs former strategy-final seat.

BEGIN;

INSERT INTO public.sys_role_permission (role_id, perm_id, created_at) VALUES
    (1, 1, NOW()),
    (1, 3, NOW()),
    (1, 4, NOW()),
    (1, 8, NOW()),
    (1, 10, NOW()),

    (2, 1, NOW()),
    (2, 3, NOW()),
    (2, 4, NOW()),
    (2, 9, NOW()),
    (2, 10, NOW()),
    (2, 11, NOW()),
    (2, 12, NOW()),

    (3, 1, NOW()),
    (3, 2, NOW()),
    (3, 3, NOW()),
    (3, 4, NOW()),
    (3, 6, NOW()),
    (3, 7, NOW()),
    (3, 9, NOW()),
    (3, 11, NOW()),
    (3, 12, NOW()),

    (4, 1, NOW()),
    (4, 2, NOW()),
    (4, 3, NOW()),
    (4, 4, NOW()),
    (4, 6, NOW()),
    (4, 7, NOW()),
    (4, 9, NOW()),
    (4, 11, NOW()),
    (4, 12, NOW());

DELETE FROM public.sys_role_permission
WHERE role_id NOT IN (1, 2, 3, 4);

COMMIT;
