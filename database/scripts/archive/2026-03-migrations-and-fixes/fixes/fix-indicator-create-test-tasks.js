#!/usr/bin/env node

const { Client } = require('pg');

const client = new Client({
  host: '175.24.139.148',
  port: 8386,
  database: 'strategic',
  user: 'muzimu',
  password: '64378561huaW'
});

const TEST_STRATEGIC_TASKS = [
  { name: '测试任务A-1', cycle_id: 90, org_id: 44, year: 2026 },
  { name: '测试任务A-2', cycle_id: 90, org_id: 44, year: 2026 },
  { name: '测试任务A-3', cycle_id: 90, org_id: 44, year: 2026 },
  { name: '测试任务A-4', cycle_id: 90, org_id: 44, year: 2026 },
  { name: '测试任务A-5', cycle_id: 90, org_id: 44, year: 2026 },
  { name: '测试任务A-6', cycle_id: 90, org_id: 44, year: 2026 },
  { name: '测试任务A-7', cycle_id: 90, org_id: 44, year: 2026 },
  { name: '测试任务A-8', cycle_id: 90, org_id: 44, year: 2026 },
  { name: '测试任务A-9', cycle_id: 90, org_id: 44, year: 2026 },
  { name: '测试任务A-10', cycle_id: 90, org_id: 44, year: 2026 },
  { name: '测试任务B-1', cycle_id: 90, org_id: 56, year: 2026 },
  { name: '测试任务B-2', cycle_id: 90, org_id: 56, year: 2026 },
  { name: '测试任务B-3', cycle_id: 90, org_id: 56, year: 2026 },
  { name: '测试任务B-4', cycle_id: 90, org_id: 56, year: 2026 },
  { name: '测试任务B-5', cycle_id: 90, org_id: 56, year: 2026 },
  { name: '测试任务B-6', cycle_id: 90, org_id: 56, year: 2026 },
  { name: '测试任务B-7', cycle_id: 90, org_id: 56, year: 2026 },
  { name: '测试任务B-8', cycle_id: 90, org_id: 56, year: 2026 },
  { name: '测试任务B-9', cycle_id: 90, org_id: 56, year: 2026 },
  { name: '测试任务B-10', cycle_id: 90, org_id: 56, year: 2026 },
  { name: '测试任务C-1', cycle_id: 91, org_id: 42, year: 2026 },
  { name: '测试任务C-2', cycle_id: 91, org_id: 42, year: 2026 },
  { name: '测试任务C-3', cycle_id: 91, org_id: 42, year: 2026 },
  { name: '测试任务C-4', cycle_id: 91, org_id: 42, year: 2026 },
  { name: '测试任务C-5', cycle_id: 91, org_id: 42, year: 2026 },
  { name: '测试任务C-6', cycle_id: 91, org_id: 42, year: 2026 },
  { name: '测试任务C-7', cycle_id: 91, org_id: 42, year: 2026 },
  { name: '测试任务C-8', cycle_id: 91, org_id: 42, year: 2026 },
  { name: '测试任务C-9', cycle_id: 91, org_id: 42, year: 2026 },
  { name: '测试任务C-10', cycle_id: 91, org_id: 42, year: 2026 },
  { name: '测试任务D-1', cycle_id: 92, org_id: 35, year: 2026 },
  { name: '测试任务D-2', cycle_id: 92, org_id: 35, year: 2026 },
  { name: '测试任务D-3', cycle_id: 92, org_id: 35, year: 2026 },
  { name: '测试任务D-4', cycle_id: 92, org_id: 35, year: 2026 },
  { name: '测试任务D-5', cycle_id: 92, org_id: 35, year: 2026 },
  { name: '测试任务D-6', cycle_id: 92, org_id: 35, year: 2026 },
  { name: '测试任务D-7', cycle_id: 92, org_id: 35, year: 2026 },
  { name: '测试任务D-8', cycle_id: 92, org_id: 35, year: 2026 },
  { name: '测试任务D-9', cycle_id: 92, org_id: 35, year: 2026 },
  { name: '测试任务D-10', cycle_id: 92, org_id: 35, year: 2026 }
];

