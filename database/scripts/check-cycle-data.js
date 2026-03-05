const { Client } = require('pg');

async function checkCycle() {
  const client = new Client({
    host: '175.24.139.148',
    port: 8386,
    database: 'strategic',
    user: 'postgres',
    password: '64378561huaW'
  });

  try {
    await client.connect();
    console.log('✅ 数据库连接成功\n');

    // 查看 cycle 数据
    console.log('📊 cycle 表数据:');
    console.log('='.repeat(80));
    const cycles = await client.query('SELECT * FROM cycle ORDER BY id');
    
    cycles.rows.forEach(cycle => {
      console.log(`Cycle ID: ${cycle.id}`);
      console.log(`  名称: ${cycle.cycle_name}`);
      console.log(`  年份: ${cycle.year}`);
      console.log(`  开始日期: ${cycle.start_date}`);
      console.log(`  结束日期: ${cycle.end_date}`);
      console.log(`  描述: ${cycle.description || '无'}`);
      console.log(`  创建时间: ${cycle.created_at}`);
      console.log('');
    });

    // 查看任务与 cycle 的关联
    console.log('📊 任务与 Cycle 的关联:');
    console.log('='.repeat(80));
    const taskCycle = await client.query(`
      SELECT 
        t.task_id,
        t.task_name,
        t.cycle_id,
        c.cycle_name,
        c.year as cycle_year,
        COUNT(i.indicator_id) as indicator_count
      FROM sys_task t
      LEFT JOIN cycle c ON c.id = t.cycle_id
      LEFT JOIN indicator i ON i.task_id = t.task_id AND (i.is_deleted = false OR i.is_deleted IS NULL)
      GROUP BY t.task_id, t.task_name, t.cycle_id, c.cycle_name, c.year
      ORDER BY t.task_id
    `);
    
    taskCycle.rows.forEach(row => {
      console.log(`任务 [${row.task_id}] ${row.task_name}`);
      console.log(`  Cycle: [${row.cycle_id}] ${row.cycle_name} (${row.cycle_year}年)`);
      console.log(`  关联指标: ${row.indicator_count} 个`);
      console.log('');
    });

    // 分析结论
    console.log('='.repeat(80));
    console.log('📝 分析结论:');
    console.log('='.repeat(80));
    console.log('1. sys_task 表没有直接的 year 字段');
    console.log('2. sys_task 通过 cycle_id 关联到 cycle 表');
    console.log('3. cycle 表有 year 字段，表示周期的年份');
    console.log('4. 因此，任务的年份 = cycle.year (通过 cycle_id 关联)');
    console.log('');
    console.log('💡 解决方案:');
    console.log('  选项 1: 在 sys_task 表添加 year 字段（冗余但查询快）');
    console.log('  选项 2: 保持现状，通过 JOIN cycle 表获取年份');
    console.log('  选项 3: 创建 2025 年的 cycle，然后创建关联的任务');

  } catch (error) {
    console.error('❌ 错误:', error.message);
  } finally {
    await client.end();
  }
}

checkCycle();
