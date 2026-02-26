-- ============================================
-- SISM 2026年指标种子数据
-- 基于前端 Mock 数据 indicators2026.ts
-- 版本: V1.0
-- 日期: 2026-01-19
-- 需求: data-alignment-sop Requirements 4.1, 4.2, 7.1, 7.2
-- ============================================

-- ============================================
-- 第一部分：2026年考核周期和战略任务
-- ============================================

-- 确保2026年考核周期存在
INSERT INTO assessment_cycle (cycle_id, cycle_name, year, start_date, end_date, description) 
VALUES (4, '2026年度考核周期', 2026, '2026-01-01', '2026-12-31', '2026年度战略指标考核周期-前端对齐版')
ON CONFLICT (cycle_id) DO UPDATE SET 
    cycle_name = EXCLUDED.cycle_name,
    description = EXCLUDED.description;

-- 插入2026年战略任务（与前端 taskContent 对应）
INSERT INTO strategic_task (task_id, cycle_id, task_name, task_desc, task_type, org_id, created_by_org_id, sort_order) VALUES
(10, 4, '全力促进毕业生多元化高质量就业创业', '围绕毕业生就业质量提升，多措并举促进高质量就业创业', 'DEVELOPMENT', 6, 1, 1),
(11, 4, '推进校友工作提质增效，赋能校友成长', '建立完善校友反馈母校的工作机制，择优建立部分地区校友会', 'BASIC', 17, 1, 2),
(12, 4, '根据学校整体部署', '信息化相关数据报送准确、及时、可靠', 'BASIC', 9, 1, 3)
ON CONFLICT (task_id) DO UPDATE SET 
    task_name = EXCLUDED.task_name,
    task_desc = EXCLUDED.task_desc;


-- ============================================
-- 第二部分：定量指标 - 战略级（职能部门）
-- Requirements: 4.1, 4.2, 7.1
-- ============================================

-- 2026-101: 优质就业比例不低于15%
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit, 
    responsible_person, progress, status_audit, progress_approval_status
) VALUES (
    4101, 10, NULL, 'STRAT_TO_FUNC', 1, 6,
    '优质就业比例不低于15%', 20.00, 1, 2026, 'ACTIVE', '力争突破',
    FALSE, '定量', '发展性', FALSE, 15.00, '%',
    '张老师', 8, '[]', 'NONE'
) ON CONFLICT (indicator_id) DO UPDATE SET
    indicator_desc = EXCLUDED.indicator_desc,
    progress = EXCLUDED.progress,
    target_value = EXCLUDED.target_value;

-- 2026-102: 毕业生就业率不低于95%
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit,
    responsible_person, progress, status_audit, progress_approval_status
) VALUES (
    4102, 10, NULL, 'STRAT_TO_FUNC', 1, 6,
    '毕业生就业率不低于95%', 20.00, 2, 2026, 'ACTIVE', '确保就业率稳定',
    FALSE, '定量', '发展性', FALSE, 95.00, '%',
    '张老师', 12, '[]', 'NONE'
) ON CONFLICT (indicator_id) DO UPDATE SET
    indicator_desc = EXCLUDED.indicator_desc,
    progress = EXCLUDED.progress,
    target_value = EXCLUDED.target_value;

-- 2026-103: 针对各学院开设专业引进优质校招企业
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit,
    responsible_person, progress, status_audit, progress_approval_status
) VALUES (
    4103, 10, NULL, 'STRAT_TO_FUNC', 1, 8,
    '针对各学院开设专业引进优质校招企业（各专业大类不低于3家）', 20.00, 3, 2026, 'ACTIVE', '根据学校发展现状',
    FALSE, '定量', '发展性', TRUE, 3.00, '家/专业',
    '王主任', 5, '[]', 'NONE'
) ON CONFLICT (indicator_id) DO UPDATE SET
    indicator_desc = EXCLUDED.indicator_desc,
    progress = EXCLUDED.progress,
    target_value = EXCLUDED.target_value;

-- 2026-104: 毕业生创业比例不低于6%
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit,
    responsible_person, progress, status_audit, progress_approval_status
) VALUES (
    4104, 10, NULL, 'STRAT_TO_FUNC', 1, 6,
    '毕业生创业比例不低于6%', 20.00, 4, 2026, 'ACTIVE', '鼓励创业精神',
    FALSE, '定量', '发展性', FALSE, 6.00, '%',
    '张老师', 6, '[]', 'NONE'
) ON CONFLICT (indicator_id) DO UPDATE SET
    indicator_desc = EXCLUDED.indicator_desc,
    progress = EXCLUDED.progress,
    target_value = EXCLUDED.target_value;

-- 2026-105: 就业率达92%，教育厅公布的就业数据排名川内同类民办院校前三
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit,
    responsible_person, progress, status_audit, progress_approval_status
) VALUES (
    4105, 10, NULL, 'STRAT_TO_FUNC', 1, 6,
    '就业率达92%，教育厅公布的就业数据排名川内同类民办院校前三', 50.00, 5, 2026, 'ACTIVE', '中长期发展规划内容',
    FALSE, '定量', '发展性', FALSE, 92.00, '%',
    '李老师', 10, '[]', 'NONE'
) ON CONFLICT (indicator_id) DO UPDATE SET
    indicator_desc = EXCLUDED.indicator_desc,
    progress = EXCLUDED.progress,
    target_value = EXCLUDED.target_value;


-- ============================================
-- 第三部分：定量指标 - 二级学院（子指标）
-- 父指标: 4101 优质就业比例
-- ============================================

