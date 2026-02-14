#!/usr/bin/env node

/**
 * 检查 strategic_task 表的数据量
 *
 * 目的: 确认是否只有 4 条,还是应该有更多数据
 */

const { Client } = require('pg');

const client = new Client({
  host: '175.24.139.148',
  port: 8386,
  database: 'strategic',
  user: 'muzimu',
  password: '64378561huaW'
});

async function checkStrategicTaskData() {
  try {
    await client.connect();
    console.log('✓ 已连接到数据库\n');

    console.log('==========================================================');
    console.log('检查 strategic_task 表数据量');
    console.log('==========================================================\n');

    // 第一步: 检查当前数据量
    console.log('【步骤 1】检查 strategic_task 表数据...\n');

    const countSQL = `SELECT COUNT(*) as total FROM strategic_task;`;
    const countResult = await client.query(countSQL);
    const totalCount = parseInt(countResult.rows[0].total);

    console.log(`📊 strategic_task 表统计:`);
    console.log(`  总记录数: ${totalCount} 条`);

    // 第二步: 按年份分组统计
    const yearSQL = `
      SELECT
        extract(YEAR FROM created_at) as year,
        COUNT(*) as count
      FROM strategic_task
      GROUP BY extract(YEAR FROM created_at)
      ORDER BY year;
    `;

    const yearResult = await client.query(yearSQL);

    if (yearResult.rows.length > 0) {
      console.log('\n📅 按年度分组:');
      yearResult.rows.forEach(row => {
        const year = row.year || 'NULL';
        const count = parseInt(row.count);
        console.log(`  ${year}年: ${count} 条`);
      });
    }

    // 第三步: 显示示例数据
    console.log('\n【步骤 2】显示示例数据 (最多 20 条)...\n');

    const sampleSQL = `
      SELECT
        task_id,
        name,
        cycle_id,
        org_id,
        created_at
      FROM strategic_task
      ORDER BY created_at DESC
      LIMIT 20;
    `;

    const sampleResult = await client.query(sampleSQL);

    if (sampleResult.rows.length > 0) {
      console.log('示例数据:\n');
      sampleResult.rows.forEach((row, index) => {
        console.log(`  ${index + 1}. ID: ${row.task_id}`);
        console.log(`     名称: ${row.name}`);
        console.log(`     周期ID: ${row.cycle_id}`);
        console.log(`     组织ID: ${row.org_id}`);
        console.log(`     创建时间: ${row.created_at}`);
        console.log('');
      });
    }

    // 第四步: 分析结论
    console.log('==========================================================');
    console.log('📊 检查结论:\n');

    if (totalCount === 4) {
      console.log('⚠️  strategic_task 表只有 4 条数据!');
      console.log('\n问题分析:');
      console.log('  1. indicator 表有 711 条记录');
      console.log('  2. 其中 703 条 (约 99%) 的 task_id 引用不存在的 strategic_task');
      console.log('  3. strategic_task 表只有 4 条记录');
      console.log('\n结论:');
      console.log('  ⚠️ 如果指标必须关联战略任务:');
      console.log('      → 需要创建 700+ 条战略任务记录');
      console.log('      → 或者修复这 703 条指标的 task_id 为 NULL');
      console.log('\n  ✅ 如果指标可以不关联任务:');
      console.log('      → 当前修复是正确的 (task_id = NULL)');
      console.log('      → strategic_task 表确实应该只有 4 条数据');
    } else if (totalCount > 4) {
      console.log(`✅ strategic_task 表有 ${totalCount} 条数据`);
      console.log('\n建议:');
      console.log('  → 当前 703 条无效引用可能对应这些任务');
      console.log('  → 需要检查并更新 indicator 表的 task_id 引用');
    } else {
      console.log('⚠️ strategic_task 表为空!');
    }

    console.log('\n⚠️ 修复建议:');
    console.log('  【方案 A】如果指标必须关联任务,创建缺失的战略任务记录');
    console.log('  【方案 B】如果指标可以不关联任务,保持当前 task_id = NULL');
    console.log('  【方案 C】检查并更新 indicator 表的 task_id,指向正确的战略任务');
    console.log('==========================================================\n');

  } catch (error) {
    console.error('\n✗ 检查失败:', error.message);
    throw error;
  } finally {
    await client.end();
    console.log('✓ 数据库连接已关闭\n');
  }
}

checkStrategicTaskData()
  .then(() => {
    console.log('✓ 检查脚本执行成功');
    process.exit(0);
  })
  .catch(error => {
    console.error('✗ 检查脚本执行失败:', error);
    process.exit(1);
  });
