const { Client } = require('pg');

async function analyze() {
  const client = new Client({
    host: '175.24.139.148',
    port: 8386,
    database: 'strategic',
    user: 'postgres',
    password: '64378561huaW'
  });

  try {
    await client.connect();

    // 检查 2026 年指标关联的任务
    const result = await client.query(`
      SELECT 
        t.task_id,
        t.name,
        t.cycle_id,
        COUNT(i.id) FILTER (WHERE i.parent_indicator_id IS NULL) as parent_count,
        COUNT(i.id) FILTER (WHERE i.parent_indicator_id IS NOT NULL) as child_count,
        COUNT(i.id) as total_count
      FROM sys_task t
      LEFT JOIN indicator i ON i.task_id = t.task_id AND i.year = 2026 AND i.is_deleted = false
      WHERE EXISTS (
        SELECT 1 FROM indicator WHERE task_id = t.task_id AND year = 2026
      )
      GROUP BY t.task_id, t.name, t.cycle_id
      ORDER BY t.cycle_id, t.task_id
    `);

    console.log('2026 年指标所属任务:');
    console.log('='.repeat(80));
    result.rows.forEach(row => {
      console.log(`任务 [${row.task_id}] ${row.name}`);
      console.log(`  Cycle ID: ${row.cycle_id}`);
      console.log(`  父指标: ${row.parent_count}, 子指标: ${row.child_count}, 总计: ${row.total_count}\n`);
    });

    // 统计不同 cycle_id 的指标数量
    const cycleStats = await client.query(`
      SELECT 
        t.cycle_id,
        COUNT(DISTINCT t.task_id) as task_count,
        COUNT(i.id) FILTER (WHERE i.parent_indicator_id IS NULL) as parent_count,
        COUNT(i.id) FILTER (WHERE i.parent_indicator_id IS NOT NULL) as child_count
      FROM sys_task t
      LEFT JOIN indicator i ON i.task_id = t.task_id AND i.year = 2026 AND i.is_deleted = false
      WHERE EXISTS (
        SELECT 1 FROM indicator WHERE task_id = t.task_id AND year = 2026
      )
      GROUP BY t.cycle_id
      ORDER BY t.cycle_id
    `);

    console.log('按 Cycle ID 统计:');
    console.log('='.repeat(80));
    cycleStats.rows.forEach(row => {
      console.log(`Cycle ID ${row.cycle_id}:`);
      console.log(`  任务数: ${row.task_count}`);
      console.log(`  父指标: ${row.parent_count}, 子指标: ${row.child_count}\n`);
    });

  } catch (error) {
    console.error('错误:', error.message);
  } finally {
    await client.end();
  }
}

analyze();