-- 2026-101-1: 计算机学院优质就业比例不低于18%
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit,
    responsible_person, progress, progress_approval_status, pending_progress, pending_remark,
    status_audit
) VALUES (
    4111, 10, 4101, 'FUNC_TO_COLLEGE', 6, 23,
    '计算机学院优质就业比例不低于18%', 25.00, 1, 2026, 'ACTIVE', '工科学院就业质量要求更高',
    FALSE, '定量', '发展性', FALSE, 18.00, '%',
    '赵院长', 15, 'PENDING', 20, '已完成Q1就业数据统计，优质就业比例达到20%',
    '[{"id":"audit-101-1-1","timestamp":"2026-01-12T00:00:00","operator":"jyc-admin","operatorName":"就业中心管理员","operatorDept":"就业创业指导中心","action":"distribute","comment":"下发子指标给计算机学院"},{"id":"audit-101-1-2","timestamp":"2026-01-20T00:00:00","operator":"zhao-dean","operatorName":"赵院长","operatorDept":"计算机学院","action":"submit","comment":"提交Q1进度，优质就业比例达到15%","previousProgress":0,"newProgress":15},{"id":"audit-101-1-3","timestamp":"2026-01-22T00:00:00","operator":"jyc-admin","operatorName":"就业中心管理员","operatorDept":"就业创业指导中心","action":"approve","comment":"审批通过，进度符合预期"},{"id":"audit-101-1-4","timestamp":"2026-01-28T00:00:00","operator":"zhao-dean","operatorName":"赵院长","operatorDept":"计算机学院","action":"submit","comment":"更新进度至20%，就业质量持续提升","previousProgress":15,"newProgress":20}]'
) ON CONFLICT (indicator_id) DO UPDATE SET
    progress = EXCLUDED.progress,
    pending_progress = EXCLUDED.pending_progress,
    status_audit = EXCLUDED.status_audit;

-- 2026-101-2: 商学院优质就业比例不低于12%
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit,
    responsible_person, progress, progress_approval_status,
    status_audit
) VALUES (
    4112, 10, 4101, 'FUNC_TO_COLLEGE', 6, 24,
    '商学院优质就业比例不低于12%', 20.00, 2, 2026, 'ACTIVE', '商科专业就业质量稳步提升',
    FALSE, '定量', '发展性', FALSE, 12.00, '%',
    '钱院长', 10, 'APPROVED',
    '[{"id":"audit-101-2-1","timestamp":"2026-01-12T00:00:00","operator":"jyc-admin","operatorName":"就业中心管理员","operatorDept":"就业创业指导中心","action":"distribute","comment":"下发子指标给商学院"},{"id":"audit-101-2-2","timestamp":"2026-01-18T00:00:00","operator":"qian-dean","operatorName":"钱院长","operatorDept":"商学院","action":"submit","comment":"提交Q1进度，优质就业比例达到10%","previousProgress":0,"newProgress":10},{"id":"audit-101-2-3","timestamp":"2026-01-20T00:00:00","operator":"jyc-admin","operatorName":"就业中心管理员","operatorDept":"就业创业指导中心","action":"approve","comment":"审批通过"}]'
) ON CONFLICT (indicator_id) DO UPDATE SET
    progress = EXCLUDED.progress,
    status_audit = EXCLUDED.status_audit;

-- 2026-101-3: 艺术与科技学院优质就业比例不低于10%
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit,
    responsible_person, progress, progress_approval_status, pending_progress, pending_remark,
    status_audit
) VALUES (
    4113, 10, 4101, 'FUNC_TO_COLLEGE', 6, 26,
    '艺术与科技学院优质就业比例不低于10%', 15.00, 3, 2026, 'ACTIVE', '艺术类专业就业质量提升',
    FALSE, '定量', '发展性', FALSE, 10.00, '%',
    '孙院长', 5, 'PENDING', 8, '艺术类专业就业渠道拓展中，预计Q2达标',
    '[{"id":"audit-101-3-1","timestamp":"2026-01-12T00:00:00","operator":"jyc-admin","operatorName":"就业中心管理员","operatorDept":"就业创业指导中心","action":"distribute","comment":"下发子指标给艺术与科技学院"},{"id":"audit-101-3-2","timestamp":"2026-01-25T00:00:00","operator":"sun-dean","operatorName":"孙院长","operatorDept":"艺术与科技学院","action":"submit","comment":"提交进度更新，就业渠道拓展中","previousProgress":5,"newProgress":8}]'
) ON CONFLICT (indicator_id) DO UPDATE SET
    progress = EXCLUDED.progress,
    pending_progress = EXCLUDED.pending_progress,
    status_audit = EXCLUDED.status_audit;

-- 2026-101-4: 工学院优质就业比例不低于16%
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit,
    responsible_person, progress, progress_approval_status,
    status_audit
) VALUES (
    4114, 10, 4101, 'FUNC_TO_COLLEGE', 6, 22,
    '工学院优质就业比例不低于16%', 22.00, 4, 2026, 'ACTIVE', '工科类专业就业质量要求高',
    FALSE, '定量', '发展性', FALSE, 16.00, '%',
    '李院长', 12, 'APPROVED',
    '[{"id":"audit-101-4-1","timestamp":"2026-01-12T00:00:00","operator":"jyc-admin","operatorName":"就业中心管理员","operatorDept":"就业创业指导中心","action":"distribute","comment":"下发子指标给工学院"},{"id":"audit-101-4-2","timestamp":"2026-01-15T00:00:00","operator":"li-dean","operatorName":"李院长","operatorDept":"工学院","action":"submit","comment":"提交初始进度9%","previousProgress":0,"newProgress":9},{"id":"audit-101-4-3","timestamp":"2026-01-17T00:00:00","operator":"jyc-admin","operatorName":"就业中心管理员","operatorDept":"就业创业指导中心","action":"reject","comment":"进度偏低，请加强就业指导工作"},{"id":"audit-101-4-4","timestamp":"2026-01-22T00:00:00","operator":"li-dean","operatorName":"李院长","operatorDept":"工学院","action":"submit","comment":"重新提交，已加强就业指导，进度提升至12%","previousProgress":9,"newProgress":12},{"id":"audit-101-4-5","timestamp":"2026-01-24T00:00:00","operator":"jyc-admin","operatorName":"就业中心管理员","operatorDept":"就业创业指导中心","action":"approve","comment":"审批通过，继续保持"}]'
) ON CONFLICT (indicator_id) DO UPDATE SET
    progress = EXCLUDED.progress,
    status_audit = EXCLUDED.status_audit;

