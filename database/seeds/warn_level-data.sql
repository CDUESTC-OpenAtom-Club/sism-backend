-- warn_level clean seed
-- Scope:
-- - Provide the canonical warning-level dictionary used by alert rules/events.

BEGIN;

INSERT INTO public.warn_level (
    id,
    level_code,
    level_name,
    severity,
    remark
)
VALUES
    (1, 'OK', '正常', 0, '进度符合预期，无需预警'),
    (2, 'INFO', '提示', 1, '存在轻微偏差，提醒关注'),
    (3, 'WARN', '预警', 2, '存在明显偏差，需要部门尽快纠偏'),
    (4, 'MAJOR', '严重', 3, '偏差较大，需要上级组织介入'),
    (5, 'CRITICAL', '危急', 4, '关键指标严重滞后，需要立即处置')
ON CONFLICT (id) DO UPDATE
SET
    level_code = EXCLUDED.level_code,
    level_name = EXCLUDED.level_name,
    severity = EXCLUDED.severity,
    remark = EXCLUDED.remark;

COMMIT;
