-- 插入组织数据
-- 执行方式: psql -U postgres -d strategic -f insert-organizations.sql

-- 1. 战略发展部 (STRATEGIC_DEPT)
INSERT INTO org (org_id, org_name, org_type, sort_order, is_active, created_at, updated_at)
VALUES (1, '战略发展部', 'STRATEGIC_DEPT', 0, true, NOW(), NOW());

-- 2. 二级学院 (SECONDARY_COLLEGE)
INSERT INTO org (org_id, org_name, org_type, sort_order, is_active, created_at, updated_at)
VALUES 
(100, '马克思主义学院', 'SECONDARY_COLLEGE', 1, true, NOW(), NOW()),
(101, '工学院', 'SECONDARY_COLLEGE', 2, true, NOW(), NOW()),
(102, '计算机学院', 'SECONDARY_COLLEGE', 3, true, NOW(), NOW()),
(103, '商学院', 'SECONDARY_COLLEGE', 4, true, NOW(), NOW()),
(104, '文理学院', 'SECONDARY_COLLEGE', 5, true, NOW(), NOW()),
(105, '艺术与科技学院', 'SECONDARY_COLLEGE', 6, true, NOW(), NOW()),
(106, '航空学院', 'SECONDARY_COLLEGE', 7, true, NOW(), NOW()),
(107, '国际教育学院', 'SECONDARY_COLLEGE', 8, true, NOW(), NOW());

-- 3. 职能部门 (FUNCTIONAL_DEPT)
-- 注意: org_id=3 的"党委保卫部 | 保卫处"已存在，跳过
INSERT INTO org (org_id, org_name, org_type, sort_order, is_active, created_at, updated_at)
VALUES 
(200, '党委办公室 | 党委联络部', 'FUNCTIONAL_DEPT', 1, true, NOW(), NOW()),
(201, '纪委办公室 | 监察处', 'FUNCTIONAL_DEPT', 2, true, NOW(), NOW()),
(202, '党委宣传部 | 宣传策划部', 'FUNCTIONAL_DEPT', 3, true, NOW(), NOW()),
(203, '党委组织部 | 党委教师工作部 | 人力资源部', 'FUNCTIONAL_DEPT', 4, true, NOW(), NOW()),
(204, '党委学工部 | 学生工作处', 'FUNCTIONAL_DEPT', 5, true, NOW(), NOW()),
(205, '党委保卫部 | 保卫处', 'FUNCTIONAL_DEPT', 6, true, NOW(), NOW())
ON CONFLICT (org_id) DO NOTHING,
(206, '学校综合办公室', 'FUNCTIONAL_DEPT', 7, true, NOW(), NOW()),
(207, '战略发展部', 'FUNCTIONAL_DEPT', 8, true, NOW(), NOW()),
(208, '教务处', 'FUNCTIONAL_DEPT', 9, true, NOW(), NOW()),
(209, '科技处', 'FUNCTIONAL_DEPT', 10, true, NOW(), NOW()),
(210, '财务部', 'FUNCTIONAL_DEPT', 11, true, NOW(), NOW()),
(211, '招生工作处', 'FUNCTIONAL_DEPT', 12, true, NOW(), NOW()),
(212, '就业创业指导中心', 'FUNCTIONAL_DEPT', 13, true, NOW(), NOW()),
(213, '实验室建设管理处', 'FUNCTIONAL_DEPT', 14, true, NOW(), NOW()),
(214, '数字校园建设办公室', 'FUNCTIONAL_DEPT', 15, true, NOW(), NOW()),
(215, '图书馆 | 档案馆', 'FUNCTIONAL_DEPT', 16, true, NOW(), NOW()),
(216, '后勤资产处', 'FUNCTIONAL_DEPT', 17, true, NOW(), NOW()),
(217, '继续教育部', 'FUNCTIONAL_DEPT', 18, true, NOW(), NOW()),
(218, '国际合作与交流处', 'FUNCTIONAL_DEPT', 19, true, NOW(), NOW());

-- 更新序列（如果使用序列生成ID）
SELECT setval('org_org_id_seq', (SELECT MAX(org_id) FROM org));

-- 验证插入结果
SELECT 
  org_type,
  COUNT(*) as count
FROM org
GROUP BY org_type
ORDER BY 
  CASE org_type
    WHEN 'STRATEGIC_DEPT' THEN 1
    WHEN 'FUNCTIONAL_DEPT' THEN 2
    WHEN 'SECONDARY_COLLEGE' THEN 3
  END;