-- 2026-101-5: 航空学院优质就业比例不低于17%
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit,
    responsible_person, progress, progress_approval_status,
    status_audit
) VALUES (
    4115, 10, 4101, 'FUNC_TO_COLLEGE', 6, 27,
    '航空学院优质就业比例不低于17%', 25.00, 5, 2026, 'ACTIVE', '航空专业就业质量优势明显',
    FALSE, '定量', '发展性', FALSE, 17.00, '%',
    '周院长', 15, 'APPROVED',
    '[{"id":"audit-101-5-1","timestamp":"2026-01-12T00:00:00","operator":"jyc-admin","operatorName":"就业中心管理员","operatorDept":"就业创业指导中心","action":"distribute","comment":"下发子指标给航空学院"},{"id":"audit-101-5-2","timestamp":"2026-01-18T00:00:00","operator":"zhou-dean","operatorName":"周院长","operatorDept":"航空学院","action":"submit","comment":"提交Q1进度，航空专业就业优势明显","previousProgress":0,"newProgress":15},{"id":"audit-101-5-3","timestamp":"2026-01-20T00:00:00","operator":"jyc-admin","operatorName":"就业中心管理员","operatorDept":"就业创业指导中心","action":"approve","comment":"审批通过，表现优秀"}]'
) ON CONFLICT (indicator_id) DO UPDATE SET
    progress = EXCLUDED.progress,
    status_audit = EXCLUDED.status_audit;


-- ============================================
-- 第四部分：定量指标 - 二级学院（子指标）
-- 父指标: 4102 毕业生就业率
-- ============================================

-- 2026-102-1: 计算机学院就业率达93%以上
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit,
    responsible_person, progress, progress_approval_status, pending_progress, pending_remark,
    status_audit
) VALUES (
    4121, 10, 4102, 'FUNC_TO_COLLEGE', 6, 23,
    '计算机学院就业率达93%以上', 30.00, 1, 2026, 'ACTIVE', '工科就业率目标更高',
    FALSE, '定量', '发展性', FALSE, 93.00, '%',
    '赵院长', 15, 'PENDING', 18, '就业率稳步提升，预计Q2可达25%',
    '[{"id":"audit-102-1-1","timestamp":"2026-01-12T00:00:00","operator":"jyc-admin","operatorName":"就业中心管理员","operatorDept":"就业创业指导中心","action":"distribute","comment":"下发子指标给计算机学院"},{"id":"audit-102-1-2","timestamp":"2026-01-20T00:00:00","operator":"zhao-dean","operatorName":"赵院长","operatorDept":"计算机学院","action":"submit","comment":"提交Q1就业率进度","previousProgress":0,"newProgress":15},{"id":"audit-102-1-3","timestamp":"2026-01-22T00:00:00","operator":"jyc-admin","operatorName":"就业中心管理员","operatorDept":"就业创业指导中心","action":"approve","comment":"审批通过"},{"id":"audit-102-1-4","timestamp":"2026-01-28T00:00:00","operator":"zhao-dean","operatorName":"赵院长","operatorDept":"计算机学院","action":"submit","comment":"更新进度至18%","previousProgress":15,"newProgress":18}]'
) ON CONFLICT (indicator_id) DO UPDATE SET
    progress = EXCLUDED.progress,
    pending_progress = EXCLUDED.pending_progress,
    status_audit = EXCLUDED.status_audit;

-- 2026-102-2: 商学院就业率达91%以上
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit,
    responsible_person, progress, progress_approval_status,
    status_audit
) VALUES (
    4122, 10, 4102, 'FUNC_TO_COLLEGE', 6, 24,
    '商学院就业率达91%以上', 25.00, 2, 2026, 'ACTIVE', '商科就业率稳步提升',
    FALSE, '定量', '发展性', FALSE, 91.00, '%',
    '钱院长', 12, 'APPROVED',
    '[{"id":"audit-102-2-1","timestamp":"2026-01-12T00:00:00","operator":"jyc-admin","operatorName":"就业中心管理员","operatorDept":"就业创业指导中心","action":"distribute","comment":"下发子指标给商学院"},{"id":"audit-102-2-2","timestamp":"2026-01-19T00:00:00","operator":"qian-dean","operatorName":"钱院长","operatorDept":"商学院","action":"submit","comment":"提交Q1就业率进度12%","previousProgress":0,"newProgress":12},{"id":"audit-102-2-3","timestamp":"2026-01-21T00:00:00","operator":"jyc-admin","operatorName":"就业中心管理员","operatorDept":"就业创业指导中心","action":"approve","comment":"审批通过"}]'
) ON CONFLICT (indicator_id) DO UPDATE SET
    progress = EXCLUDED.progress,
    status_audit = EXCLUDED.status_audit;

-- 2026-102-3: 文理学院就业率达89%以上
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit,
    responsible_person, progress, progress_approval_status, pending_progress, pending_remark,
    status_audit
) VALUES (
    4123, 10, 4102, 'FUNC_TO_COLLEGE', 6, 25,
    '文理学院就业率达89%以上', 20.00, 3, 2026, 'ACTIVE', '文理综合类专业就业稳步提升',
    FALSE, '定量', '发展性', FALSE, 89.00, '%',
    '吴院长', 10, 'PENDING', 12, '文理学院就业指导工作持续推进中',
    '[{"id":"audit-102-3-1","timestamp":"2026-01-12T00:00:00","operator":"jyc-admin","operatorName":"就业中心管理员","operatorDept":"就业创业指导中心","action":"distribute","comment":"下发子指标给文理学院"},{"id":"audit-102-3-2","timestamp":"2026-01-20T00:00:00","operator":"wu-dean","operatorName":"吴院长","operatorDept":"文理学院","action":"submit","comment":"提交Q1就业率进度10%","previousProgress":0,"newProgress":10},{"id":"audit-102-3-3","timestamp":"2026-01-22T00:00:00","operator":"jyc-admin","operatorName":"就业中心管理员","operatorDept":"就业创业指导中心","action":"approve","comment":"审批通过"},{"id":"audit-102-3-4","timestamp":"2026-01-28T00:00:00","operator":"wu-dean","operatorName":"吴院长","operatorDept":"文理学院","action":"submit","comment":"更新进度至12%","previousProgress":10,"newProgress":12}]'
) ON CONFLICT (indicator_id) DO UPDATE SET
    progress = EXCLUDED.progress,
    pending_progress = EXCLUDED.pending_progress,
    status_audit = EXCLUDED.status_audit;

