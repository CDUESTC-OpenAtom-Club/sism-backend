-- 补齐缺失里程碑：2026-03-20
-- 目标：仅为当前没有任何里程碑的指标补齐月度里程碑
-- 当前确认缺失指标：indicator.id = 30059, 指标描述 = 设备维护完成100%

BEGIN;

-- 执行前检查：应返回 1
SELECT COUNT(*) AS missing_indicator_count
FROM indicator i
WHERE COALESCE(i.is_deleted, false) = false
  AND i.id = 30059
  AND NOT EXISTS (
    SELECT 1
    FROM indicator_milestone m
    WHERE m.indicator_id = i.id
  );

-- 校正主键序列，避免默认 nextval 撞到历史数据
SELECT setval(
  'public.indicator_milestone_id_seq',
  COALESCE((SELECT MAX(id) FROM indicator_milestone), 1),
  true
);

WITH month_template AS (
  SELECT *
  FROM (
    VALUES
      (1,  DATE '2026-01-31', 8),
      (2,  DATE '2026-02-28', 17),
      (3,  DATE '2026-03-31', 25),
      (4,  DATE '2026-04-30', 33),
      (5,  DATE '2026-05-31', 42),
      (6,  DATE '2026-06-30', 50),
      (7,  DATE '2026-07-31', 58),
      (8,  DATE '2026-08-31', 67),
      (9,  DATE '2026-09-30', 75),
      (10, DATE '2026-10-31', 83),
      (11, DATE '2026-11-30', 92),
      (12, DATE '2026-12-31', 100)
  ) AS t(sort_order, due_date, target_progress)
)
INSERT INTO indicator_milestone (
  indicator_id,
  milestone_name,
  milestone_desc,
  due_date,
  status,
  sort_order,
  target_progress,
  is_paired
)
SELECT
  i.id AS indicator_id,
  i.indicator_desc || ' - ' || mt.sort_order || '月' AS milestone_name,
  '2026年' || mt.sort_order || '月阶段性目标' AS milestone_desc,
  mt.due_date,
  'NOT_STARTED' AS status,
  mt.sort_order,
  mt.target_progress,
  false AS is_paired
FROM indicator i
JOIN month_template mt ON TRUE
WHERE i.id = 30059
  AND COALESCE(i.is_deleted, false) = false
  AND NOT EXISTS (
    SELECT 1
    FROM indicator_milestone m
    WHERE m.indicator_id = i.id
  );

-- 执行后检查：应返回 12
SELECT COUNT(*) AS inserted_milestone_count
FROM indicator_milestone
WHERE indicator_id = 30059;

COMMIT;
