-- ============================================================
-- 2026年度“战略发展部 -> 职能部门”联动数据补齐脚本
--
-- 目标：
-- 1. 补齐缺失职能部门的 plan / sys_task / indicator
-- 2. 为 2026 年职能部门指标补齐里程碑，形成完整联动链路
--
-- 约定：
-- - 战略发展部 org_id = 35
-- - 2026 年周期 cycle_id = 4
-- - 职能部门范围 org_id = 36 ~ 54
-- - 仅补齐 STRAT_TO_FUNC 层级数据，不触碰学院层数据
-- - 脚本可重复执行
-- ============================================================

BEGIN;

-- ------------------------------------------------------------
-- 0. 同步主键序列，避免线上历史手工插数导致 nextval 冲突
-- ------------------------------------------------------------
SELECT setval('plan_id_seq', COALESCE((SELECT MAX(id) FROM plan), 1), true);
SELECT setval('strategic_task_task_id_seq', COALESCE((SELECT MAX(task_id) FROM sys_task), 1), true);
SELECT setval('indicator_id_seq', COALESCE((SELECT MAX(id) FROM indicator), 1), true);
SELECT setval('indicator_milestone_id_seq', COALESCE((SELECT MAX(id) FROM indicator_milestone), 1), true);

-- ------------------------------------------------------------
-- A. 缺失职能部门计划补齐
-- ------------------------------------------------------------
WITH target_orgs AS (
  SELECT id, name
  FROM sys_org
  WHERE id BETWEEN 36 AND 54
    AND COALESCE(is_deleted, false) = false
),
missing_plans AS (
  SELECT o.id AS target_org_id
  FROM target_orgs o
  LEFT JOIN plan p
    ON p.target_org_id = o.id
   AND p.created_by_org_id = 35
   AND p.cycle_id = 4
   AND p.plan_level = 'STRAT_TO_FUNC'
   AND COALESCE(p.is_deleted, false) = false
  WHERE p.id IS NULL
)
INSERT INTO plan (
  cycle_id,
  created_at,
  updated_at,
  is_deleted,
  target_org_id,
  created_by_org_id,
  plan_level,
  status
)
SELECT
  4,
  NOW(),
  NOW(),
  false,
  mp.target_org_id,
  35,
  'STRAT_TO_FUNC',
  'DRAFT'
FROM missing_plans mp;

-- ------------------------------------------------------------
-- B. 缺失职能部门任务补齐
-- ------------------------------------------------------------
WITH target_orgs AS (
  SELECT id, name
  FROM sys_org
  WHERE id BETWEEN 36 AND 54
    AND COALESCE(is_deleted, false) = false
),
target_plans AS (
  SELECT p.id AS plan_id, p.target_org_id, o.name
  FROM plan p
  JOIN target_orgs o ON o.id = p.target_org_id
  WHERE p.created_by_org_id = 35
    AND p.cycle_id = 4
    AND p.plan_level = 'STRAT_TO_FUNC'
    AND COALESCE(p.is_deleted, false) = false
),
missing_tasks AS (
  SELECT tp.plan_id, tp.target_org_id, tp.name
  FROM target_plans tp
  LEFT JOIN sys_task t
    ON t.plan_id = tp.plan_id
   AND t.org_id = tp.target_org_id
   AND t.cycle_id = 4
   AND COALESCE(t.is_deleted, false) = false
  WHERE t.task_id IS NULL
)
INSERT INTO sys_task (
  created_at,
  updated_at,
  remark,
  sort_order,
  task_type,
  created_by_org_id,
  cycle_id,
  org_id,
  is_deleted,
  plan_id,
  name,
  "desc"
)
SELECT
  NOW(),
  NOW(),
  '战略发展部下发的2026年度重点任务',
  1,
  'BASIC',
  35,
  4,
  mt.target_org_id,
  false,
  mt.plan_id,
  CONCAT('2026年度', mt.name, '任务'),
  CONCAT('战略发展部向', mt.name, '下发的2026年度重点工作任务')
FROM missing_tasks mt;

