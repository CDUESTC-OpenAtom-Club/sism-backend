#!/usr/bin/env node

/**
 * 修复 indicator 表的 task_id 引用问题
 *
 * 问题: indicator 表有 703 条记录的 task_id 引用不存在的表
 * 原因: indicator 表原本指向 task_deprecated (已废弃),迁移后未更新
 * 修复: 将这 703 条的 task_id 设为 NULL (指标不关联任务)
 */

const { Client } = require('pg');

const client = new Client({
  host: '175.24.139.148',
  port: 8386,
  database: 'strategic',
  user: 'muzimu',
  password: '64378561huaW'
});

async function fixIndicatorTaskIds() {
  try {
    await client.connect();
    console.log('✓ 已连接到数据库\n');

    console.log('==========================================================');
    console.log('修复 indicator 表的 task_id 引用问题');
    console.log('==========================================================\n');

    // 第一步: 检查问题数据
    console.log('【步骤 1】检查问题数据...\n');

    const checkSQL = `
      SELECT
          COUNT(*) as total_count,
          COUNT(CASE WHEN task_id IS NOT NULL THEN 1 END) as with_task_id
      FROM indicator;
    `;

    const checkResult = await client.query(checkSQL);
    const totalCount = parseInt(checkResult.rows[0].total_count);
    const withTaskId = parseInt(checkResult.rows[0].with_task_id);

    console.log(`📊 indicator 表统计:`);
    console.log(`  总记录数: ${totalCount}`);
    console.log(`  有 task_id: ${withTaskId}`);
    console.log(`  无 task_id: ${totalCount - withTaskId}\n`);

    // 检查无效引用数量
    const invalidCheckSQL = `
      SELECT COUNT(*) as invalid_count
      FROM indicator i
      LEFT JOIN strategic_task st ON st.task_id = i.task_id
      WHERE i.task_id IS NOT NULL
        AND st.task_id IS NULL;
    `;

    const invalidResult = await client.query(invalidCheckSQL);
    const invalidCount = parseInt(invalidResult.rows[0].invalid_count);

    if (invalidCount === 0) {
      console.log('✅ 未发现无效 task_id 引用\n');
      return;
    }

    console.log(`⚠️  发现 ${invalidCount} 条无效 task_id 引用\n`);

    // 显示示例数据
    const sampleSQL = `
      SELECT
          i.id,
          i.task_id,
          i.indicator_desc,
          st.task_id AS strategic_task_id,
          st.name AS strategic_task_name
      FROM indicator i
      LEFT JOIN strategic_task st ON st.task_id = i.task_id
      WHERE i.task_id IS NOT NULL
        AND st.task_id IS NULL
      LIMIT 10;
    `;

    const sampleResult = await client.query(sampleSQL);

    if (sampleResult.rows.length > 0) {
      console.log('📋 示例数据 (前 10 条):\n');
      sampleResult.rows.forEach((row, index) => {
        console.log(`  ${index + 1}. ID: ${row.id}`);
        console.log(`     task_id: ${row.task_id} (不存在)`);
        console.log(`     描述: ${row.indicator_desc.substring(0, 50)}...`);
        console.log('');
      });
    }

    // 第二步: 确认修复方案
    console.log('\n【步骤 2】确认修复方案...\n');
    console.log('✓ 修复方案: 将无效 task_id 设为 NULL');
    console.log('✓ 说明: 这些指标不关联任何战略任务\n');

    // 第三步: 执行修复
    console.log('\n【步骤 3】执行修复...\n');

    const updateSQL = `
      UPDATE indicator
      SET task_id = NULL,
          updated_at = CURRENT_TIMESTAMP
      WHERE task_id IS NOT NULL
        AND NOT EXISTS (
          SELECT 1
          FROM strategic_task
          WHERE task_id = indicator.task_id
        );
    `;

    console.log('🔧 正在更新 indicator 表...');
    const updateResult = await client.query(updateSQL);
    const updatedCount = updateResult.rowCount;

    console.log(`✓ 已更新 ${updatedCount} 条记录`);

    // 第四步: 验证修复结果
    console.log('\n【步骤 4】验证修复结果...\n');

    const verifySQL = `
      SELECT COUNT(*) as remaining_invalid
      FROM indicator i
      LEFT JOIN strategic_task st ON st.task_id = i.task_id
      WHERE i.task_id IS NOT NULL
        AND st.task_id IS NULL;
    `;

    const verifyResult = await client.query(verifySQL);
    const remainingInvalid = parseInt(verifyResult.rows[0].remaining_invalid);

    console.log('📊 修复后统计:');
    console.log(`  剩余无效引用: ${remainingInvalid}`);

    if (remainingInvalid === 0) {
      console.log('\n✅ 所有无效 task_id 引用已修复!');
    } else {
      console.log(`\n⚠️  仍有 ${remainingInvalid} 条无效引用`);
    }

    // 输出总结报告
    console.log('\n==========================================================');
    console.log('✅ 修复完成\n');
    console.log('📊 修复统计:');
    console.log(`  发现问题: ${invalidCount} 条`);
    console.log(`  成功修复: ${updatedCount} 条`);
    console.log(`  剩余问题: ${remainingInvalid} 条`);
    console.log('\n⚠️  重要提示:');
    console.log('  1. 这些指标的 task_id 现在为 NULL');
    console.log('  2. 如果业务逻辑需要关联任务,需要手动创建关联');
    console.log('  3. 建议验证业务系统运行正常');
    console.log('==========================================================\n');

  } catch (error) {
    console.error('\n✗ 修复失败:', error.message);
    throw error;
  } finally {
    await client.end();
    console.log('✓ 数据库连接已关闭\n');
  }
}

// 执行修复
fixIndicatorTaskIds()
  .then(() => {
    console.log('✓ 修复脚本执行成功');
    console.log('\n下一步: 可以安全删除废弃表了');
    process.exit(0);
  })
  .catch(error => {
    console.error('✗ 修复脚本执行失败:', error);
    process.exit(1);
  });
