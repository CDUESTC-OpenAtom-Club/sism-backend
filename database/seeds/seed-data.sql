-- ============================================
-- SISM 示例数据 - 战略任务与指标
-- ============================================

-- 插入战略任务（战略发展部创建，分配给职能部门）
INSERT INTO strategic_task (cycle_id, task_name, task_desc, task_type, org_id, created_by_org_id, sort_order) VALUES
(1, '教学质量提升工程', '全面提升本科教学质量，推进课程改革与教学创新', 'DEVELOPMENT', 2, 1, 1),
(1, '科研能力建设计划', '加强科研平台建设，提升科研产出质量', 'DEVELOPMENT', 3, 1, 2),
(1, '人才队伍建设', '优化师资结构，引进高层次人才', 'BASIC', 4, 1, 3),
(1, '学生综合素质培养', '提升学生创新创业能力和综合素质', 'DEVELOPMENT', 5, 1, 4);

-- 插入一级指标（战略发展部→职能部门）
INSERT INTO indicator (task_id, parent_indicator_id, level, owner_org_id, target_org_id, indicator_desc, weight_percent, sort_order, year) VALUES
-- 教学质量提升工程的指标
(1, NULL, 'STRAT_TO_FUNC', 1, 2, '课程建设与改革', 40.00, 1, 2025),
(1, NULL, 'STRAT_TO_FUNC', 1, 2, '教学成果培育', 30.00, 2, 2025),
(1, NULL, 'STRAT_TO_FUNC', 1, 2, '教学质量监控', 30.00, 3, 2025),
-- 科研能力建设计划的指标
(2, NULL, 'STRAT_TO_FUNC', 1, 3, '科研项目申报', 35.00, 1, 2025),
(2, NULL, 'STRAT_TO_FUNC', 1, 3, '科研成果产出', 35.00, 2, 2025),
(2, NULL, 'STRAT_TO_FUNC', 1, 3, '科研平台建设', 30.00, 3, 2025),
-- 人才队伍建设的指标
(3, NULL, 'STRAT_TO_FUNC', 1, 4, '高层次人才引进', 50.00, 1, 2025),
(3, NULL, 'STRAT_TO_FUNC', 1, 4, '师资培训提升', 50.00, 2, 2025),
-- 学生综合素质培养的指标
(4, NULL, 'STRAT_TO_FUNC', 1, 5, '创新创业教育', 40.00, 1, 2025),
(4, NULL, 'STRAT_TO_FUNC', 1, 5, '学生竞赛获奖', 30.00, 2, 2025),
(4, NULL, 'STRAT_TO_FUNC', 1, 5, '就业质量提升', 30.00, 3, 2025);

-- 插入二级指标（职能部门→二级学院）
-- 教务处分解给计算机学院
INSERT INTO indicator (task_id, parent_indicator_id, level, owner_org_id, target_org_id, indicator_desc, weight_percent, sort_order, year) VALUES
(1, 1, 'FUNC_TO_COLLEGE', 2, 6, '计算机学院-新建精品课程2门', 25.00, 1, 2025),
(1, 1, 'FUNC_TO_COLLEGE', 2, 7, '商学院-新建精品课程2门', 25.00, 2, 2025),
(1, 1, 'FUNC_TO_COLLEGE', 2, 8, '外国语学院-新建精品课程1门', 25.00, 3, 2025),
(1, 1, 'FUNC_TO_COLLEGE', 2, 9, '艺术学院-新建精品课程1门', 25.00, 4, 2025);

-- 科研处分解给各学院
INSERT INTO indicator (task_id, parent_indicator_id, level, owner_org_id, target_org_id, indicator_desc, weight_percent, sort_order, year) VALUES
(2, 4, 'FUNC_TO_COLLEGE', 3, 6, '计算机学院-申报省部级项目3项', 30.00, 1, 2025),
(2, 4, 'FUNC_TO_COLLEGE', 3, 7, '商学院-申报省部级项目2项', 25.00, 2, 2025),
(2, 4, 'FUNC_TO_COLLEGE', 3, 10, '机械工程学院-申报省部级项目3项', 30.00, 3, 2025),
(2, 4, 'FUNC_TO_COLLEGE', 3, 9, '艺术学院-申报省部级项目1项', 15.00, 4, 2025);


