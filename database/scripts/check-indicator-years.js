/**
 * 检查 indicator 表中的年份分布
 * 用于排查"年份切换无效"问题
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

async function checkIndicatorYears() {
  // 从 DB_URL 解析连接信息
  const dbUrl = process.env.DB_URL || 'jdbc:postgresql://localhost:5432/sism';
  const urlMatch = dbUrl.match(/jdbc:postgresql:\/\/([^:]+):(\d+)\/([^?]+)/);
  
  const client = new Client({
    host: urlMatch ? urlMatch[1] : (process.env.DB_HOST || 'localhost'),
    port: urlMatch ? parseInt(urlMatch[2]) : parseInt(process.env.DB_PORT || '5432'),
    database: urlMatch ? urlMatch[3] : (process.env.DB_NAME || 'sism'),
    user: process.env.DB_USERNAME || process.env.DB_USER || 'postgres',
    password: process.env.DB_PASSWORD || 'postgres'
  });

  try {
    await client.connect();
    console.log('✅ 已连接到数据库');

    // 1. 检查年份分布
    console.log('\n📊 年份分布统计:');
    console.log('='.repeat(60));
    const yearDistribution = await client.query(`
      SELECT 
        year,
        COUNT(*) as count,
        COUNT(CASE WHEN status = 'ACTIVE' THEN 1 END) as active_count,
        COUNT(CASE WHEN is_deleted = true THEN 1 END) as deleted_count
      FROM indicator
      GROUP BY year
      ORDER BY year DESC
    `);
    
    console.table(yearDistribution.rows);

    // 2. 检查每个年份的示例数据
    console.log('\n📝 各年份示例指标:');
    console.log('='.repeat(60));
    for (const row of yearDistribution.rows) {
      const year = row.year;
      const samples = await client.query(`
        SELECT 
          indicator_id,
          indicator_desc,
          year,
          status,
          owner_dept,
          responsible_dept
        FROM indicator
        WHERE year = $1 AND (is_deleted = false OR is_deleted IS NULL)
        LIMIT 3
      `, [year]);

      console.log(`\n${year} 年度 (共 ${row.count} 条):`);
      samples.rows.forEach(s => {
        console.log(`  - [${s.indicator_id}] ${s.indicator_desc.substring(0, 40)}... (${s.status})`);
      });
    }

    // 3. 检查是否有 NULL 年份
    const nullYears = await client.query(`
      SELECT COUNT(*) as count
      FROM indicator
      WHERE year IS NULL
    `);
    
    if (parseInt(nullYears.rows[0].count) > 0) {
      console.log(`\n⚠️  警告: 发现 ${nullYears.rows[0].count} 条年份为 NULL 的记录`);
    }

    // 4. 总结
    console.log('\n' + '='.repeat(60));
    console.log('📋 总结:');
    const total = yearDistribution.rows.reduce((sum, row) => sum + parseInt(row.count), 0);
    console.log(`  总指标数: ${total}`);
    console.log(`  年份范围: ${yearDistribution.rows[yearDistribution.rows.length - 1]?.year} - ${yearDistribution.rows[0]?.year}`);
    console.log(`  年份种类: ${yearDistribution.rows.length} 个不同年份`);

  } catch (error) {
    console.error('❌ 错误:', error.message);
    throw error;
  } finally {
    await client.end();
    console.log('\n✅ 数据库连接已关闭');
  }
}

// 执行检查
checkIndicatorYears().catch(console.error);
