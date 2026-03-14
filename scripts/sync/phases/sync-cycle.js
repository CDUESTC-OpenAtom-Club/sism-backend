/**
 * 考核周期同步阶段
 * 创建缺失的年度考核周期
 * 
 * Requirements: 1.1, 1.4
 */

const YEARS = [2023, 2024, 2025, 2026];

/**
 * 执行考核周期同步
 * @param {import('../sync-context.js').SyncContext} ctx - 同步上下文
 * @returns {Promise<import('../sync-context.js').PhaseResult>}
 */
export async function syncCycle(ctx) {
  const client = await ctx.getClient();
  
  try {
    console.log('正在同步考核周期...');
    
    // 1. 查询现有周期
    const existingCycles = await client.query('SELECT cycle_id, year FROM assessment_cycle');
    const existingYears = new Set(existingCycles.rows.map(r => r.year));
    
    // 建立现有周期的 ID 映射
    existingCycles.rows.forEach(cycle => {
      ctx.maps.cycle.set(cycle.year, cycle.cycle_id);
    });
    
    console.log(`数据库现有周期年份: ${Array.from(existingYears).join(', ')}`);
    
    // 2. 开始事务
    await client.query('BEGIN');
    
    // 3. 创建缺失的年度周期
    for (const year of YEARS) {
      if (!existingYears.has(year)) {
        const result = await client.query(
          `INSERT INTO assessment_cycle (cycle_name, year, start_date, end_date, description) 
           VALUES ($1, $2, $3, $4, $5) RETURNING cycle_id`,
          [
            `${year}年度考核周期`,
            year,
            `${year}-01-01`,
            `${year}-12-31`,
            `${year}年度战略指标考核周期`
          ]
        );
        ctx.maps.cycle.set(year, result.rows[0].cycle_id);
        ctx.recordInsert('cycle');
        console.log(`✅ 新增: ${year}年度考核周期`);
      } else {
        ctx.recordSkip('cycle');
      }
    }
    
    // 4. 提交事务
    await client.query('COMMIT');
    
    // 5. 输出映射信息
    console.log('\n年份 → cycle_id 映射:');
    ctx.maps.cycle.forEach((cycleId, year) => {
      console.log(`  ${year} → ${cycleId}`);
    });
    
    return {
      success: true,
      inserted: ctx.getPhaseStats('cycle').inserted,
      skipped: ctx.getPhaseStats('cycle').skipped
    };
    
  } catch (err) {
    await client.query('ROLLBACK');
    ctx.recordError('cycle');
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

export default { syncCycle };