-- 插入里程碑（为部分指标添加里程碑）
-- 指标1: 课程建设与改革 的里程碑
INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, sort_order) VALUES
(1, 'Q1-课程调研与规划', '完成课程改革调研报告，制定改革方案', '2025-03-31', 20.00, 1),
(1, 'Q2-课程大纲修订', '完成核心课程大纲修订', '2025-06-30', 30.00, 2),
(1, 'Q3-试点实施', '选取试点课程进行改革实施', '2025-09-30', 30.00, 3),
(1, 'Q4-总结推广', '总结经验，全面推广', '2025-12-31', 20.00, 4);

-- 指标4: 科研项目申报 的里程碑
INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, sort_order) VALUES
(4, '项目选题论证', '完成项目选题和可行性论证', '2025-03-15', 25.00, 1),
(4, '申报书撰写', '完成申报书初稿撰写', '2025-05-31', 35.00, 2),
(4, '专家评审修改', '组织专家评审并修改完善', '2025-07-31', 25.00, 3),
(4, '正式提交申报', '完成正式申报提交', '2025-09-30', 15.00, 4);

-- 指标12: 计算机学院-新建精品课程2门 的里程碑
INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, sort_order) VALUES
(12, '课程立项', '完成精品课程立项申报', '2025-03-31', 20.00, 1),
(12, '资源建设', '完成课程资源建设（视频、课件等）', '2025-08-31', 50.00, 2),
(12, '验收评审', '完成课程验收评审', '2025-12-15', 30.00, 3);

-- 指标16: 计算机学院-申报省部级项目3项 的里程碑
INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, sort_order) VALUES
(16, '项目遴选', '确定申报项目和负责人', '2025-02-28', 20.00, 1),
(16, '申报材料准备', '完成申报材料撰写', '2025-06-30', 50.00, 2),
(16, '提交申报', '完成项目申报提交', '2025-09-30', 30.00, 3);

-- 插入一些进度汇报示例
INSERT INTO progress_report (indicator_id, milestone_id, percent_complete, achieved_milestone, narrative, reporter_id, status, is_final, version_no, reported_at) VALUES
-- 计算机学院汇报课程立项进度
(12, 9, 100.00, TRUE, '已完成2门精品课程的立项申报，分别是《人工智能导论》和《大数据技术基础》，均已通过学校评审。', 6, 'APPROVED', TRUE, 1, '2025-03-28 10:00:00'),
-- 计算机学院汇报资源建设进度
(12, 10, 60.00, FALSE, '《人工智能导论》课程视频录制完成80%，《大数据技术基础》课件制作完成70%，整体进度60%。', 6, 'SUBMITTED', FALSE, 1, '2025-07-15 14:30:00');

-- 插入审批记录
INSERT INTO approval_record (report_id, approver_id, action, comment, acted_at) VALUES
(1, 2, 'APPROVE', '课程立项工作完成良好，同意通过。', '2025-03-29 09:00:00');

-- 插入临时任务示例
INSERT INTO adhoc_task (cycle_id, creator_org_id, scope_type, task_title, task_desc, open_at, due_at, status) VALUES
(1, 2, 'ALL_ORGS', '期中教学检查数据填报', '请各学院填报本学期期中教学检查相关数据', '2025-04-15', '2025-04-30', 'OPEN'),
(1, 3, 'BY_DEPT_ISSUED_INDICATORS', '科研项目进展月报', '请各学院汇报本月科研项目进展情况', '2025-05-01', '2025-05-10', 'DRAFT');

-- 临时任务目标组织
INSERT INTO adhoc_task_target (adhoc_task_id, target_org_id) VALUES
(1, 6), (1, 7), (1, 8), (1, 9), (1, 10);
