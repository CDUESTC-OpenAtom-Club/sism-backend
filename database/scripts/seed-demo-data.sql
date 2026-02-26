-- ============================================
-- SISM 演示数据生成脚本
-- 用途: 为系统生成符合业务规范的演示数据
-- 版本: V1.0
-- ============================================

\echo '============================================'
\echo 'SISM 演示数据生成脚本'
\echo '============================================'
\echo ''

BEGIN;

-- ============================================
-- 1. 清理现有数据（可选）
-- ============================================
-- DELETE FROM progress_report;
-- DELETE FROM indicator_milestone;
-- DELETE FROM indicator;
-- DELETE FROM sys_task;
-- DELETE FROM assessment_cycle;
-- DELETE FROM sys_user WHERE username NOT IN ('admin');
-- DELETE FROM sys_org WHERE id NOT IN (SELECT MIN(id) FROM sys_org);

-- ============================================
-- 2. 创建评估周期
-- ============================================
\echo '>>> 创建评估周期'

INSERT INTO assessment_cycle (id, name, year, start_date, end_date, status, created_at, updated_at)
VALUES
(1, '2026年度战略指标评估', 2026, '2026-01-01', '2026-12-31', 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

INSERT INTO assessment_cycle (id, name, year, start_date, end_date, status, created_at, updated_at)
VALUES
(2, '2025年度战略指标评估', 2025, '2025-01-01', '2025-12-31', 'COMPLETED', NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

RAISE NOTICE '✓ 已创建 2 个评估周期';

-- ============================================
-- 3. 创建组织结构
-- ============================================
\echo '>>> 创建组织结构'

-- 3.1 创建根节点：学校
INSERT INTO sys_org (id, name, type, parent_id, sort_order, is_active, created_at, updated_at)
VALUES
(1, '成都大学', 'SCHOOL', NULL, 1, true, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name;

-- 3.2 创建一级部门
INSERT INTO sys_org (id, name, type, parent_id, sort_order, is_active, created_at, updated_at)
VALUES
(2, '战略发展部', 'STRATEGIC_DEPT', 1, 1, true, NOW(), NOW()),
(3, '教务处', 'FUNCTIONAL_DEPT', 1, 2, true, NOW(), NOW()),
(4, '科研处', 'FUNCTIONAL_DEPT', 1, 3, true, NOW(), NOW()),
(5, '人事处', 'FUNCTIONAL_DEPT', 1, 4, true, NOW(), NOW()),
(6, '二级学院群组', 'COLLEGE_GROUP', 1, 5, true, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name;

-- 3.3 创建二级学院
INSERT INTO sys_org (id, name, type, parent_id, sort_order, is_active, created_at, updated_at)
VALUES
(101, '计算机科学与技术学院', 'SECONDARY_COLLEGE', 6, 1, true, NOW(), NOW()),
(102, '外国语学院', 'SECONDARY_COLLEGE', 6, 2, true, NOW(), NOW()),
(103, '经济管理学院', 'SECONDARY_COLLEGE', 6, 3, true, NOW(), NOW()),
(104, '艺术设计学院', 'SECONDARY_COLLEGE', 6, 4, true, NOW(), NOW()),
(105, '体育学院', 'SECONDARY_COLLEGE', 6, 5, true, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET name = EXCLUDED.name;

RAISE NOTICE '✓ 已创建组织结构';

-- ============================================
-- 4. 创建用户
-- ============================================
\echo '>>> 创建用户'

-- 系统管理员
INSERT INTO sys_user (id, username, password_hash, real_name, email, org_id, is_active, created_at, updated_at)
VALUES
(1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '系统管理员', 'admin@cduestc.cn', 2, true, NOW(), NOW())
ON CONFLICT (username) DO UPDATE SET real_name = EXCLUDED.real_name;

-- 各部门用户
INSERT INTO sys_user (id, username, password_hash, real_name, email, org_id, is_active, created_at, updated_at)
VALUES
(2, 'dept_strategic', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '战略发展部', 'strategic@cduestc.cn', 2, true, NOW(), NOW()),
(3, 'dept_academic', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '教务处', 'academic@cduestc.cn', 3, true, NOW(), NOW()),
(4, 'dept_research', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '科研处', 'research@cduestc.cn', 4, true, NOW(), NOW()),
(5, 'college_cs', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '计算机学院', 'cs@cduestc.cn', 101, true, NOW(), NOW()),
(6, 'college_foreign', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '外国语学院', 'foreign@cduestc.cn', 102, true, NOW(), NOW()),
(7, 'college_econ', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', '经济管理学院', 'econ@cduestc.cn', 103, true, NOW(), NOW())
ON CONFLICT (username) DO NOTHING;

RAISE NOTICE '✓ 已创建 7 个用户';

-- ============================================
-- 5. 创建战略任务
-- ============================================
\echo '>>> 创建战略任务'

INSERT INTO sys_task (id, task_name, task_type, cycle_id, org_id, created_by_org_id, status, created_at, updated_at)
VALUES
(1, '提升教学质量和人才培养水平', 'TEACHING_IMPROVEMENT', 1, 2, 2, 'ACTIVE', NOW(), NOW()),
(2, '加强科研创新能力建设', 'RESEARCH_INNOVATION', 1, 2, 2, 'ACTIVE', NOW(), NOW()),
(3, '推进师资队伍建设', 'FACULTY_DEVELOPMENT', 1, 2, 2, 'ACTIVE', NOW(), NOW()),
(4, '提升国际化办学水平', 'INTERNATIONALIZATION', 1, 2, 2, 'ACTIVE', NOW(), NOW()),
(5, '改善办学条件和基础设施', 'INFRASTRUCTURE', 1, 2, 2, 'ACTIVE', NOW(), NOW()),
(6, '加强党建工作', 'PARTY_BUILDING', 1, 2, 2, 'ACTIVE', NOW(), NOW()),
(7, '学生工作与服务', 'STUDENT_AFFAIRS', 1, 2, 2, 'ACTIVE', NOW(), NOW()),
(8, '社会服务与声誉建设', 'SOCIAL_SERVICE', 1, 2, 2, 'ACTIVE', NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET task_name = EXCLUDED.task_name;

RAISE NOTICE '✓ 已创建 8 个战略任务';

-- ============================================
-- 6. 创建战略指标
-- ============================================
\echo '>>> 创建战略指标'

-- 为每个任务创建 3-5 个指标
INSERT INTO indicator (
    id, indicator_desc, task_id, owner_org_id, target_org_id,
    level, weight_percent, sort_order, type, type1, type2,
    progress, status, year, is_deleted, created_at, updated_at
) VALUES
-- 任务1: 教学质量 (5个指标)
(1, '本科毕业生就业率达到90%以上', 1, 2, 3, 'PRIMARY', 20.0, 1, '基础性', '定量', '发展性', 75, 'ACTIVE', 2026, false, NOW(), NOW()),
(2, '研究生录取比例提升至15%', 1, 2, 3, 'PRIMARY', 15.0, 2, '发展性', '定量', '发展性', 60, 'ACTIVE', 2026, false, NOW(), NOW()),
(3, '一流课程建设达到50门', 1, 2, 3, 'PRIMARY', 15.0, 3, '发展性', '定量', '发展性', 40, 'ACTIVE', 2026, false, NOW(), NOW()),
(4, '教学成果奖获奖数增长10%', 1, 2, 101, 'SECONDARY', 25.0, 4, '基础性', '定量', '基础性', 80, 'ACTIVE', 2026, false, NOW(), NOW()),
(5, '学生满意度保持在85分以上', 1, 2, 3, 'PRIMARY', 25.0, 5, '基础性', '定量', '基础性', 88, 'ACTIVE', 2026, false, NOW(), NOW()),

-- 任务2: 科研创新 (4个指标)
(6, '国家级科研项目立项数达到50项', 2, 2, 4, 'PRIMARY', 25.0, 1, '发展性', '定量', '发展性', 45, 'ACTIVE', 2026, false, NOW(), NOW()),
(7, 'SCI论文发表数量增长20%', 2, 2, 101, 'SECONDARY', 25.0, 2, '基础性', '定量', '发展性', 70, 'ACTIVE', 2026, false, NOW(), NOW()),
(8, '科研经费到款额突破5000万', 2, 2, 4, 'PRIMARY', 25.0, 3, '发展性', '定量', '发展性', 55, 'ACTIVE', 2026, false, NOW(), NOW()),
(9, '省部级科研平台建设达到3个', 2, 2, 102, 'SECONDARY', 25.0, 4, '基础性', '定量', '发展性', 33, 'ACTIVE', 2026, false, NOW(), NOW()),

-- 任务3: 师资队伍 (4个指标)
(10, '专任教师中博士比例达到40%', 3, 2, 5, 'PRIMARY', 25.0, 1, '发展性', '定量', '发展性', 35, 'ACTIVE', 2026, false, NOW(), NOW()),
(11, '高层次人才引进达到30人', 3, 2, 5, 'PRIMARY', 25.0, 2, '发展性', '定量', '发展性', 50, 'ACTIVE', 2026, false, NOW(), NOW()),
(12, '教师出国进修比例达到15%', 3, 2, 103, 'SECONDARY', 25.0, 3, '基础性', '定量', '发展性', 25, 'ACTIVE', 2026, false, NOW(), NOW()),
(13, '双师型教师比例达到30%', 3, 2, 101, 'SECONDARY', 25.0, 4, '基础性', '定量', '基础性', 40, 'ACTIVE', 2026, false, NOW(), NOW()),

-- 任务4: 国际化 (3个指标)
(14, '国际合作院校达到20所', 4, 2, 2, 'PRIMARY', 30.0, 1, '发展性', '定量', '发展性', 65, 'ACTIVE', 2026, false, NOW(), NOW()),
(15, '留学生比例达到3%', 4, 2, 102, 'SECONDARY', 35.0, 2, '基础性', '定量', '发展性', 20, 'ACTIVE', 2026, false, NOW(), NOW()),
(16, '全英文授课课程达到30门', 4, 2, 103, 'SECONDARY', 35.0, 3, '基础性', '定量', '发展性', 45, 'ACTIVE', 2026, false, NOW(), NOW()),

-- 任务5: 基础设施 (3个指标)
(17, '教学科研用房面积增加10%', 5, 2, 3, 'PRIMARY', 30.0, 1, '基础性', '定量', '发展性', 70, 'ACTIVE', 2026, false, NOW(), NOW()),
(18, '实验室设备总值达到2亿', 5, 2, 4, 'PRIMARY', 35.0, 2, '发展性', '定量', '发展性', 60, 'ACTIVE', 2026, false, NOW(), NOW()),
(19, '智慧教室覆盖率达到80%', 5, 2, 101, 'SECONDARY', 35.0, 3, '基础性', '定量', '基础性', 75, 'ACTIVE', 2026, false, NOW(), NOW()),

-- 任务6: 党建工作 (3个指标)
(20, '党组织规范化建设达标率100%', 6, 2, 2, 'PRIMARY', 30.0, 1, '基础性', '定性', '基础性', 90, 'ACTIVE', 2026, false, NOW(), NOW()),
(21, '党员教师比例达到60%', 6, 2, 101, 'SECONDARY', 35.0, 2, '基础性', '定量', '基础性', 55, 'ACTIVE', 2026, false, NOW(), NOW()),
(22, '党建工作品牌项目达到5个', 6, 2, 102, 'SECONDARY', 35.0, 3, '发展性', '定性', '发展性', 60, 'ACTIVE', 2026, false, NOW(), NOW()),

-- 任务7: 学生工作 (4个指标)
(23, '学生竞赛获奖率达到15%', 7, 2, 3, 'PRIMARY', 25.0, 1, '基础性', '定量', '发展性', 68, 'ACTIVE', 2026, false, NOW(), NOW()),
(24, '创新创业参与率达到20%', 7, 2, 101, 'SECONDARY', 25.0, 2, '基础性', '定量', '发展性', 45, 'ACTIVE', 2026, false, NOW(), NOW()),
(25, '毕业生对母校满意度达到90%', 7, 2, 3, 'PRIMARY', 25.0, 3, '基础性', '定量', '基础性', 85, 'ACTIVE', 2026, false, NOW(), NOW()),
(26, '心理健康教育覆盖率100%', 7, 2, 103, 'SECONDARY', 25.0, 4, '基础性', '定性', '基础性', 95, 'ACTIVE', 2026, false, NOW(), NOW()),

-- 任务8: 社会服务 (3个指标)
(27, '横向课题经费增长15%', 8, 2, 4, 'PRIMARY', 30.0, 1, '发展性', '定量', '发展性', 50, 'ACTIVE', 2026, false, NOW(), NOW()),
(28, '智库报告采纳数达到20份', 8, 2, 102, 'SECONDARY', 35.0, 2, '发展性', '定量', '发展性', 40, 'ACTIVE', 2026, false, NOW(), NOW()),
(29, '社会培训人次达到5000人', 8, 2, 103, 'SECONDARY', 35.0, 3, '基础性', '定量', '发展性', 65, 'ACTIVE', 2026, false, NOW(), NOW())
ON CONFLICT (id) DO UPDATE SET indicator_desc = EXCLUDED.indicator_desc;

RAISE NOTICE '✓ 已创建 29 个战略指标';

-- ============================================
-- 7. 创建里程碑
-- ============================================
\echo '>>> 创建里程碑'

-- 为每个主要指标创建里程碑
INSERT INTO indicator_milestone (
    id, indicator_id, milestone_name, milestone_desc,
    due_date, target_progress, status, sort_order, created_at, updated_at
) VALUES
-- 指标1: 就业率
(1, 1, '第一季度就业率统计', '统计3月底毕业生就业情况', '2026-03-31', 25, 'PENDING', 1, NOW(), NOW()),
(2, 1, '第二季度就业率统计', '统计6月底毕业生就业情况', '2026-06-30', 50, 'PENDING', 2, NOW(), NOW()),
(3, 1, '第三季度就业率统计', '统计9月底毕业生就业情况', '2026-09-30', 75, 'PENDING', 3, NOW(), NOW()),
(4, 1, '年度就业率最终统计', '统计12月底毕业生就业情况', '2026-12-31', 90, 'PENDING', 4, NOW(), NOW()),

-- 指标6: 科研项目
(5, 6, '上半年项目立项统计', '统计6月底国家级项目立项情况', '2026-06-30', 25, 'PENDING', 1, NOW(), NOW()),
(6, 6, '下半年项目立项统计', '统计12月底国家级项目立项情况', '2026-12-31', 50, 'PENDING', 2, NOW(), NOW()),

-- 指标10: 教师博士化
(7, 10, '上半年博士比例统计', '统计6月底专任教师博士比例', '2026-06-30', 35, 'PENDING', 1, NOW(), NOW()),
(8, 10, '年度博士比例统计', '统计12月底专任教师博士比例', '2026-12-31', 40, 'PENDING', 2, NOW(), NOW()),

-- 指标14: 国际合作院校
(9, 14, '上半年合作院校统计', '统计6月底国际合作院校数量', '2026-06-30', 15, 'PENDING', 1, NOW(), NOW()),
(10, 14, '年度合作院校统计', '统计12月底国际合作院校数量', '2026-12-31', 20, 'PENDING', 2, NOW(), NOW()),

-- 指标17: 基础设施建设
(11, 17, '第一季度建设进度', '检查教学科研用房建设进度', '2026-03-31', 25, 'PENDING', 1, NOW(), NOW()),
(12, 17, '第二季度建设进度', '检查教学科研用房建设进度', '2026-06-30', 50, 'PENDING', 2, NOW(), NOW()),
(13, 17, '第三季度建设进度', '检查教学科研用房建设进度', '2026-09-30', 75, 'PENDING', 3, NOW(), NOW()),
(14, 17, '年度建设验收', '教学科研用房建设完成验收', '2026-12-31', 100, 'PENDING', 4, NOW(), NOW())
ON CONFLICT (id) DO NOTHING;

RAISE NOTICE '✓ 已创建 14 个里程碑';

COMMIT;

\echo ''
\echo '============================================'
\echo '演示数据生成完成！'
\echo '============================================'
\echo ''
\echo '数据摘要:'
\echo '  - 评估周期: 2 个'
\echo '  - 组织机构: 11 个'
\echo '  - 系统用户: 7 个'
\echo '  - 战略任务: 8 个'
\echo '  - 战略指标: 29 个'
\echo '  - 里程碑: 14 个'
\echo ''
\echo '默认登录账号:'
\echo '  用户名: admin'
\echo '  密码: admin123'
\echo ''
