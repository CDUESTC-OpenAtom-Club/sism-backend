#!/usr/bin/env node

/**
 * V1.9 补充脚本: 删除剩余的外键约束
 *
 * 之前的脚本删除了部分外键,但还有一些遗漏
 * 这个脚本将删除所有剩余的外键约束
 */

const { Client } = require('pg');

const client = new Client({
  host: '175.24.139.148',
  port: 8386,
  database: 'strategic',
  user: 'muzimu',
  password: '64378561huaW'
});

/**
 * 剩余的外键约束清单(按表分组)
 */
const remainingForeignKeys = {
  // sys_user_deprecated 表
  'sys_user_deprecated': ['fk4t57d8akmdpl9klotnq9elx5j'],

  // common_log 表
  'common_log': [
    'audit_log_actor_org_id_fkey',
    'audit_log_actor_user_id_fkey'
  ],

  // indicator_milestone 表
  'indicator_milestone': [
    'fknsg6vu52h1w3i9vjgtarsas0w',
    'milestone_indicator_id_fkey'
  ],

  // sys_org 表
  'sys_org': ['fkilhqbikhb5oxlxc4qo2j6ur47'],

  // refresh_tokens 表
  'refresh_tokens': ['fk_refresh_tokens_user_id'],

  // audit_flow_def 相关表
  'audit_step_def': ['audit_step_def_flow_id_fkey'],
  'audit_instance': [
    'audit_instance_current_step_id_fkey',
    'audit_instance_flow_id_fkey'
  ],
  'audit_action_log': [
    'audit_action_log_from_step_id_fkey',
    'audit_action_log_instance_id_fkey',
    'audit_action_log_step_id_fkey',
    'audit_action_log_to_step_id_fkey'
  ],

  // plan_report_indicator 表
  'plan_report_indicator': ['plan_report_indicator_report_id_fkey'],

  // plan_report_indicator_attachment 表
  'plan_report_indicator_attachment': [
    'plan_report_indicator_attachment_attachment_id_fkey',
    'plan_report_indicator_attachment_plan_report_indicator_id_fkey'
  ],

  // 预警告警表
  '2_warn_event': ['warn_event_level_id_fkey'],
  '2_warn_summary_daily': ['warn_summary_daily_level_id_fkey'],

  // 系统角色权限表
  'sys_user_role': [
    'sys_user_role_role_id_fkey',
    'sys_user_role_user_id_fkey'
  ],
  'sys_role_permission': [
    'sys_role_permission_perm_id_fkey',
    'sys_role_permission_role_id_fkey'
  ],

  // adhoc_task_indicator_map 表
  'adhoc_task_indicator_map': [
    'fkbj5sf01egss1wsq1q8y9yg6ag',
    'fkqfjy0x8qt95ucyh6chglogblq'
  ],

  // adhoc_task_target 表
  'adhoc_task_target': [
    'fk2h6ehplkoah6sp0pxp1dlbkua',
    'fkbca5eqhotyqb4v6nl2ekva80j',
    'fkd8x5son8i6hxtyx7bp7jlt46h'
  ],

  // alert_event 表
  'alert_event': [
    'fk1sehn6mpxtshf781kyly6ecre',
    'fkajaeo9e6s7r4vp5a9o8f0u39g',
    'fkeg9g2avn3h566oyg76tsoraxh',
    'fkpwuvi62205iww6i8svcqht7x',
    'fkqpvvdt0y5aba9k4tkmpqu2aut'
  ],

  // alert_rule 表
  'alert_rule': [
    'fkbgrvb6k5fc6v32wnishg0dbbt',
    'fkjohoca8obstyp3y41pnpdal6v'
  ],

  // alert_window 表
  'alert_window': [
    'fk4kkplw2s4thxscplhsoon868q',
    'fkg6seqmmvduyjc1xi8ja1wic4c'
  ],

  // sys_user 表
  'sys_user': ['fk_sys_user_sys_org'],

  // approval_record 表(剩余)
  'approval_record': [],  // 已删除

  // audit_log 表(剩余部分)
  'audit_log': [],  // 已删除

  // milestone 表(剩余部分)
  'milestone': [],  // 已删除

  // progress_report 表(剩余部分)
  'progress_report': [],  // 已删除

  // strategic_task 表(剩余部分)
  'strategic_task': [],  // 已删除

  // indicator 表(剩余部分)
  'indicator': []  // 已删除
};

