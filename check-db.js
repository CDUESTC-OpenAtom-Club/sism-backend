const { Client } = require('pg');

const client = new Client({
  host: '175.24.139.148',
  port: 8386,
  database: 'strategic',
  user: 'postgres',
  password: '64378561huaW',
});

(async () => {
  try {
    await client.connect();
    console.log('=== 数据库连接成功 ===\n');

    // 查看所有表
    const tables = await client.query("SELECT tablename FROM pg_tables WHERE schemaname = 'public' ORDER BY tablename");
    console.log('=== 数据库表列表 ===');
    tables.rows.forEach(row => console.log('  -', row.tablename));

    // 查看表结构
    console.log('\n=== indicator 表结构 ===');
    const indicatorSchema = await client.query(`
      SELECT column_name, data_type, is_nullable
      FROM information_schema.columns
      WHERE table_name = 'indicator'
      ORDER BY ordinal_position
    `);
    indicatorSchema.rows.forEach(row => {
      console.log(`  ${row.column_name}: ${row.data_type} (nullable: ${row.is_nullable})`);
    });

    console.log('\n=== 指标表数据 (indicator) ===');
    const indicators = await client.query(`
      SELECT indicator_id, indicator_desc, level, weight_percent, target_value, actual_value,
             progress, responsible_person, status, year
      FROM indicator
      ORDER BY indicator_id
      LIMIT 15
    `);
    indicators.rows.forEach(row => {
      console.log(`  ${row.indicator_id}: ${row.indicator_desc.substring(0, 40)}...`);
      console.log(`    级别: ${row.level} | 年份: ${row.year} | 权重: ${row.weight_percent}% | 状态: ${row.status}`);
      console.log(`    目标: ${row.target_value} | 实际: ${row.actual_value} | 进度: ${row.progress}%`);
      console.log(`    责任人: ${row.responsible_person || '未指定'}`);
    });

    console.log('\n=== 用户表数据 (app_user) ===');
    const userSchema = await client.query(`
      SELECT column_name, data_type
      FROM information_schema.columns
      WHERE table_name = 'app_user'
      ORDER BY ordinal_position
    `);
    console.log('  表结构:');
    userSchema.rows.forEach(row => console.log(`    ${row.column_name}: ${row.data_type}`));

    const users = await client.query('SELECT * FROM app_user LIMIT 5');
    users.rows.forEach(row => {
      console.log(`  用户数据:`, JSON.stringify(row, null, 2).split('\n').join('\n    '));
    });

    console.log('\n=== 组织表数据 (org) ===');
    const orgSchema = await client.query(`
      SELECT column_name, data_type
      FROM information_schema.columns
      WHERE table_name = 'org'
      ORDER BY ordinal_position
    `);
    console.log('  表结构:');
    orgSchema.rows.forEach(row => console.log(`    ${row.column_name}: ${row.data_type}`));

    const orgs = await client.query('SELECT * FROM org ORDER BY org_id LIMIT 10');
    orgs.rows.forEach(row => {
      console.log(`  ${row.org_id}: ${row.org_name} | 父级: ${row.parent_org_id || '无'} | 层级: ${row.level}`);
    });

    console.log('\n=== 仪表盘统计数据 ===');
    const total = await client.query('SELECT COUNT(*) as count FROM indicator');
    const byStatus = await client.query('SELECT status, COUNT(*) as count FROM indicator GROUP BY status');
    const byYear = await client.query('SELECT year, COUNT(*) as count FROM indicator GROUP BY year ORDER BY year');

    console.log(`  总指标数: ${total.rows[0].count}`);
    console.log('  按状态统计:');
    byStatus.rows.forEach(row => console.log(`    ${row.status}: ${row.count}`));
    console.log('  按年份统计:');
    byYear.rows.forEach(row => console.log(`    ${row.year}年: ${row.count}个`));

    await client.end();
  } catch (err) {
    console.error('数据库连接或查询错误:', err.message);
    process.exit(1);
  }
})();