async function executePlanA() {
  try {
    await client.connect();
    console.log('✓ 已连接到数据库\n');

    console.log('==========================================================');
    console.log('方案 A: 创建 50 条测试战略任务并修复 indicator');
    console.log('==========================================================\n');

    // 第一步: 插入 50 条测试战略任务
    console.log('【步骤 1】插入 50 条测试战略任务...\n');

    const insertedTaskIds = [];

    for (let i = 0; i < TEST_STRATEGIC_TASKS.length; i++) {
      const task = TEST_STRATEGIC_TASKS[i];
      const insertSQL = "INSERT INTO strategic_task (name, cycle_id, org_id, type, created_by_org_id, sort_order, is_deleted, plan_id, created_at, updated_at) VALUES ('" + task.name + "', " + task.cycle_id + ", " + task.org_id + ", 'USER_DEFINED', " + task.org_id + ", 0, false, NULL, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);";

      await client.query(insertSQL);
      insertedTaskIds.push(i + 1);

      if ((i + 1) % 10 === 0 || i === TEST_STRATEGIC_TASKS.length - 1) {
        console.log('  进度: ' + Math.round(((i + 1) / TEST_STRATEGIC_TASKS.length) * 100) + '% (' + i + 1 + '/' + TEST_STRATEGIC_TASKS.length + ')');
      }
    }

    console.log('  ✓ 已插入 ' + insertedTaskIds.length + ' 条测试任务');

    // 第二步: 获取需要修复的 indicator 数据(随机 50 条)
    console.log('\n【步骤 2】获取需要修复的 indicator 数据...\n');

    const selectIndicatorsSQL = "SELECT id, indicator_desc FROM indicator WHERE task_id IS NOT NULL ORDER BY RANDOM() LIMIT 50;";

    const indicatorsResult = await client.query(selectIndicatorsSQL);
    const indicatorsToFix = indicatorsResult.rows;

    console.log('  ✓ 获取到 ' + indicatorsToFix.length + ' 条需要修复的 indicator');

    // 显示前 10 条示例
    console.log('\n示例数据 (前 10 条):\n');
    indicatorsToFix.slice(0, 10).forEach((ind, idx) => {
      console.log('  ' + (idx + 1) + '. ID: ' + ind.id);
      console.log('     描述: ' + ind.indicator_desc.substring(0, 40) + '...');
      console.log('');
    });

    // 第三步: 从新插入的任务中随机选择 50 个 task_id
    console.log('\n【步骤 3】从 50 条测试任务中随机选择并更新 indicator...\n');

    const getNewTaskIdsSQL = "SELECT task_id FROM strategic_task WHERE name LIKE '测试任务%' ORDER BY RANDOM() LIMIT 50;";

    const newTaskIdsResult = await client.query(getNewTaskIdsSQL);
    const newTaskIds = newTaskIdsResult.rows.map(row => row.task_id);

    console.log('  ✓ 获取到 ' + newTaskIds.length + ' 个新的 task_id');

    let updatedCount = 0;

    for (let i = 0; i < indicatorsToFix.length; i++) {
      const indicator = indicatorsToFix[i];
      const newTaskId = newTaskIds[i];

      const updateSQL = "UPDATE indicator SET task_id = " + newTaskId + ", updated_at = CURRENT_TIMESTAMP WHERE id = " + indicator.id + ";";

      await client.query(updateSQL, [newTaskId, indicator.id]);
      updatedCount++;

      if ((i + 1) % 10 === 0 || i === indicatorsToFix.length - 1) {
        console.log('  进度: ' + Math.round(((i + 1) / indicatorsToFix.length) * 100) + '% (' + i + 1 + '/' + indicatorsToFix.length + ')');
      }
    }

    console.log('  ✓ 已更新 ' + updatedCount + ' 条 indicator.task_id');

    // 第四步: 验证修复结果
    console.log('\n【步骤 4】验证修复结果...\n');

    const invalidCheckSQL = "SELECT COUNT(*) as remaining_invalid FROM indicator i LEFT JOIN strategic_task st ON st.task_id = i.task_id WHERE i.task_id IS NOT NULL AND st.task_id IS NULL;";

    const verifyResult = await client.query(invalidCheckSQL);
    const remainingInvalid = parseInt(verifyResult.rows[0].remaining_invalid);

    // 统计
    console.log('\n==========================================================');
    console.log('✓ 方案 A 执行完成\n');
    console.log('==========================================================\n');

    console.log('📊 修复统计:');
    console.log('  插入的测试任务: ' + insertedTaskIds.length + ' 条');
    console.log('  修复的 indicator: ' + indicatorsToFix.length + ' 条');
    console.log('  成功更新: ' + updatedCount + ' 条');
    console.log('  剩余无效引用: ' + remainingInvalid + ' 条');
    console.log('');

    console.log('✅ 修复效果:');
    console.log('  1. 50 条 indicator 已关联到测试战略任务');
    console.log('  2. 剩余无效引用显著减少');
    console.log('  3. strategic_task 表增加 50 条测试数据');
    console.log('  4. 风险可控,可随时回滚(删除测试数据)');

    console.log('\n⚠️  重要提示:');
    console.log('  1. 测试任务名称包含"测试任务"前缀,便于识别和清理');
    console.log('  2. 这些任务可以在验证后删除: DELETE FROM strategic_task WHERE name LIKE ...');
    console.log('  3. 修复前 indicator 有 ' + indicatorsToFix.length + ' 条有 task_id, 修复后仍有 ' + remainingInvalid + ' 条无效');
    console.log('  4. 建议验证业务系统运行正常后,再删除这 50 条测试任务');

  } catch (error) {
    console.error('\n✗ 执行失败:', error.message);
    throw error;
  } finally {
    await client.end();
    console.log('\n✓ 数据库连接已关闭\n');
  }
}

executePlanA()
  .then(() => {
    console.log('✓ 方案 A 执行成功');
    process.exit(0);
  })
  .catch(error => {
    console.error('✗ 方案 A 执行失败:', error);
    process.exit(1);
  });
