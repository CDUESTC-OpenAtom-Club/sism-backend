const { Client } = require('pg');

async function listTables() {
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

    const result = await client.query(`
      SELECT table_name 
      FROM information_schema.tables 
      WHERE table_schema = 'public' 
        AND table_type = 'BASE TABLE'
      ORDER BY table_name
    `);
    
    console.log('📋 数据库中的所有表:');
    console.log('='.repeat(60));
    result.rows.forEach((row, index) => {
      console.log(`${(index + 1).toString().padStart(3)}. ${row.table_name}`);
    });
    console.log('='.repeat(60));
    console.log(`\n总计: ${result.rows.length} 个表\n`);

  } catch (error) {
    console.error('❌ 错误:', error.message);
  } finally {
    await client.end();
  }
}

listTables();
