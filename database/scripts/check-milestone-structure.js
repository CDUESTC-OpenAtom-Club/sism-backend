const { Client } = require('pg');

async function checkMilestone() {
  const client = new Client({
    host: '175.24.139.148',
    port: 8386,
    database: 'strategic',
    user: 'postgres',
    password: '64378561huaW'
  });

  try {
    await client.connect();
    
    console.log('📋 indicator_milestone 表结构:');
    console.log('='.repeat(60));
    const columns = await client.query(`
      SELECT column_name, data_type, is_nullable
      FROM information_schema.columns
      WHERE table_name = 'indicator_milestone'
      ORDER BY ordinal_position
    `);
    columns.rows.forEach(row => {
      console.log(`  ${row.column_name.padEnd(30)} ${row.data_type.padEnd(20)} ${row.is_nullable === 'YES' ? 'NULL' : 'NOT NULL'}`);
    });
    
    // 检查数据量
    const count = await client.query('SELECT COUNT(*) FROM indicator_milestone');
    console.log(`\n总记录数: ${count.rows[0].count}`);

  } catch (error) {
    console.error('❌ 错误:', error.message);
  } finally {
    await client.end();
  }
}

checkMilestone();