-- 2026-102-4: 国际教育学院就业率达86%以上
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit,
    responsible_person, progress, progress_approval_status,
    status_audit
) VALUES (
    4124, 10, 4102, 'FUNC_TO_COLLEGE', 6, 28,
    '国际教育学院就业率达86%以上', 18.00, 4, 2026, 'ACTIVE', '国际化专业就业多元化',
    FALSE, '定量', '发展性', FALSE, 86.00, '%',
    '郑院长', 12, 'APPROVED',
    '[{"id":"audit-102-4-1","timestamp":"2026-01-12T00:00:00","operator":"jyc-admin","operatorName":"就业中心管理员","operatorDept":"就业创业指导中心","action":"distribute","comment":"下发子指标给国际教育学院"},{"id":"audit-102-4-2","timestamp":"2026-01-18T00:00:00","operator":"zheng-dean","operatorName":"郑院长","operatorDept":"国际教育学院","action":"submit","comment":"提交Q1就业率进度，国际化就业渠道拓展中","previousProgress":0,"newProgress":12},{"id":"audit-102-4-3","timestamp":"2026-01-20T00:00:00","operator":"jyc-admin","operatorName":"就业中心管理员","operatorDept":"就业创业指导中心","action":"approve","comment":"审批通过，继续保持"}]'
) ON CONFLICT (indicator_id) DO UPDATE SET
    progress = EXCLUDED.progress,
    status_audit = EXCLUDED.status_audit;


-- ============================================
-- 第五部分：定量指标 - 教务处课程优良率
-- ============================================

-- 2026-401: 提升教学质量，课程优良率达87%以上
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit,
    responsible_person, progress, status_audit, progress_approval_status
) VALUES (
    4401, 10, NULL, 'STRAT_TO_FUNC', 1, 2,
    '提升教学质量，课程优良率达87%以上', 30.00, 1, 2026, 'ACTIVE', '中长期发展规划核心内容',
    FALSE, '定量', '基础性', FALSE, 87.00, '%',
    '陈处长', 12, '[]', 'NONE'
) ON CONFLICT (indicator_id) DO UPDATE SET
    indicator_desc = EXCLUDED.indicator_desc,
    progress = EXCLUDED.progress,
    target_value = EXCLUDED.target_value;

-- 2026-401-1: 计算机学院课程优良率达90%
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit,
    responsible_person, progress, progress_approval_status, pending_progress, pending_remark,
    status_audit
) VALUES (
    4411, 10, 4401, 'FUNC_TO_COLLEGE', 2, 23,
    '计算机学院课程优良率达90%', 25.00, 1, 2026, 'ACTIVE', '工科专业教学质量领先',
    FALSE, '定量', '基础性', FALSE, 90.00, '%',
    '赵院长', 10, 'PENDING', 15, '本学期课程优良率统计完成，达到15%',
    '[{"id":"audit-401-1-1","timestamp":"2026-01-12T00:00:00","operator":"jiaowu-admin","operatorName":"教务处管理员","operatorDept":"教务处","action":"distribute","comment":"下发子指标给计算机学院"},{"id":"audit-401-1-2","timestamp":"2026-01-20T00:00:00","operator":"zhao-dean","operatorName":"赵院长","operatorDept":"计算机学院","action":"submit","comment":"提交Q1课程优良率数据","previousProgress":0,"newProgress":10},{"id":"audit-401-1-3","timestamp":"2026-01-22T00:00:00","operator":"jiaowu-admin","operatorName":"教务处管理员","operatorDept":"教务处","action":"approve","comment":"审批通过"},{"id":"audit-401-1-4","timestamp":"2026-01-28T00:00:00","operator":"zhao-dean","operatorName":"赵院长","operatorDept":"计算机学院","action":"submit","comment":"更新进度至15%，教学质量持续提升","previousProgress":10,"newProgress":15}]'
) ON CONFLICT (indicator_id) DO UPDATE SET
    progress = EXCLUDED.progress,
    pending_progress = EXCLUDED.pending_progress,
    status_audit = EXCLUDED.status_audit;

-- 2026-401-2: 工学院课程优良率达88%
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit,
    responsible_person, progress, progress_approval_status,
    status_audit
) VALUES (
    4412, 10, 4401, 'FUNC_TO_COLLEGE', 2, 22,
    '工学院课程优良率达88%', 24.00, 2, 2026, 'ACTIVE', '工科教学质量稳步提升',
    FALSE, '定量', '基础性', FALSE, 88.00, '%',
    '李院长', 9, 'APPROVED',
    '[{"id":"audit-401-2-1","timestamp":"2026-01-12T00:00:00","operator":"jiaowu-admin","operatorName":"教务处管理员","operatorDept":"教务处","action":"distribute","comment":"下发子指标给工学院"},{"id":"audit-401-2-2","timestamp":"2026-01-18T00:00:00","operator":"li-dean","operatorName":"李院长","operatorDept":"工学院","action":"submit","comment":"提交Q1课程优良率数据9%","previousProgress":0,"newProgress":9},{"id":"audit-401-2-3","timestamp":"2026-01-20T00:00:00","operator":"jiaowu-admin","operatorName":"教务处管理员","operatorDept":"教务处","action":"approve","comment":"审批通过"}]'
) ON CONFLICT (indicator_id) DO UPDATE SET
    progress = EXCLUDED.progress,
    status_audit = EXCLUDED.status_audit;

-- 2026-401-3: 商学院课程优良率达85%
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit,
    responsible_person, progress, progress_approval_status, pending_progress, pending_remark,
    status_audit
) VALUES (
    4413, 10, 4401, 'FUNC_TO_COLLEGE', 2, 24,
    '商学院课程优良率达85%', 20.00, 3, 2026, 'ACTIVE', '商科教学质量提升',
    FALSE, '定量', '基础性', FALSE, 85.00, '%',
    '钱院长', 8, 'PENDING', 12, '商学院课程优良率统计完成，达到12%',
    '[{"id":"audit-401-3-1","timestamp":"2026-01-12T00:00:00","operator":"jiaowu-admin","operatorName":"教务处管理员","operatorDept":"教务处","action":"distribute","comment":"下发子指标给商学院"},{"id":"audit-401-3-2","timestamp":"2026-01-22T00:00:00","operator":"qian-dean","operatorName":"钱院长","operatorDept":"商学院","action":"submit","comment":"提交Q1课程优良率数据12%","previousProgress":0,"newProgress":12}]'
) ON CONFLICT (indicator_id) DO UPDATE SET
    progress = EXCLUDED.progress,
    pending_progress = EXCLUDED.pending_progress,
    status_audit = EXCLUDED.status_audit;

