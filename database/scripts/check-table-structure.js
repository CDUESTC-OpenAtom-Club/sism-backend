/**
 * 检查表结构
 */

const { Client } = require('pg');

async function checkTables() {
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

    // 检查 sys_task 表结构
    console.log('📋 sys_task 表结构:');
    console.log('='.repeat(60));
    const taskColumns = await client.query(`
      SELECT column_name, data_type, is_nullable
      FROM information_schema.columns
      WHERE table_name = 'sys_task'
      ORDER BY ordinal_position
    `);
    taskColumns.rows.forEach(row => {
      console.log(`  ${row.column_name.padEnd(30)} ${row.data_type.padEnd(20)} ${row.is_nullable === 'YES' ? 'NULL' : 'NOT NULL'}`);
    });
    
    console.log('\n📋 indicator 表结构:');
    console.log('='.repeat(60));
    const indicatorColumns = await client.query(`
      SELECT column_name, data_type, is_nullable
      FROM information_schema.columns
      WHERE table_name = 'indicator'
      ORDER BY ordinal_position
    `);
    indicatorColumns.rows.forEach(row => {
      console.log(`  ${row.column_name.padEnd(30)} ${row.data_type.padEnd(20)} ${row.is_nullable === 'YES' ? 'NULL' : 'NOT NULL'}`);
    });
    
    // 检查现有数据
    console.log('\n📊 现有数据统计:');
    console.log('='.repeat(60));
    
    const taskCount = await client.query('SELECT COUNT(*) FROM sys_task');
    console.log(`sys_task: ${taskCount.rows[0].count} 条`);
    
    const indicatorCount = await client.query('SELECT COUNT(*) FROM indicator WHERE is_deleted = false OR is_deleted IS NULL');
    console.log(`indicator: ${indicatorCount.rows[0].count} 条`);
    
    // 检查 indicator 的年份分布
    const yearDist = await client.query(`
      SELECT year, COUNT(*) as count
      FROM indicator
      WHERE is_deleted = false OR is_deleted IS NULL
      GROUP BY year
      ORDER BY year DESC
    `);
    
    console.log('\nindicator 年份分布:');
    yearDist.rows.forEach(row => {
      console.log(`  ${row.year || 'NULL'}: ${row.count} 条`);
    });

  } catch (error) {
    console.error('❌ 错误:', error.message);
  } finally {
    await client.end();
  }
}

checkTables();
