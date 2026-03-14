const { Client } = require('pg');

async function verify() {
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

    // 检查任务
    const taskResult = await client.query(`
      SELECT 
        t.task_id,
        t.task_name,
        t.cycle_id,
        c.year,
        c.cycle_name,
        COUNT(i.id) as indicator_count
      FROM sys_task t
      LEFT JOIN cycle c ON t.cycle_id = c.id
      LEFT JOIN indicator i ON i.task_id = t.task_id AND i.year = 2025
      WHERE t.cycle_id = 7
      GROUP BY t.task_id, t.task_name, t.cycle_id, c.year, c.cycle_name
      ORDER BY t.task_id
    `);

    console.log('📋 2025 年任务列表:');
    console.log('='.repeat(80));
    taskResult.rows.forEach(row => {
      console.log(`任务 [${row.task_id}] ${row.task_name}`);
      console.log(`  Cycle: [${row.cycle_id}] ${row.cycle_name} (${row.year}年)`);
      console.log(`  关联指标: ${row.indicator_count} 个\n`);
    });

    // 检查指标统计
    const indicatorStats = await client.query(`
      SELECT 
        year,
        COUNT(*) as total,
        COUNT(CASE WHEN parent_indicator_id IS NULL THEN 1 END) as parent_count,
        COUNT(CASE WHEN parent_indicator_id IS NOT NULL THEN 1 END) as child_count,
        ROUND(AVG(progress)) as avg_progress,
        MIN(progress) as min_progress,
        MAX(progress) as max_progress
      FROM indicator
      WHERE is_deleted = false
      GROUP BY year
      ORDER BY year DESC
    `);

    console.log('📊 指标统计:');
    console.log('='.repeat(80));
    indicatorStats.rows.forEach(row => {
      console.log(`${row.year || 'NULL'} 年:`);
      console.log(`  总数: ${row.total} 个 (父指标: ${row.parent_count}, 子指标: ${row.child_count})`);
      console.log(`  进度: 平均 ${row.avg_progress}%, 范围 ${row.min_progress}%-${row.max_progress}%\n`);
    });

    // 检查里程碑
    const milestoneStats = await client.query(`
      SELECT 
        i.year,
        COUNT(m.id) as total,
        COUNT(CASE WHEN m.status = 'COMPLETED' THEN 1 END) as completed,
        COUNT(CASE WHEN m.status = 'IN_PROGRESS' THEN 1 END) as in_progress,
        COUNT(CASE WHEN m.status = 'NOT_STARTED' THEN 1 END) as not_started
      FROM indicator_milestone m
      JOIN indicator i ON m.indicator_id = i.id
      WHERE i.is_deleted = false
      GROUP BY i.year
      ORDER BY i.year DESC
    `);

    console.log('🎯 里程碑统计:');
    console.log('='.repeat(80));
    milestoneStats.rows.forEach(row => {
      console.log(`${row.year || 'NULL'} 年: ${row.total} 个`);
      console.log(`  已完成: ${row.completed}, 进行中: ${row.in_progress}, 未开始: ${row.not_started}\n`);
    });

    console.log('✅ 验证完成！');

  } catch (error) {
    console.error('❌ 错误:', error.message);
  } finally {
    await client.end();
  }
}

verify();