-- 2026-401-4: 文理学院课程优良率达84%
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit,
    responsible_person, progress, progress_approval_status,
    status_audit
) VALUES (
    4414, 10, 4401, 'FUNC_TO_COLLEGE', 2, 25,
    '文理学院课程优良率达84%', 18.00, 4, 2026, 'ACTIVE', '文理综合类教学质量提升',
    FALSE, '定量', '基础性', FALSE, 84.00, '%',
    '吴院长', 7, 'APPROVED',
    '[{"id":"audit-401-4-1","timestamp":"2026-01-12T00:00:00","operator":"jiaowu-admin","operatorName":"教务处管理员","operatorDept":"教务处","action":"distribute","comment":"下发子指标给文理学院"},{"id":"audit-401-4-2","timestamp":"2026-01-20T00:00:00","operator":"wu-dean","operatorName":"吴院长","operatorDept":"文理学院","action":"submit","comment":"提交Q1课程优良率数据7%","previousProgress":0,"newProgress":7},{"id":"audit-401-4-3","timestamp":"2026-01-22T00:00:00","operator":"jiaowu-admin","operatorName":"教务处管理员","operatorDept":"教务处","action":"approve","comment":"审批通过"}]'
) ON CONFLICT (indicator_id) DO UPDATE SET
    progress = EXCLUDED.progress,
    status_audit = EXCLUDED.status_audit;


-- ============================================
-- 第六部分：更多定量指标 - 职能部门
-- ============================================

-- 2026-402: 提升科研水平，年度发表高水平论文不少于55篇
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit,
    responsible_person, progress, status_audit, progress_approval_status
) VALUES (
    4402, 10, NULL, 'STRAT_TO_FUNC', 1, 3,
    '提升科研水平，年度发表高水平论文不少于55篇', 25.00, 1, 2026, 'ACTIVE', '科研水平提升关键指标',
    FALSE, '定量', '发展性', FALSE, 55.00, '篇',
    '林处长', 8, '[]', 'NONE'
) ON CONFLICT (indicator_id) DO UPDATE SET
    progress = EXCLUDED.progress,
    target_value = EXCLUDED.target_value;

-- 2026-403: 加强学生思想政治教育，学生满意度达92%以上
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit,
    responsible_person, progress, status_audit, progress_approval_status
) VALUES (
    4403, 10, NULL, 'STRAT_TO_FUNC', 1, 5,
    '加强学生思想政治教育，学生满意度达92%以上', 20.00, 1, 2026, 'ACTIVE', '立德树人根本任务',
    FALSE, '定量', '基础性', FALSE, 92.00, '%',
    '王部长', 15, '[]', 'NONE'
) ON CONFLICT (indicator_id) DO UPDATE SET
    progress = EXCLUDED.progress,
    target_value = EXCLUDED.target_value;

-- 2026-404: 提升生源质量，一志愿录取率达78%以上
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit,
    responsible_person, progress, status_audit, progress_approval_status
) VALUES (
    4404, 10, NULL, 'STRAT_TO_FUNC', 1, 8,
    '提升生源质量，一志愿录取率达78%以上', 22.00, 1, 2026, 'ACTIVE', '生源质量提升关键',
    FALSE, '定量', '发展性', FALSE, 78.00, '%',
    '刘处长', 5, '[]', 'NONE'
) ON CONFLICT (indicator_id) DO UPDATE SET
    progress = EXCLUDED.progress,
    target_value = EXCLUDED.target_value;

-- 2026-501: 加强党建工作，党员发展质量达标率96%以上
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit,
    responsible_person, progress, progress_approval_status, pending_progress, pending_remark,
    status_audit
) VALUES (
    4501, 12, NULL, 'STRAT_TO_FUNC', 1, 12,
    '加强党建工作，党员发展质量达标率96%以上', 25.00, 1, 2026, 'ACTIVE', '党建引领发展',
    FALSE, '定量', '基础性', FALSE, 96.00, '%',
    '李书记', 25, 'PENDING', 28, 'Q1党员发展工作顺利完成，发展党员12名，待审批',
    '[{"id":"audit-501-1","timestamp":"2026-01-10T00:00:00","operator":"li-shuji","operatorName":"李书记","operatorDept":"党委办公室 | 党委统战部","action":"submit","comment":"提交Q1进度，党员发展工作启动","previousProgress":0,"newProgress":10},{"id":"audit-501-2","timestamp":"2026-01-12T00:00:00","operator":"strategy-admin","operatorName":"战略发展部管理员","operatorDept":"战略发展部","action":"approve","comment":"审批通过，进度符合预期"},{"id":"audit-501-3","timestamp":"2026-01-28T00:00:00","operator":"li-shuji","operatorName":"李书记","operatorDept":"党委办公室 | 党委统战部","action":"submit","comment":"更新进度，党员发展培训完成，待审批","previousProgress":10,"newProgress":28}]'
) ON CONFLICT (indicator_id) DO UPDATE SET
    progress = EXCLUDED.progress,
    pending_progress = EXCLUDED.pending_progress,
    status_audit = EXCLUDED.status_audit;

-- 2026-503: 党风廉政建设责任制落实率100%
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit,
    responsible_person, progress, progress_approval_status, pending_progress, pending_remark,
    status_audit
) VALUES (
    4503, 12, NULL, 'STRAT_TO_FUNC', 1, 12,
    '党风廉政建设责任制落实率100%', 20.00, 2, 2026, 'ACTIVE', '全面从严治党',
    FALSE, '定量', '基础性', FALSE, 100.00, '%',
    '李书记', 30, 'PENDING', 35, '廉政建设责任书签订完成，培训覆盖率100%，待审批',
    '[{"id":"audit-503-1","timestamp":"2026-01-06T00:00:00","operator":"li-shuji","operatorName":"李书记","operatorDept":"党委办公室 | 党委统战部","action":"submit","comment":"提交初始进度，廉政责任书签订启动","previousProgress":0,"newProgress":15},{"id":"audit-503-2","timestamp":"2026-01-08T00:00:00","operator":"strategy-admin","operatorName":"战略发展部管理员","operatorDept":"战略发展部","action":"reject","comment":"请补充责任书签订明细"},{"id":"audit-503-3","timestamp":"2026-01-10T00:00:00","operator":"li-shuji","operatorName":"李书记","operatorDept":"党委办公室 | 党委统战部","action":"submit","comment":"补充材料后重新提交，附责任书签订清单","previousProgress":0,"newProgress":18},{"id":"audit-503-4","timestamp":"2026-01-12T00:00:00","operator":"strategy-admin","operatorName":"战略发展部管理员","operatorDept":"战略发展部","action":"approve","comment":"材料完整，审批通过"},{"id":"audit-503-5","timestamp":"2026-01-28T00:00:00","operator":"li-shuji","operatorName":"李书记","operatorDept":"党委办公室 | 党委统战部","action":"submit","comment":"更新进度，廉政培训全覆盖完成，待审批","previousProgress":18,"newProgress":35}]'
) ON CONFLICT (indicator_id) DO UPDATE SET
    progress = EXCLUDED.progress,
    pending_progress = EXCLUDED.pending_progress,
    status_audit = EXCLUDED.status_audit;

