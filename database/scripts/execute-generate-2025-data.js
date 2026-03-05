/**
 * 执行生成 2025 年数据的 SQL 脚本
 */

const { Client } = require('pg');
const fs = require('fs');
const path = require('path');

// 手动读取 .env 文件
function loadEnv() {
  const envPath = path.join(__dirname, '../../.env');
  if (fs.existsSync(envPath)) {
    const envContent = fs.readFileSync(envPath, 'utf-8');
    envContent.split('\n').forEach(line => {
      const match = line.match(/^([^=:#]+)=(.*)$/);
      if (match) {
        const key = match[1].trim();
        const value = match[2].trim();
        process.env[key] = value;
      }
    });
  }
}

loadEnv();

async function executeSQL() {
  // 直接使用环境变量中的配置
  const client = new Client({
    host: '175.24.139.148',
    port: 8386,
    database: 'strategic',
    user: 'postgres',
    password: '64378561huaW'
  });

  try {
    console.log('🔌 正在连接到数据库...');
    console.log(`   Host: ${client.host}:${client.port}`);
    console.log(`   Database: ${client.database}`);
    console.log(`   User: ${client.user}`);
    
    await client.connect();
    console.log('✅ 数据库连接成功\n');

    // 读取 SQL 文件
    const sqlPath = path.join(__dirname, 'generate-2025-data.sql');
    console.log(`📄 读取 SQL 文件: ${sqlPath}`);
    const sql = fs.readFileSync(sqlPath, 'utf-8');
    console.log(`✅ SQL 文件读取成功 (${sql.length} 字符)\n`);

    // 执行 SQL
    console.log('🚀 开始执行 SQL 脚本...\n');
    console.log('='.repeat(60));
    
    const result = await client.query(sql);
    
    console.log('='.repeat(60));
    console.log('\n✅ SQL 脚本执行成功！\n');

    // 验证结果
    console.log('📊 验证生成的数据...\n');
    
    // 检查任务数量
    const taskResult = await client.query(`
      SELECT COUNT(*) as count
      FROM sys_task
    `);
    
    console.log('战略任务统计:');
    console.log(`  总数: ${taskResult.rows[0].count} 个`);
    console.log('');
    
    // 检查指标数量
    const indicatorResult = await client.query(`
      SELECT 
        year, 
        COUNT(*) as count,
        COUNT(CASE WHEN status = 'ACTIVE' THEN 1 END) as active_count,
        ROUND(AVG(progress)) as avg_progress
      FROM indicator
      WHERE is_deleted = false OR is_deleted IS NULL
      GROUP BY year
      ORDER BY year DESC
    `);
    
    console.log('指标统计:');
    indicatorResult.rows.forEach(row => {
      console.log(`  ${row.year} 年: ${row.count} 个 (活跃: ${row.active_count}, 平均进度: ${row.avg_progress}%)`);
    });
    console.log('');
    
    // 检查里程碑数量
    const milestoneResult = await client.query(`
      SELECT 
        i.year,
        COUNT(m.id) as count,
        COUNT(CASE WHEN m.status = 'COMPLETED' THEN 1 END) as completed_count,
        COUNT(CASE WHEN m.status = 'NOT_STARTED' THEN 1 END) as not_started_count
      FROM indicator_milestone m
      JOIN indicator i ON i.indicator_id = m.indicator_id
      GROUP BY i.year
      ORDER BY i.year DESC
    `);
    
    console.log('里程碑统计:');
    milestoneResult.rows.forEach(row => {
      console.log(`  ${row.year} 年: ${row.count} 个 (已完成: ${row.completed_count}, 未开始: ${row.not_started_count})`);
    });
    console.log('');
    
    // 显示 2025 年示例数据
    const sampleResult = await client.query(`
      SELECT 
        indicator_id,
        LEFT(indicator_desc, 50) as indicator_desc,
        progress,
        owner_dept,
        responsible_dept
      FROM indicator
      WHERE year = 2025 AND (is_deleted = false OR is_deleted IS NULL)
      ORDER BY indicator_id
      LIMIT 5
    `);
    
    console.log('2025 年示例指标:');
    sampleResult.rows.forEach((row, index) => {
      console.log(`  ${index + 1}. [${row.indicator_id}] ${row.indicator_desc}...`);
      console.log(`     进度: ${row.progress}% | ${row.owner_dept} → ${row.responsible_dept}`);
    });
    console.log('');
    
    console.log('='.repeat(60));
    console.log('✅ 2025 年数据生成完成！');
    console.log('='.repeat(60));
    console.log('\n💡 下一步:');
    console.log('   1. 刷新前端页面');
    console.log('   2. 点击年份选择器');
    console.log('   3. 切换到 2025 年');
    console.log('   4. 验证数据显示正确\n');

  } catch (error) {
    console.error('\n❌ 执行失败:', error.message);
    if (error.code) {
      console.error(`   错误代码: ${error.code}`);
    }
    if (error.detail) {
      console.error(`   详细信息: ${error.detail}`);
    }
    if (error.hint) {
      console.error(`   提示: ${error.hint}`);
    }
    throw error;
  } finally {
    await client.end();
    console.log('\n🔌 数据库连接已关闭');
  }
}

// 执行脚本
executeSQL().catch(error => {
  console.error('\n💥 脚本执行失败');
  process.exit(1);
});
