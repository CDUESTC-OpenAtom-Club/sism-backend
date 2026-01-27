/**
 * 战略任务同步阶段
 * 将前端定义的战略任务同步到数据库
 * 
 * Requirements: 1.1, 1.4
 */

// 前端定义的战略任务
const TASKS = [
  {
    frontendId: '1',
    title: '全力促进毕业生多元化高质量就业创业',
    desc: '围绕毕业生就业质量提升，多措并举促进高质量就业创业',
    type: 'DEVELOPMENT',
    year: 2025
  },
  {
    frontendId: '2',
    title: '推进校友工作提质增效，赋能校友成长',
    desc: '建立完善校友工作机制，提升校友服务质量',
    type: 'BASIC',
    year: 2025
  },
  {
    frontendId: '3',
    title: '根据学校整体部署',
    desc: '按照学校整体战略部署推进信息化建设等相关工作',
    type: 'BASIC',
    year: 2025
  }
];

// 2026年度任务
const TASKS_2026 = [
  {
    frontendId: '2026-1',
    title: '全力促进毕业生多元化高质量就业创业',
    desc: '围绕毕业生就业质量提升，多措并举促进高质量就业创业',
    type: 'DEVELOPMENT',
    year: 2026
  },
  {
    frontendId: '2026-2',
    title: '推进校友工作提质增效，赋能校友成长',
    desc: '建立完善校友工作机制，提升校友服务质量',
    type: 'BASIC',
    year: 2026
  },
  {
    frontendId: '2026-3',
    title: '根据学校整体部署',
    desc: '按照学校整体战略部署推进信息化建设等相关工作',
    type: 'BASIC',
    year: 2026
  }
];

/**
 * 执行战略任务同步
 * @param {import('../sync-context.js').SyncContext} ctx - 同步上下文
 * @returns {Promise<import('../sync-context.js').PhaseResult>}
 */
export async function syncTask(ctx) {
  const client = await ctx.getClient();
  
  try {
    console.log('正在同步战略任务...');
    
    // 1. 获取战略发展部 org_id
    const strategyOrgId = ctx.maps.org.get('战略发展部');
    if (!strategyOrgId) {
      throw new Error('战略发展部不存在，请先执行组织机构同步');
    }
    console.log(`战略发展部 org_id: ${strategyOrgId}`);
    
    // 2. 查询现有任务
    const existingTasks = await client.query('SELECT task_id, task_name, cycle_id FROM strategic_task');
    const existingSet = new Set(existingTasks.rows.map(t => `${t.task_name}_${t.cycle_id}`));
    
    // 建立现有任务的 ID 映射
    existingTasks.rows.forEach(task => {
      ctx.maps.task.set(`${task.task_name}_${task.cycle_id}`, task.task_id);
    });
    
    console.log(`数据库现有任务: ${existingTasks.rows.length} 个`);
    
    // 3. 开始事务
    await client.query('BEGIN');
    
    // 4. 同步所有任务
    const allTasks = [...TASKS, ...TASKS_2026];
    
    for (const task of allTasks) {
      const cycleId = ctx.maps.cycle.get(task.year);
      if (!cycleId) {
        console.log(`⚠️ 跳过: ${task.title} (周期 ${task.year} 不存在)`);
        ctx.recordSkip('task');
        continue;
      }
      
      const key = `${task.title}_${cycleId}`;
      
      if (!existingSet.has(key)) {
        const result = await client.query(
          `INSERT INTO strategic_task (cycle_id, task_name, task_desc, task_type, org_id, created_by_org_id, sort_order) 
           VALUES ($1, $2, $3, $4, $5, $6, $7)
           RETURNING task_id`,
          [cycleId, task.title, task.desc, task.type, strategyOrgId, strategyOrgId, ctx.getPhaseStats('task').inserted + 10]
        );
        ctx.maps.task.set(key, result.rows[0].task_id);
        ctx.recordInsert('task');
        console.log(`✅ 新增: ${task.title} (${task.year})`);
      } else {
        ctx.recordSkip('task');
      }
    }
    
    // 5. 提交事务
    await client.query('COMMIT');
    
    // 6. 输出映射信息
    console.log(`\n战略任务 ID 映射已建立: ${ctx.maps.task.size} 个`);
    
    return {
      success: true,
      inserted: ctx.getPhaseStats('task').inserted,
      skipped: ctx.getPhaseStats('task').skipped
    };
    
  } catch (err) {
    await client.query('ROLLBACK');
    ctx.recordError('task');
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

export default { syncTask };