-- 2026-506: 意识形态工作责任制落实，舆情处置及时率100%
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit,
    responsible_person, progress, progress_approval_status, pending_progress, pending_remark,
    status_audit
) VALUES (
    4506, 12, NULL, 'STRAT_TO_FUNC', 1, 12,
    '意识形态工作责任制落实，舆情处置及时率100%', 25.00, 3, 2026, 'ACTIVE', '守好意识形态阵地',
    FALSE, '定量', '基础性', FALSE, 100.00, '%',
    '李书记', 22, 'PENDING', 25, '舆情监测机制建立，处置流程完善，待审批',
    '[{"id":"audit-506-1","timestamp":"2026-01-15T00:00:00","operator":"li-shuji","operatorName":"李书记","operatorDept":"党委办公室 | 党委统战部","action":"submit","comment":"提交进度，意识形态责任制落实方案制定完成","previousProgress":0,"newProgress":20},{"id":"audit-506-2","timestamp":"2026-01-18T00:00:00","operator":"strategy-admin","operatorName":"战略发展部管理员","operatorDept":"战略发展部","action":"approve","comment":"审批通过，方案完善"},{"id":"audit-506-3","timestamp":"2026-01-26T00:00:00","operator":"li-shuji","operatorName":"李书记","operatorDept":"党委办公室 | 党委统战部","action":"submit","comment":"更新进度，舆情监测系统上线运行，待审批","previousProgress":20,"newProgress":25}]'
) ON CONFLICT (indicator_id) DO UPDATE SET
    progress = EXCLUDED.progress,
    pending_progress = EXCLUDED.pending_progress,
    status_audit = EXCLUDED.status_audit;


-- ============================================
-- 第七部分：定性指标
-- Requirements: 4.1, 4.2, 7.2
-- ============================================

-- 2026-201: 建立完善校友反馈母校的工作机制
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit,
    responsible_person, progress, status_audit, progress_approval_status
) VALUES (
    4201, 11, NULL, 'STRAT_TO_FUNC', 1, 17,
    '建立完善校友反馈母校的工作机制，择优建立部分地区校友会并开展高质量活动', 25.00, 1, 2026, 'ACTIVE', '中长期发展规划未完成内容',
    TRUE, '定性', '基础性', FALSE, 100.00, '%',
    '陈主任', 8, '[]', 'NONE'
) ON CONFLICT (indicator_id) DO UPDATE SET
    indicator_desc = EXCLUDED.indicator_desc,
    progress = EXCLUDED.progress,
    is_qualitative = EXCLUDED.is_qualitative,
    type1 = EXCLUDED.type1;

-- 2026-201-1: 计算机学院完善校友信息库
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit,
    responsible_person, progress, status_audit, progress_approval_status
) VALUES (
    4211, 11, 4201, 'FUNC_TO_COLLEGE', 17, 23,
    '计算机学院完善校友信息库，建立本学院校友联络机制', 20.00, 1, 2026, 'ACTIVE', '工科学院校友资源丰富',
    TRUE, '定性', '基础性', FALSE, 100.00, '%',
    '赵院长', 10, '[]', 'NONE'
) ON CONFLICT (indicator_id) DO UPDATE SET
    progress = EXCLUDED.progress,
    is_qualitative = EXCLUDED.is_qualitative,
    type1 = EXCLUDED.type1;

-- 2026-201-2: 商学院建立区域校友会
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit,
    responsible_person, progress, status_audit, progress_approval_status
) VALUES (
    4212, 11, 4201, 'FUNC_TO_COLLEGE', 17, 24,
    '商学院建立区域校友会，开展校友返校日活动', 18.00, 2, 2026, 'ACTIVE', '商科校友资源广泛',
    TRUE, '定性', '基础性', FALSE, 100.00, '%',
    '钱院长', 6, '[]', 'NONE'
) ON CONFLICT (indicator_id) DO UPDATE SET
    progress = EXCLUDED.progress,
    is_qualitative = EXCLUDED.is_qualitative,
    type1 = EXCLUDED.type1;

-- 2026-301: 信息化相关数据报送准确、及时、可靠
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit,
    responsible_person, progress, status_audit, progress_approval_status
) VALUES (
    4301, 12, NULL, 'STRAT_TO_FUNC', 1, 9,
    '信息化相关数据报送准确、及时、可靠', 5.00, 1, 2026, 'ACTIVE', '加快提升信息化治理水平',
    TRUE, '定性', '基础性', FALSE, 100.00, '%',
    '刘工', 10, '[]', 'NONE'
) ON CONFLICT (indicator_id) DO UPDATE SET
    indicator_desc = EXCLUDED.indicator_desc,
    progress = EXCLUDED.progress,
    is_qualitative = EXCLUDED.is_qualitative,
    type1 = EXCLUDED.type1;

-- 2026-301-1: 计算机学院按时准确报送教学、科研相关数据
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit,
    responsible_person, progress, status_audit, progress_approval_status
) VALUES (
    4311, 12, 4301, 'FUNC_TO_COLLEGE', 9, 23,
    '计算机学院按时准确报送教学、科研相关数据', 10.00, 1, 2026, 'ACTIVE', '工科学院数据化意识强',
    TRUE, '定性', '基础性', FALSE, 100.00, '%',
    '赵院长', 12, '[]', 'NONE'
) ON CONFLICT (indicator_id) DO UPDATE SET
    progress = EXCLUDED.progress,
    is_qualitative = EXCLUDED.is_qualitative,
    type1 = EXCLUDED.type1;

