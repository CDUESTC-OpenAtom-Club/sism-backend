/**
 * 指标同步阶段
 * 将前端定义的指标同步到数据库
 * 
 * Requirements: 1.1, 1.4
 */

// 2026年度指标数据
const INDICATORS_2026 = [
  // 战略任务1：全力促进毕业生多元化高质量就业创业 - 职能部门指标
  { id: '2026-101', name: '优质就业比例不低于15%', weight: 20, targetValue: 15, unit: '%', responsibleDept: '就业创业指导中心', isStrategic: true, ownerDept: '战略发展部', taskContent: '全力促进毕业生多元化高质量就业创业', year: 2026 },
  { id: '2026-102', name: '毕业生就业率不低于95%', weight: 20, targetValue: 95, unit: '%', responsibleDept: '就业创业指导中心', isStrategic: true, ownerDept: '战略发展部', taskContent: '全力促进毕业生多元化高质量就业创业', year: 2026 },
  { id: '2026-103', name: '针对各学院开设专业引进优质校招企业（各专业大类不低于3家）', weight: 20, targetValue: 3, unit: '家/专业', responsibleDept: '招生工作处', isStrategic: true, ownerDept: '战略发展部', taskContent: '全力促进毕业生多元化高质量就业创业', year: 2026 },
  { id: '2026-104', name: '毕业生创业比例不低于6%', weight: 20, targetValue: 6, unit: '%', responsibleDept: '就业创业指导中心', isStrategic: true, ownerDept: '战略发展部', taskContent: '全力促进毕业生多元化高质量就业创业', year: 2026 },
  { id: '2026-105', name: '就业率达92%，教育厅公布的就业数据排名川内同类民办院校前三', weight: 50, targetValue: 92, unit: '%', responsibleDept: '就业创业指导中心', isStrategic: true, ownerDept: '战略发展部', taskContent: '全力促进毕业生多元化高质量就业创业', year: 2026 },
  
  // 二级学院子指标
  { id: '2026-101-1', name: '计算机学院优质就业比例不低于18%', weight: 25, targetValue: 18, unit: '%', responsibleDept: '计算机学院', isStrategic: false, ownerDept: '就业创业指导中心', parentIndicatorId: '2026-101', taskContent: '全力促进毕业生多元化高质量就业创业', year: 2026 },
  { id: '2026-101-2', name: '商学院优质就业比例不低于12%', weight: 20, targetValue: 12, unit: '%', responsibleDept: '商学院', isStrategic: false, ownerDept: '就业创业指导中心', parentIndicatorId: '2026-101', taskContent: '全力促进毕业生多元化高质量就业创业', year: 2026 },
  { id: '2026-101-3', name: '艺术与科技学院优质就业比例不低于10%', weight: 15, targetValue: 10, unit: '%', responsibleDept: '艺术与科技学院', isStrategic: false, ownerDept: '就业创业指导中心', parentIndicatorId: '2026-101', taskContent: '全力促进毕业生多元化高质量就业创业', year: 2026 },
  { id: '2026-101-4', name: '工学院优质就业比例不低于16%', weight: 22, targetValue: 16, unit: '%', responsibleDept: '工学院', isStrategic: false, ownerDept: '就业创业指导中心', parentIndicatorId: '2026-101', taskContent: '全力促进毕业生多元化高质量就业创业', year: 2026 },
  { id: '2026-101-5', name: '航空学院优质就业比例不低于17%', weight: 25, targetValue: 17, unit: '%', responsibleDept: '航空学院', isStrategic: false, ownerDept: '就业创业指导中心', parentIndicatorId: '2026-101', taskContent: '全力促进毕业生多元化高质量就业创业', year: 2026 },
  
  // 战略任务2：推进校友工作提质增效
  { id: '2026-201', name: '建立完善校友反馈母校的工作机制，择优建立部分地区校友会并开展高质量活动', weight: 25, targetValue: 100, unit: '%', responsibleDept: '学校综合办公室', isStrategic: true, ownerDept: '战略发展部', taskContent: '推进校友工作提质增效，赋能校友成长', year: 2026 },
  { id: '2026-201-1', name: '计算机学院完善校友信息库，建立本学院校友联络机制', weight: 20, targetValue: 100, unit: '%', responsibleDept: '计算机学院', isStrategic: false, ownerDept: '学校综合办公室', parentIndicatorId: '2026-201', taskContent: '推进校友工作提质增效，赋能校友成长', year: 2026 },
  { id: '2026-201-2', name: '商学院建立区域校友会，开展校友返校日活动', weight: 18, targetValue: 100, unit: '%', responsibleDept: '商学院', isStrategic: false, ownerDept: '学校综合办公室', parentIndicatorId: '2026-201', taskContent: '推进校友工作提质增效，赋能校友成长', year: 2026 },
  
  // 战略任务3：根据学校整体部署
  { id: '2026-301', name: '信息化相关数据报送准确、及时、可靠', weight: 5, targetValue: 100, unit: '%', responsibleDept: '数字校园建设办公室', isStrategic: true, ownerDept: '战略发展部', taskContent: '根据学校整体部署', year: 2026 },
  { id: '2026-301-1', name: '计算机学院按时准确报送教学、科研相关数据', weight: 10, targetValue: 100, unit: '%', responsibleDept: '计算机学院', isStrategic: false, ownerDept: '数字校园建设办公室', parentIndicatorId: '2026-301', taskContent: '根据学校整体部署', year: 2026 },
  { id: '2026-301-2', name: '商学院建立数据报送责任人制度，确保数据准确', weight: 8, targetValue: 100, unit: '%', responsibleDept: '商学院', isStrategic: false, ownerDept: '数字校园建设办公室', parentIndicatorId: '2026-301', taskContent: '根据学校整体部署', year: 2026 },
];