-- ------------------------------------------------------------
-- C. 缺失职能部门指标补齐
-- 每个缺失职能部门新增 2 个指标，便于前端联调展示
-- ------------------------------------------------------------
WITH indicator_templates AS (
  SELECT *
  FROM (
    VALUES
      (39, 1, '基层党组织建设达标率100%', 50, '定量'),
      (39, 2, '年度干部教师培训覆盖率不低于95%', 50, '定量'),
      (41, 1, '学生日常教育管理满意度不低于90%', 50, '定量'),
      (41, 2, '重点学生群体帮扶覆盖率100%', 50, '定量'),
      (42, 1, '校园安全演练开展不少于12次', 50, '定量'),
      (42, 2, '重点风险隐患整改闭环率100%', 50, '定量'),
      (43, 1, '综合协调督办事项按期办结率100%', 50, '定量'),
      (43, 2, '重要会议与公文保障准确率100%', 50, '定量'),
      (49, 1, '实验室安全检查覆盖率100%', 50, '定量'),
      (49, 2, '重点实验室建设项目按期完成率100%', 50, '定量'),
      (50, 1, '核心业务系统可用率不低于99.5%', 50, '定量'),
      (50, 2, '数字校园重点项目年度完成率100%', 50, '定量'),
      (51, 1, '馆藏资源年度更新完成率100%', 50, '定量'),
      (51, 2, '师生借阅与档案服务满意度不低于95%', 50, '定量'),
      (53, 1, '继续教育年度招生目标完成率100%', 50, '定量'),
      (53, 2, '培训项目质量评价优秀率不低于90%', 50, '定量'),
      (54, 1, '国际合作项目年度推进完成率100%', 50, '定量'),
      (54, 2, '来访接待与交流活动满意度不低于95%', 50, '定量')
  ) AS t(target_org_id, sort_order, indicator_desc, weight_percent, indicator_type)
),
target_tasks AS (
  SELECT t.task_id, t.org_id AS target_org_id
  FROM sys_task t
  JOIN plan p ON p.id = t.plan_id
  WHERE t.cycle_id = 4
    AND p.created_by_org_id = 35
    AND p.plan_level = 'STRAT_TO_FUNC'
    AND t.org_id BETWEEN 36 AND 54
    AND COALESCE(t.is_deleted, false) = false
    AND COALESCE(p.is_deleted, false) = false
),
missing_indicators AS (
  SELECT
    tt.task_id,
    it.target_org_id,
    it.sort_order,
    it.indicator_desc,
    it.weight_percent,
    it.indicator_type
  FROM indicator_templates it
  JOIN target_tasks tt ON tt.target_org_id = it.target_org_id
  LEFT JOIN indicator i
    ON i.task_id = tt.task_id
   AND i.target_org_id = it.target_org_id
   AND i.indicator_desc = it.indicator_desc
   AND COALESCE(i.is_deleted, false) = false
  WHERE i.id IS NULL
)
INSERT INTO indicator (
  task_id,
  parent_indicator_id,
  indicator_desc,
  weight_percent,
  sort_order,
  remark,
  created_at,
  updated_at,
  type,
  progress,
  is_deleted,
  owner_org_id,
  target_org_id,
  status,
  responsible_user_id,
  is_enabled
)
SELECT
  mi.task_id,
  NULL,
  mi.indicator_desc,
  mi.weight_percent,
  mi.sort_order,
  '战略发展部下发到职能部门的2026年度指标',
  NOW(),
  NOW(),
  mi.indicator_type,
  0,
  false,
  35,
  mi.target_org_id,
  'DRAFT',
  NULL,
  true
FROM missing_indicators mi;

-- ------------------------------------------------------------
-- D. 为 2026 年职能部门指标补齐月度里程碑
-- 仅对当前没有任何里程碑的指标生成，避免重复
-- ------------------------------------------------------------
WITH functional_indicators AS (
  SELECT
    i.id AS indicator_id,
    i.indicator_desc
  FROM indicator i
  JOIN sys_task t ON t.task_id = i.task_id
  JOIN plan p ON p.id = t.plan_id
  WHERE p.created_by_org_id = 35
    AND p.plan_level = 'STRAT_TO_FUNC'
    AND p.cycle_id = 4
    AND t.org_id BETWEEN 36 AND 54
    AND COALESCE(p.is_deleted, false) = false
    AND COALESCE(t.is_deleted, false) = false
    AND COALESCE(i.is_deleted, false) = false
),
indicators_without_milestones AS (
  SELECT fi.indicator_id, fi.indicator_desc
  FROM functional_indicators fi
  LEFT JOIN indicator_milestone m ON m.indicator_id = fi.indicator_id
  GROUP BY fi.indicator_id, fi.indicator_desc
  HAVING COUNT(m.id) = 0
),
month_series AS (
  SELECT generate_series(1, 12) AS month_no
),
milestone_seed AS (
  SELECT
    iwm.indicator_id,
    CONCAT(iwm.indicator_desc, ' - ', ms.month_no, '月') AS milestone_name,
    CONCAT('2026年', ms.month_no, '月阶段性目标') AS milestone_desc,
    (make_date(2026, ms.month_no, 1) + INTERVAL '1 month - 1 day')::date AS due_date,
    ROUND(ms.month_no * 100.0 / 12)::int AS target_progress,
    ms.month_no AS sort_order
  FROM indicators_without_milestones iwm
  CROSS JOIN month_series ms
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
  ms.indicator_id,
  ms.milestone_name,
  ms.milestone_desc,
  ms.due_date,
  'NOT_STARTED',
  ms.sort_order,
  ms.target_progress,
  false
FROM milestone_seed ms
LEFT JOIN indicator_milestone m
  ON m.indicator_id = ms.indicator_id
 AND m.milestone_name = ms.milestone_name
WHERE m.id IS NULL;

COMMIT;