-- 2026-301-2: 商学院建立数据报送责任人制度
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit,
    responsible_person, progress, status_audit, progress_approval_status
) VALUES (
    4312, 12, 4301, 'FUNC_TO_COLLEGE', 9, 24,
    '商学院建立数据报送责任人制度，确保数据准确', 8.00, 2, 2026, 'ACTIVE', '建立数据报送规范',
    TRUE, '定性', '基础性', FALSE, 100.00, '%',
    '钱院长', 8, '[]', 'NONE'
) ON CONFLICT (indicator_id) DO UPDATE SET
    progress = EXCLUDED.progress,
    is_qualitative = EXCLUDED.is_qualitative,
    type1 = EXCLUDED.type1;

-- 2026-502: 统战工作覆盖率达100%，民主党派联络机制完善
INSERT INTO indicator (
    indicator_id, task_id, parent_indicator_id, level, owner_org_id, target_org_id,
    indicator_desc, weight_percent, sort_order, year, status, remark,
    is_qualitative, type1, type2, can_withdraw, target_value, unit,
    responsible_person, progress, progress_approval_status, pending_progress, pending_remark,
    status_audit
) VALUES (
    4502, 12, NULL, 'STRAT_TO_FUNC', 1, 12,
    '统战工作覆盖率达100%，民主党派联络机制完善', 20.00, 4, 2026, 'ACTIVE', '统一战线工作',
    TRUE, '定性', '基础性', FALSE, 100.00, '%',
    '李书记', 15, 'PENDING', 20, '已完成民主党派联络机制初步建立，待审批',
    '[{"id":"audit-502-1","timestamp":"2026-01-08T00:00:00","operator":"li-shuji","operatorName":"李书记","operatorDept":"党委办公室 | 党委统战部","action":"submit","comment":"提交初始进度，统战工作启动","previousProgress":0,"newProgress":12},{"id":"audit-502-2","timestamp":"2026-01-10T00:00:00","operator":"strategy-admin","operatorName":"战略发展部管理员","operatorDept":"战略发展部","action":"approve","comment":"审批通过"},{"id":"audit-502-3","timestamp":"2026-01-25T00:00:00","operator":"li-shuji","operatorName":"李书记","operatorDept":"党委办公室 | 党委统战部","action":"submit","comment":"更新进度，民主党派联络机制初步建立","previousProgress":12,"newProgress":20}]'
) ON CONFLICT (indicator_id) DO UPDATE SET
    progress = EXCLUDED.progress,
    pending_progress = EXCLUDED.pending_progress,
    is_qualitative = EXCLUDED.is_qualitative,
    type1 = EXCLUDED.type1,
    status_audit = EXCLUDED.status_audit;


-- ============================================
-- 第八部分：里程碑数据
-- Requirements: 7.4
-- 定量指标：季度里程碑（Q1-Q4）
-- 定性指标：季度里程碑（Q1-Q4）
-- ============================================

-- 为指标 4101 (优质就业比例不低于15%) 创建季度里程碑
INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, status, sort_order) VALUES
(4101, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4101, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4101, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4101, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4)
ON CONFLICT DO NOTHING;

-- 为指标 4102 (毕业生就业率不低于95%) 创建季度里程碑
INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, status, sort_order) VALUES
(4102, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4102, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4102, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4102, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4)
ON CONFLICT DO NOTHING;

-- 为指标 4103 (引进优质校招企业) 创建季度里程碑
INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, status, sort_order) VALUES
(4103, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4103, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4103, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4103, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4)
ON CONFLICT DO NOTHING;

-- 为指标 4104 (毕业生创业比例不低于6%) 创建季度里程碑
INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, status, sort_order) VALUES
(4104, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4104, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4104, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4104, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4)
ON CONFLICT DO NOTHING;

-- 为指标 4105 (就业率达92%) 创建季度里程碑
INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, status, sort_order) VALUES
(4105, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4105, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4105, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4105, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4)
ON CONFLICT DO NOTHING;

-- 为二级学院指标创建季度里程碑
-- 4111: 计算机学院优质就业比例
INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, status, sort_order) VALUES
(4111, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4111, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4111, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4111, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4)
ON CONFLICT DO NOTHING;

-- 4112: 商学院优质就业比例
INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, status, sort_order) VALUES
(4112, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4112, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4112, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4112, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4)
ON CONFLICT DO NOTHING;

-- 4113: 艺术与科技学院优质就业比例
INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, status, sort_order) VALUES
(4113, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4113, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4113, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4113, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4)
ON CONFLICT DO NOTHING;

-- 4114: 工学院优质就业比例
INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, status, sort_order) VALUES
(4114, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4114, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4114, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4114, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4)
ON CONFLICT DO NOTHING;

-- 4115: 航空学院优质就业比例
INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, status, sort_order) VALUES
(4115, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4115, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4115, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4115, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4)
ON CONFLICT DO NOTHING;


-- 就业率子指标里程碑
-- 4121: 计算机学院就业率
INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, status, sort_order) VALUES
(4121, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4121, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4121, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4121, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4)
ON CONFLICT DO NOTHING;

-- 4122: 商学院就业率
INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, status, sort_order) VALUES
(4122, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4122, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4122, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4122, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4)
ON CONFLICT DO NOTHING;

-- 4123: 文理学院就业率
INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, status, sort_order) VALUES
(4123, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4123, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4123, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4123, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4)
ON CONFLICT DO NOTHING;

-- 4124: 国际教育学院就业率
INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, status, sort_order) VALUES
(4124, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4124, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4124, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4124, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4)
ON CONFLICT DO NOTHING;

-- 教务处课程优良率指标里程碑
-- 4401: 课程优良率达87%
INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, status, sort_order) VALUES
(4401, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4401, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4401, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4401, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4)
ON CONFLICT DO NOTHING;

-- 4411-4414: 各学院课程优良率
INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, status, sort_order) VALUES
(4411, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4411, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4411, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4411, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4),
(4412, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4412, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4412, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4412, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4),
(4413, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4413, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4413, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4413, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4),
(4414, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4414, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4414, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4414, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4)
ON CONFLICT DO NOTHING;

-- 其他职能部门指标里程碑
-- 4402: 科研论文
INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, status, sort_order) VALUES
(4402, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4402, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4402, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4402, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4)
ON CONFLICT DO NOTHING;

