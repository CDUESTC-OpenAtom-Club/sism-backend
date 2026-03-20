/**
 * 战略任务同步阶段
 *
 * 注意：
 * - 当前业务主模型已经迁移到 sys_task.name / desc。
 * - 该历史同步脚本缺少 plan_id 等现代必填上下文，不能再安全地自动新增任务。
 * - 现阶段仅保留“读取既有 sys_task 并建立映射”的兼容能力。
 * - 如需重建干净任务数据，请使用 database/seeds 中的种子链路。
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
    const existingTasks = await client.query(`
      SELECT task_id, name, cycle_id
      FROM sys_task
      WHERE COALESCE(is_deleted, false) = false
    `);
    const existingSet = new Set(existingTasks.rows.map(t => `${t.name}_${t.cycle_id}`));
    
    // 建立现有任务的 ID 映射
    existingTasks.rows.forEach(task => {
      ctx.maps.task.set(`${task.name}_${task.cycle_id}`, task.task_id);
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
        throw new Error(
          [
            'sync-task.js 已停用自动新增能力：当前 sys_task 需要 plan_id 等现代业务上下文。',
            '请改用 database/seeds/sys_task-data.sql 或 database/seeds/reset-and-load-clean-seeds.sql 维护干净任务数据。'
          ].join(' ')
        );
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
