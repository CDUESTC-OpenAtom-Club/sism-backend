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

    const result = await client.query(`
      SELECT 
        COUNT(*) FILTER (WHERE parent_indicator_id IS NULL) as parent_count,
        COUNT(*) FILTER (WHERE parent_indicator_id IS NOT NULL) as child_count,
        COUNT(*) as total
      FROM indicator
      WHERE year = 2026 AND is_deleted = false
    `);

    console.log('2026 年指标结构:');
    console.log(`  父指标: ${result.rows[0].parent_count}`);
    console.log(`  子指标: ${result.rows[0].child_count}`);
    console.log(`  总计: ${result.rows[0].total}`);

    // 检查是否有子指标
    if (result.rows[0].child_count > 0) {
      const childSample = await client.query(`
        SELECT 
          i.id,
          i.indicator_desc,
          i.parent_indicator_id,
          p.indicator_desc as parent_desc
        FROM indicator i
        JOIN indicator p ON i.parent_indicator_id = p.id
        WHERE i.year = 2026 AND i.is_deleted = false
        LIMIT 5
      `);

      console.log('\n子指标示例:');
      childSample.rows.forEach(row => {
        console.log(`  [${row.id}] ${row.indicator_desc.substring(0, 40)}...`);
        console.log(`    父指标: [${row.parent_indicator_id}] ${row.parent_desc.substring(0, 40)}...\n`);
      });
    }

  } catch (error) {
    console.error('错误:', error.message);
  } finally {
    await client.end();
  }
}

check();