/**
 * 主执行函数
 */
async function removeRemainingForeignKeys() {
  try {
    await client.connect();
    console.log('✓ 已连接到数据库\n');

    console.log('==========================================================');
    console.log('V1.9 补充脚本: 删除剩余外键约束\n');

    // 第一步: 检查当前剩余的外键
    console.log('【步骤 1】检查剩余外键约束...\n');
    const checkSQL = `
      SELECT
          conname AS constraint_name,
          conrelid::regclass AS table_name,
          confrelid::regclass AS referenced_table
      FROM pg_constraint c
      JOIN pg_namespace n ON n.oid = c.connamespace
      WHERE contype = 'f'
        AND n.nspname = 'public'
      ORDER BY conrelid::regclass, conname;
    `;

    const checkResult = await client.query(checkSQL);
    const remainingCount = checkResult.rows.length;
    console.log(`✓ 当前剩余 ${remainingCount} 个外键约束\n`);

    // 按表分组显示
    const byTable = {};
    checkResult.rows.forEach(row => {
      const table = row.table_name;
      if (!byTable[table]) {
        byTable[table] = [];
      }
      byTable[table].push(row);
    });

    console.log('剩余外键分布:');
    Object.keys(byTable).forEach(table => {
      console.log(`  📋 ${table}: ${byTable[table].length} 个`);
    });

    // 第二步: 删除所有剩余的外键
    console.log('\n【步骤 2】删除所有剩余外键约束...\n');

    let droppedCount = 0;
    let skippedCount = 0;
    let errorCount = 0;

    // 遍历所有表
    for (const tableName of Object.keys(byTable)) {
      const constraints = byTable[tableName];

      console.log(`\n处理表: ${tableName} (${constraints.length} 个外键)`);

      for (const row of constraints) {
        const constraintName = row.constraint_name;
        const referencedTable = row.referenced_table;

        try {
          const dropSQL = `ALTER TABLE public.${tableName} DROP CONSTRAINT IF EXISTS ${constraintName};`;
          await client.query(dropSQL);

          droppedCount++;

          // 标记指向废弃表的外键
          const isDeprecated = ['task_deprecated', 'org_deprecated', 'sys_user_deprecated'].includes(referencedTable);
          const symbol = isDeprecated ? '⚠️  ' : '✓ ';
          const message = isDeprecated ? `已删除(指向废弃表 ${referencedTable})` : '已删除';

          console.log(`  ${symbol}${message}: ${constraintName}`);

        } catch (error) {
          // 忽略"约束不存在"错误
          if (error.message.includes('does not exist') || error.message.includes('not exist')) {
            skippedCount++;
            console.log(`  ⚠️  跳过(不存在): ${constraintName}`);
          } else {
            errorCount++;
            console.error(`  ✗ 失败: ${constraintName} - ${error.message}`);
          }
        }
      }
    }

    // 第三步: 验证外键已全部删除
    console.log('\n【步骤 3】验证外键约束...\n');
    const verifyResult = await client.query(checkSQL);
    const finalRemainingCount = verifyResult.rows.length;

    // 输出总结
    console.log('\n==========================================================');
    console.log('V1.9 补充脚本执行完成\n');
    console.log('📊 统计信息:');
    console.log(`  开始时外键数量: ${remainingCount} 个`);
    console.log(`  成功删除: ${droppedCount} 个`);
    console.log(`  跳过(不存在): ${skippedCount} 个`);
    console.log(`  失败: ${errorCount} 个`);
    console.log(`  最终剩余外键: ${finalRemainingCount} 个`);

    if (finalRemainingCount === 0) {
      console.log('\n✅ 所有外键约束已成功删除!');
    } else {
      console.log('\n⚠️  仍有外键约束未删除:');
      verifyResult.rows.forEach(row => {
        console.log(`  - ${row.table_name}.${row.constraint_name} → ${row.referenced_table}`);
      });
    }

    console.log('\n==========================================================\n');

  } catch (error) {
    console.error('\n✗ 脚本执行失败:', error.message);
    throw error;
  } finally {
    await client.end();
    console.log('✓ 数据库连接已关闭\n');
  }
}

// 执行脚本
removeRemainingForeignKeys()
  .then(() => {
    console.log('✓ 补充脚本执行成功');
    process.exit(0);
  })
  .catch(error => {
    console.error('✗ 补充脚本执行失败:', error);
    process.exit(1);
  });
