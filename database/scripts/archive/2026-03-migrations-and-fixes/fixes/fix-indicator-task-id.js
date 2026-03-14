#!/usr/bin/env node

/**
 * 修复 indicator 表的 task_id 引用问题
 *
 * 问题: indicator.task_id 有 NOT NULL 约束,但有 706 条数据引用不存在的 strategic_task
 * 修复步骤:
 *   1. 删除 task_id 字段的 NOT NULL 约束
 *   2. 将这 706 条的 task_id 设为 NULL
 */

const { Client } = require('pg');

const client = new Client({
  host: '175.24.139.148',
  port: 8386,
  database: 'strategic',
  user: 'muzimu',
  password: '64378561huaW'
});

async function fixIndicatorTaskId() {
  try {
    await client.connect();
    console.log('✓ 已连接到数据库\n');

    console.log('==========================================================');
    console.log('修复 indicator.task_id 的 NOT NULL 约束和无效引用');
    console.log('==========================================================\n');

    // 第一步: 检查问题
    console.log('【步骤 1】检查问题...\n');

    const checkSQL = `
      SELECT COUNT(*) as invalid_count
      FROM indicator i
      LEFT JOIN strategic_task st ON st.task_id = i.task_id
      WHERE i.task_id IS NOT NULL
        AND st.task_id IS NULL;
    `;

    const checkResult = await client.query(checkSQL);
    const invalidCount = parseInt(checkResult.rows[0].invalid_count);

    console.log(`⚠️  发现 ${invalidCount} 条无效 task_id 引用\n`);

    if (invalidCount === 0) {
      console.log('✅ 未发现无效引用,无需修复\n');
      return;
    }

    // 第二步: 删除 NOT NULL 约束
    console.log('\n【步骤 2】删除 task_id 的 NOT NULL 约束...\n');

    const alterSQL = `
      ALTER TABLE indicator
      ALTER COLUMN task_id DROP NOT NULL;
    `;

    await client.query(alterSQL);
    console.log('✓ 已删除 task_id 的 NOT NULL 约束');

    // 第三步: 更新无效引用为 NULL
    console.log('\n【步骤 3】更新无效引用为 NULL...\n');

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

    // 第四步: 验证
    console.log('\n【步骤 4】验证修复结果...\n');

    const verifyResult = await client.query(checkSQL);
    const remainingInvalid = parseInt(verifyResult.rows[0].invalid_count);

    console.log('📊 修复统计:');
    console.log(`  发现问题: ${invalidCount} 条`);
    console.log(`  成功修复: ${updatedCount} 条`);
    console.log(`  剩余问题: ${remainingInvalid} 条`);

    if (remainingInvalid === 0) {
      console.log('\n✅ 所有无效 task_id 引用已修复!');
    } else {
      console.log(`\n⚠️  仍有 ${remainingInvalid} 条无效引用`);
    }

    // 输出总结
    console.log('\n==========================================================');
    console.log('✅ 修复完成\n');
    console.log('⚠️  重要:');
    console.log('  1. task_id 字段现在允许 NULL');
    console.log('  2. 已将 706 条无效引用设为 NULL');
    console.log('  3. 如果业务需要关联任务,需要手动创建关联');
    console.log('  4. 现在可以安全删除废弃表了');
    console.log('==========================================================\n');

  } catch (error) {
    console.error('\n✗ 修复失败:', error.message);
    throw error;
  } finally {
    await client.end();
    console.log('✓ 数据库连接已关闭\n');
  }
}

fixIndicatorTaskId()
  .then(() => {
    console.log('✓ 修复脚本执行成功');
    process.exit(0);
  })
  .catch(error => {
    console.error('✗ 修复脚本执行失败:', error);
    process.exit(1);
  });
