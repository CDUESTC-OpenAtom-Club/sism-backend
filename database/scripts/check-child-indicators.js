const { Client } = require('pg');

async function check() {
  const client = new Client({
    host: '175.24.139.148',
    port: 8386,
    database: 'strategic',
    user: 'postgres',
    password: '64378561huaW'
  });

  try {
    await client.connect();

    // 检查 2025 年子指标
    const result2025 = await client.query(`
      SELECT COUNT(*) as count
      FROM indicator
      WHERE year = 2025 AND parent_indicator_id IS NOT NULL
    `);
    console.log('2025 年子指标数:', result2025.rows[0].count);

    // 检查 2026 年子指标
    const result2026 = await client.query(`
      SELECT COUNT(*) as count
      FROM indicator
      WHERE year = 2026 AND parent_indicator_id IS NOT NULL
    `);
    console.log('2026 年子指标数:', result2026.rows[0].count);

    // 检查映射表是否正确
    const mappingCheck = await client.query(`
      SELECT 
        COUNT(DISTINCT old_ind.id) as old_parent_count,
        COUNT(DISTINCT new_ind.id) as new_parent_count
      FROM indicator old_ind
      JOIN indicator new_ind ON 
        REPLACE(old_ind.indicator_desc, '2026', '2025') = new_ind.indicator_desc
        AND old_ind.year = 2026
        AND new_ind.year = 2025
        AND old_ind.parent_indicator_id IS NULL
        AND new_ind.parent_indicator_id IS NULL
    `);
    console.log('\n父指标映射:');
    console.log('  2026 父指标:', mappingCheck.rows[0].old_parent_count);
    console.log('  2025 父指标:', mappingCheck.rows[0].new_parent_count);

    // 检查为什么子指标没有被插入
    const childCheck = await client.query(`
      SELECT 
        i.id,
        i.indicator_desc,
        i.parent_indicator_id,
        p.indicator_desc as parent_desc
      FROM indicator i
      JOIN indicator p ON i.parent_indicator_id = p.id
      WHERE i.year = 2026
      LIMIT 3
    `);

    console.log('\n2026 年子指标示例:');
    childCheck.rows.forEach(row => {
      console.log(`  子指标: ${row.indicator_desc.substring(0, 40)}`);
      console.log(`  父指标: ${row.parent_desc.substring(0, 40)}\n`);
    });

  } catch (error) {
    console.error('错误:', error.message);
  } finally {
    await client.end();
  }
}

check();
