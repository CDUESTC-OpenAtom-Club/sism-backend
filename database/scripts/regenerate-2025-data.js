const { Client } = require('pg');
const fs = require('fs');
const path = require('path');

async function regenerate() {
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

    // 步骤 1: 清理旧数据
    console.log('🧹 步骤 1: 清理已有的 2025 年数据...');
    const cleanupSQL = fs.readFileSync(
      path.join(__dirname, 'cleanup-2025-data.sql'),
      'utf-8'
    );
    await client.query(cleanupSQL);
    console.log('✅ 清理完成\n');

    // 步骤 2: 生成新数据
    console.log('🚀 步骤 2: 生成完整的 2025 年数据...');
    const generateSQL = fs.readFileSync(
      path.join(__dirname, 'generate-2025-complete-data.sql'),
      'utf-8'
    );
    const result = await client.query(generateSQL);
    console.log('✅ 生成完成\n');

    // 显示统计结果
    if (result.rows && result.rows.length > 0) {
      console.log('📊 生成数据统计:');
      console.log('='.repeat(80));
      result.rows.forEach(row => {
        console.log(`  ${row['统计项']}: ${row['数量']}`);
      });
      console.log('');
    }

    // 步骤 3: 详细验证
    console.log('🔍 步骤 3: 验证数据完整性...\n');

    // 验证任务
    const taskCheck = await client.query(`
      SELECT 
        t.task_id,
        t.name,
        COUNT(i.id) as indicator_count
      FROM sys_task t
      LEFT JOIN indicator i ON i.task_id = t.task_id AND i.year = 2025
      WHERE t.cycle_id = 7
      GROUP BY t.task_id, t.name
      ORDER BY t.task_id
    `);

    console.log('任务与指标关联:');
    taskCheck.rows.forEach(row => {
      console.log(`  [${row.task_id}] ${row.name}: ${row.indicator_count} 个指标`);
    });
    console.log('');

    // 验证指标层级
    const hierarchyCheck = await client.query(`
      SELECT 
        CASE 
          WHEN parent_indicator_id IS NULL THEN '父指标'
          ELSE '子指标'
        END as 类型,
        COUNT(*) as 数量,
        ROUND(AVG(progress)) as 平均进度
      FROM indicator
      WHERE year = 2025 AND is_deleted = false
      GROUP BY CASE WHEN parent_indicator_id IS NULL THEN '父指标' ELSE '子指标' END
    `);

    console.log('指标层级分布:');
    hierarchyCheck.rows.forEach(row => {
      console.log(`  ${row['类型']}: ${row['数量']} 个 (平均进度: ${row['平均进度']}%)`);
    });
    console.log('');

    // 验证里程碑
    const milestoneCheck = await client.query(`
      SELECT 
        COUNT(*) as total,
        COUNT(CASE WHEN m.status = 'COMPLETED' THEN 1 END) as completed,
        COUNT(CASE WHEN m.status = 'IN_PROGRESS' THEN 1 END) as in_progress,
        COUNT(CASE WHEN m.status = 'NOT_STARTED' THEN 1 END) as not_started
      FROM indicator_milestone m
      JOIN indicator i ON m.indicator_id = i.id
      WHERE i.year = 2025
    `);

    console.log('里程碑状态分布:');
    const ms = milestoneCheck.rows[0];
    console.log(`  总数: ${ms.total}`);
    console.log(`  已完成: ${ms.completed}, 进行中: ${ms.in_progress}, 未开始: ${ms.not_started}`);
    console.log('');

    console.log('='.repeat(80));
    console.log('✅ 2025 年完整数据生成成功！');
    console.log('='.repeat(80));
    console.log('\n💡 下一步:');
    console.log('   1. 刷新前端页面');
    console.log('   2. 切换到 2025 年');
    console.log('   3. 验证父指标和子指标都能正常显示\n');

  } catch (error) {
    console.error('\n❌ 错误:', error.message);
    if (error.detail) console.error('   详细:', error.detail);
    throw error;
  } finally {
    await client.end();
    console.log('🔌 数据库连接已关闭');
  }
}

regenerate().catch(() => process.exit(1));