/**
 * 执行指标同步
 * @param {import('../sync-context.js').SyncContext} ctx - 同步上下文
 * @returns {Promise<import('../sync-context.js').PhaseResult>}
 */
export async function syncIndicator(ctx) {
  const client = await ctx.getClient();
  
  try {
    console.log('正在同步指标...');
    
    // 1. 查询现有指标
    const existingIndicators = await client.query('SELECT indicator_id, indicator_desc, year FROM indicator WHERE year = 2026');
    const existingSet = new Set(existingIndicators.rows.map(i => i.indicator_desc));
    
    console.log(`数据库现有 2026 年度指标: ${existingIndicators.rows.length} 个`);
    
    // 2. 分离战略级指标和子指标
    const strategicIndicators = INDICATORS_2026.filter(i => i.isStrategic);
    const childIndicators = INDICATORS_2026.filter(i => !i.isStrategic);
    
    console.log(`战略级指标: ${strategicIndicators.length} 个`);
    console.log(`子指标: ${childIndicators.length} 个`);
    
    // 3. 开始事务
    await client.query('BEGIN');
    
    // 4. 先同步战略级指标
    console.log('\n同步战略级指标...');
    for (const ind of strategicIndicators) {
      if (existingSet.has(ind.name)) {
        ctx.recordSkip('indicator');
        continue;
      }
      
      const ownerOrgId = ctx.maps.org.get(ind.ownerDept);
      const targetOrgId = ctx.maps.org.get(ind.responsibleDept);
      const taskKey = `${ind.taskContent}_${ctx.maps.cycle.get(ind.year)}`;
      const taskId = ctx.maps.task.get(taskKey);
      
      if (!ownerOrgId || !targetOrgId || !taskId) {
        console.log(`⚠️ 跳过: ${ind.name.substring(0, 30)}... (映射缺失)`);
        ctx.recordSkip('indicator');
        continue;
      }
      
      const result = await client.query(
        `INSERT INTO indicator (
          task_id, indicator_desc, level, weight_percent, 
          status, year, owner_org_id, target_org_id, sort_order, remark
        ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10)
        RETURNING indicator_id`,
        [
          taskId,
          ind.name,
          'STRAT_TO_FUNC',
          ind.weight,
          'ACTIVE',
          ind.year,
          ownerOrgId,
          targetOrgId,
          ctx.getPhaseStats('indicator').inserted + 1,
          `目标值: ${ind.targetValue}${ind.unit}`
        ]
      );
      
      ctx.maps.indicator.set(ind.id, result.rows[0].indicator_id);
      ctx.recordInsert('indicator');
      console.log(`✅ 新增: [${result.rows[0].indicator_id}] ${ind.name.substring(0, 40)}...`);
    }
    
    // 5. 再同步子指标
    console.log('\n同步子指标...');
    for (const ind of childIndicators) {
      if (existingSet.has(ind.name)) {
        ctx.recordSkip('indicator');
        continue;
      }
      
      const ownerOrgId = ctx.maps.org.get(ind.ownerDept);
      const targetOrgId = ctx.maps.org.get(ind.responsibleDept);
      const taskKey = `${ind.taskContent}_${ctx.maps.cycle.get(ind.year)}`;
      const taskId = ctx.maps.task.get(taskKey);
      const parentIndicatorId = ctx.maps.indicator.get(ind.parentIndicatorId);
      
      if (!ownerOrgId || !targetOrgId || !taskId) {
        console.log(`⚠️ 跳过: ${ind.name.substring(0, 30)}... (映射缺失)`);
        ctx.recordSkip('indicator');
        continue;
      }
      
      if (!parentIndicatorId) {
        console.log(`⚠️ 跳过: ${ind.name.substring(0, 30)}... (父指标未找到)`);
        ctx.recordSkip('indicator');
        continue;
      }
      
      const result = await client.query(
        `INSERT INTO indicator (
          task_id, indicator_desc, level, weight_percent, 
          status, year, owner_org_id, target_org_id, parent_indicator_id, sort_order, remark
        ) VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9, $10, $11)
        RETURNING indicator_id`,
        [
          taskId,
          ind.name,
          'FUNC_TO_COLLEGE',
          ind.weight,
          'ACTIVE',
          ind.year,
          ownerOrgId,
          targetOrgId,
          parentIndicatorId,
          ctx.getPhaseStats('indicator').inserted + 1,
          `目标值: ${ind.targetValue}${ind.unit}`
        ]
      );
      
      ctx.maps.indicator.set(ind.id, result.rows[0].indicator_id);
      ctx.recordInsert('indicator');
      console.log(`✅ 新增: [${result.rows[0].indicator_id}] ${ind.name.substring(0, 40)}...`);
    }
    
    // 6. 提交事务
    await client.query('COMMIT');
    
    // 7. 输出映射信息
    console.log(`\n指标 ID 映射已建立: ${ctx.maps.indicator.size} 个`);
    
    return {
      success: true,
      inserted: ctx.getPhaseStats('indicator').inserted,
      skipped: ctx.getPhaseStats('indicator').skipped
    };
    
  } catch (err) {
    await client.query('ROLLBACK');
    ctx.recordError('indicator');
    return {
      success: false,
      inserted: 0,
      skipped: 0,
      error: err
    };
  } finally {
    client.release();
  }
}

export default { syncIndicator };
