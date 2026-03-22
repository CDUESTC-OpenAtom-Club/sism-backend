-- sys_user_role clean seed
-- Scope:
-- - Bind the workflow roles to the full organization test-account matrix.
-- - Keep only four business roles.
-- - College dean seats are merged into vice president role (4).
-- - Strategy final approver seats are merged into strategy dept head role (3).
-- - Functional department leader-like reserve accounts are kept as login accounts only and do not
--   receive workflow roles unless a workflow node really uses them.

BEGIN;

INSERT INTO public.sys_user_role (user_id, role_id, created_at) VALUES
    (124, 1, NOW()),
    (124, 2, NOW()),
    (124, 3, NOW()),
    (124, 4, NOW()),
    (188, 1, NOW()),
    (188, 3, NOW()),
    (189, 3, NOW()),
    (190, 3, NOW()),
    (191, 1, NOW()),
    (192, 2, NOW()),
    (215, 1, NOW()),
    (216, 2, NOW()),
    (223, 1, NOW()),
    (224, 2, NOW()),
    (267, 1, NOW()),
    (268, 2, NOW()),
    (269, 4, NOW()),
    (301, 1, NOW()),
    (302, 2, NOW()),
    (305, 1, NOW()),
    (306, 2, NOW()),
    (309, 1, NOW()),
    (310, 2, NOW()),
    (313, 1, NOW()),
    (314, 2, NOW()),
    (317, 1, NOW()),
    (318, 2, NOW()),
    (322, 1, NOW()),
    (323, 2, NOW()),
    (327, 1, NOW()),
    (328, 2, NOW()),
    (331, 1, NOW()),
    (332, 2, NOW()),
    (335, 1, NOW()),
    (336, 2, NOW()),
    (339, 1, NOW()),
    (340, 2, NOW()),
    (343, 1, NOW()),
    (344, 2, NOW()),
    (347, 1, NOW()),
    (348, 2, NOW()),
    (351, 1, NOW()),
    (352, 2, NOW()),
    (355, 1, NOW()),
    (356, 2, NOW()),
    (359, 1, NOW()),
    (360, 2, NOW()),
    (363, 1, NOW()),
    (364, 2, NOW()),
    (367, 1, NOW()),
    (368, 2, NOW()),
    (369, 4, NOW()),
    (370, 1, NOW()),
    (371, 2, NOW()),
    (372, 4, NOW()),
    (373, 1, NOW()),
    (374, 2, NOW()),
    (375, 4, NOW()),
    (376, 1, NOW()),
    (377, 2, NOW()),
    (378, 4, NOW()),
    (379, 1, NOW()),
    (380, 2, NOW()),
    (381, 4, NOW()),
    (382, 1, NOW()),
    (383, 2, NOW()),
    (384, 4, NOW()),
    (385, 1, NOW()),
    (386, 2, NOW()),
    (387, 4, NOW());

DELETE FROM public.sys_user_role
WHERE role_id NOT IN (1, 2, 3, 4);

COMMIT;