-- 4403: 学生满意度
INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, status, sort_order) VALUES
(4403, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4403, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4403, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4403, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4)
ON CONFLICT DO NOTHING;

-- 4404: 一志愿录取率
INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, status, sort_order) VALUES
(4404, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4404, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4404, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4404, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4)
ON CONFLICT DO NOTHING;

-- 党建相关指标里程碑
-- 4501: 党员发展质量
INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, status, sort_order) VALUES
(4501, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4501, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4501, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4501, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4)
ON CONFLICT DO NOTHING;

-- 4502: 统战工作
INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, status, sort_order) VALUES
(4502, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4502, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4502, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4502, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4)
ON CONFLICT DO NOTHING;

-- 4503: 党风廉政
INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, status, sort_order) VALUES
(4503, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4503, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4503, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4503, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4)
ON CONFLICT DO NOTHING;

-- 4506: 意识形态
INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, status, sort_order) VALUES
(4506, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4506, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4506, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4506, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4)
ON CONFLICT DO NOTHING;

-- 定性指标里程碑
-- 4201: 校友工作机制
INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, status, sort_order) VALUES
(4201, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4201, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4201, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4201, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4)
ON CONFLICT DO NOTHING;

-- 4211, 4212: 校友工作子指标
INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, status, sort_order) VALUES
(4211, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4211, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4211, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4211, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4),
(4212, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4212, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4212, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4212, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4)
ON CONFLICT DO NOTHING;

-- 4301: 信息化数据报送
INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, status, sort_order) VALUES
(4301, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4301, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4301, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4301, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4)
ON CONFLICT DO NOTHING;

-- 4311, 4312: 信息化子指标
INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, status, sort_order) VALUES
(4311, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4311, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4311, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4311, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4),
(4312, 'Q1: 阶段目标', '第一季度目标进度25%', '2026-03-31', 25.00, 'NOT_STARTED', 1),
(4312, 'Q2: 阶段目标', '第二季度目标进度50%', '2026-06-30', 25.00, 'NOT_STARTED', 2),
(4312, 'Q3: 阶段目标', '第三季度目标进度75%', '2026-09-30', 25.00, 'NOT_STARTED', 3),
(4312, 'Q4: 阶段目标', '第四季度目标进度100%', '2026-12-31', 25.00, 'NOT_STARTED', 4)
ON CONFLICT DO NOTHING;


-- ============================================
-- 第九部分：数据验证
-- Requirements: 4.3, 5.3
-- ============================================

-- 重置序列以避免冲突
SELECT setval('indicator_indicator_id_seq', 5000, false);
SELECT setval('milestone_milestone_id_seq', 1000, false);

-- ============================================
-- 验证查询
-- ============================================

-- 1. 验证定量指标数量（应 >= 12）
SELECT '=== 定量指标统计 ===' AS section;
SELECT 
    type1,
    COUNT(*) as count
FROM indicator
WHERE year = 2026 AND indicator_id >= 4100
GROUP BY type1;

-- 2. 验证外键关系 - owner_org_id
SELECT '=== 验证 owner_org_id 外键 ===' AS section;
SELECT 
    i.indicator_id,
    i.indicator_desc,
    i.owner_org_id,
    o.org_name as owner_org_name
FROM indicator i
LEFT JOIN org o ON i.owner_org_id = o.org_id
WHERE i.year = 2026 AND i.indicator_id >= 4100
  AND o.org_id IS NULL;

-- 3. 验证外键关系 - target_org_id
SELECT '=== 验证 target_org_id 外键 ===' AS section;
SELECT 
    i.indicator_id,
    i.indicator_desc,
    i.target_org_id,
    o.org_name as target_org_name
FROM indicator i
LEFT JOIN org o ON i.target_org_id = o.org_id
WHERE i.year = 2026 AND i.indicator_id >= 4100
  AND o.org_id IS NULL;

-- 4. 验证外键关系 - parent_indicator_id
SELECT '=== 验证 parent_indicator_id 外键 ===' AS section;
SELECT 
    i.indicator_id,
    i.indicator_desc,
    i.parent_indicator_id,
    p.indicator_desc as parent_desc
FROM indicator i
LEFT JOIN indicator p ON i.parent_indicator_id = p.indicator_id
WHERE i.year = 2026 AND i.indicator_id >= 4100
  AND i.parent_indicator_id IS NOT NULL
  AND p.indicator_id IS NULL;

-- 5. 验证里程碑数据完整性
SELECT '=== 里程碑统计 ===' AS section;
SELECT 
    i.indicator_id,
    i.indicator_desc,
    COUNT(m.milestone_id) as milestone_count
FROM indicator i
LEFT JOIN milestone m ON i.indicator_id = m.indicator_id
WHERE i.year = 2026 AND i.indicator_id >= 4100
GROUP BY i.indicator_id, i.indicator_desc
HAVING COUNT(m.milestone_id) < 4;

-- 6. 汇总统计
SELECT '=== 2026年数据汇总 ===' AS section;
SELECT 
    '指标总数' as metric,
    COUNT(*) as value
FROM indicator WHERE year = 2026 AND indicator_id >= 4100
UNION ALL
SELECT 
    '定量指标数' as metric,
    COUNT(*) as value
FROM indicator WHERE year = 2026 AND indicator_id >= 4100 AND type1 = '定量'
UNION ALL
SELECT 
    '定性指标数' as metric,
    COUNT(*) as value
FROM indicator WHERE year = 2026 AND indicator_id >= 4100 AND type1 = '定性'
UNION ALL
SELECT 
    '战略级指标数' as metric,
    COUNT(*) as value
FROM indicator WHERE year = 2026 AND indicator_id >= 4100 AND parent_indicator_id IS NULL
UNION ALL
SELECT 
    '二级学院指标数' as metric,
    COUNT(*) as value
FROM indicator WHERE year = 2026 AND indicator_id >= 4100 AND parent_indicator_id IS NOT NULL
UNION ALL
SELECT 
    '里程碑总数' as metric,
    COUNT(*) as value
FROM milestone m
JOIN indicator i ON m.indicator_id = i.indicator_id
WHERE i.year = 2026 AND i.indicator_id >= 4100;

-- ============================================
-- 完成
-- ============================================
-- 2026年指标种子数据脚本执行完成
-- 版本: V1.0
-- 日期: 2026-01-19

