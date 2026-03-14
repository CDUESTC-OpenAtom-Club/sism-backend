/**
 * 里程碑同步阶段
 * 为每个指标生成季度里程碑
 * 
 * Requirements: 1.1, 1.4
 */

/**
 * 生成季度里程碑模板
 * @param {number} year - 年份
 * @returns {Array<{name: string, desc: string, dueDate: string, weight: number, status: string}>}
 */
function generateQuarterlyMilestones(year) {
  return [
    { name: 'Q1里程碑', desc: `${year}年第一季度目标`, dueDate: `${year}-03-31`, weight: 25, status: 'IN_PROGRESS' },
    { name: 'Q2里程碑', desc: `${year}年第二季度目标`, dueDate: `${year}-06-30`, weight: 25, status: 'NOT_STARTED' },
    { name: 'Q3里程碑', desc: `${year}年第三季度目标`, dueDate: `${year}-09-30`, weight: 25, status: 'NOT_STARTED' },
    { name: 'Q4里程碑', desc: `${year}年第四季度目标`, dueDate: `${year}-12-31`, weight: 25, status: 'NOT_STARTED' }
  ];
}

/**
 * 执行里程碑同步
 * @param {import('../sync-context.js').SyncContext} ctx - 同步上下文
 * @returns {Promise<import('../sync-context.js').PhaseResult>}
 */
export async function syncMilestone(ctx) {
  const client = await ctx.getClient();
  
  try {
    console.log('正在同步里程碑...');
    
    // 1. 获取 2026 年度所有指标
    const indicators = await client.query('SELECT indicator_id FROM indicator WHERE year = 2026');
    console.log(`2026年度指标数: ${indicators.rows.length} 个`);
    
    // 2. 检查现有里程碑
    const existingMilestones = await client.query(`
      SELECT m.indicator_id, COUNT(*) as count 
      FROM milestone m 
      JOIN indicator i ON m.indicator_id = i.indicator_id 
      WHERE i.year = 2026 
      GROUP BY m.indicator_id
    `);
    const existingSet = new Set(existingMilestones.rows.map(r => r.indicator_id));
    console.log(`已有里程碑的指标数: ${existingSet.size} 个`);
    
    // 3. 开始事务
    await client.query('BEGIN');
    
    // 4. 为每个指标创建季度里程碑
    for (const row of indicators.rows) {
      const indicatorId = parseInt(row.indicator_id);
      
      if (existingSet.has(row.indicator_id)) {
        ctx.recordSkip('milestone');
        continue;
      }
      
      const milestones = generateQuarterlyMilestones(2026);
      
      for (let i = 0; i < milestones.length; i++) {
        const m = milestones[i];
        await client.query(
          `INSERT INTO milestone (indicator_id, milestone_name, milestone_desc, due_date, weight_percent, status, sort_order)
           VALUES ($1, $2, $3, $4, $5, $6, $7)`,
          [indicatorId, m.name, m.desc, m.dueDate, m.weight, m.status, i + 1]
        );
        ctx.recordInsert('milestone');
      }
      
      console.log(`✅ 指标 [${indicatorId}] 创建 4 个里程碑`);
    }
    
    // 5. 提交事务
    await client.query('COMMIT');
    
    // 6. 验证结果
    const totalMilestones = await client.query(`
      SELECT COUNT(*) as count FROM milestone m 
      JOIN indicator i ON m.indicator_id = i.indicator_id 
      WHERE i.year = 2026
    `);
    
    console.log(`\n2026年度里程碑总数: ${totalMilestones.rows[0].count} 个`);
    
    return {
      success: true,
      inserted: ctx.getPhaseStats('milestone').inserted,
      skipped: ctx.getPhaseStats('milestone').skipped
    };
    
  } catch (err) {
    await client.query('ROLLBACK');
    ctx.recordError('milestone');
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

export default { syncMilestone };
