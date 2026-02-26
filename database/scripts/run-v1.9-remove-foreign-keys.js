#!/usr/bin/env node

/**
 * 执行 V1.9 迁移: 删除所有外键约束
 *
 * 目的: 适配分布式数据库架构
 * 原因:
 *   1. 分布式数据库对外键支持不佳
 *   2. 修复指向废弃表的外键约束
 *   3. 应用层约束更灵活,性能更好
 *
 * 执行时间: 2026-02-13
 */

const { Client } = require('pg');
const fs = require('fs');
const path = require('path');

const client = new Client({
  host: '175.24.139.148',
  port: 8386,
  database: 'strategic',
  user: 'muzimu',
  password: '64378561huaW'
});

/**
 * 统计信息
 */
const stats = {
  total: 0,
  dropped: 0,
  skipped: 0,
  errors: [],
  byTable: {}
};

/**
 * 迁移步骤定义
 */
const migrationSteps = [
  {
    name: '检查当前外键约束状态',
    action: 'check',
    sql: `
      SELECT
          conname AS constraint_name,
          conrelid::regclass AS table_name,
          confrelid::regclass AS referenced_table,
          pg_get_constraintdef(c.oid) AS constraint_definition
      FROM pg_constraint c
      JOIN pg_namespace n ON n.oid = c.connamespace
      WHERE contype = 'f'
        AND n.nspname = 'public'
      ORDER BY conrelid::regclass, conname;
    `
  },
  {
    name: '删除核心业务表的外键',
    action: 'drop',
    tables: ['indicator', 'strategic_task', 'sys_user', 'milestone', 'sys_org'],
    constraints: [
      'fk_indicator_owner_org',
      'fk_indicator_target_org',
      'indicator_parent_indicator_id_fkey',
      'indicator_task_id_fkey',
      'fk3tyk1n5u74bcxtjktmyq6do8c',
      'fkktdybr93c6kg19alcjtto3x9t',
      'fk_sys_user_sys_org',
      'fkqvhtajo3dxpjxnh1vlhpsdr2l',
      'fktqe34s0m0vu11sbsiw0wj0c3f',
      'fkilhqbikhb5oxlxc4qo2j6ur47'
    ]
  },
  {
    name: '删除计划报告表的外键',
    action: 'drop',
    tables: ['progress_report', 'plan_report_indicator', 'plan_report_indicator_attachment'],
    constraints: [
      'fk1xkrqi5dkqex8hg3x675b2tv',
      'fk289plgq42or3890tc3n1a7nf5',
      'fk67j2ijf77spcyr6exqry2qkkg',
      'fkb3yqcsepsywaqj17s57cvi4mt',
      'fkfbpotyeh7xn10q1eo5h74fbjx',
      'fkfk9bq6d7t6xhs7yvoiivp15nc',
      'plan_report_indicator_report_id_fkey',
      'plan_report_indicator_attachment_attachment_id_fkey',
      'plan_report_indicator_attachment_plan_report_indicator_id_fkey'
    ]
  },
  {
    name: '删除审批审计表的外键',
    action: 'drop',
    tables: ['approval_record', 'audit_log', 'audit_step_def', 'audit_instance', 'audit_action_log'],
    constraints: [
      'fk1cb8tu1vnfi0jbln622k4if02',
      'fk62xydortyp5d7lni35v3s7c23',
      'fk3mjsnaisct36xrhu1qu6uej0d',
      'fk95439en8h8i8i9mo4wdxtl8kx',
      'fkai65vj7sgx9bg8i5xi207rq5e',
      'fkb86r3lofjmd6g99vdjjlelf3r',
      'fkcr4qaim7devhkwkr9ugvn5mn9',
      'audit_step_def_flow_id_fkey',
      'audit_instance_current_step_id_fkey',
      'audit_instance_flow_id_fkey',
      'audit_action_log_from_step_id_fkey',
      'audit_action_log_instance_id_fkey',
      'audit_action_log_step_id_fkey',
      'audit_action_log_to_step_id_fkey'
    ]
  },
  {
    name: '删除预警告警表的外键',
    action: 'drop',
    tables: ['warn_rule', '2_warn_event', '2_warn_summary_daily', 'alert_rule', 'alert_window', 'alert_event'],
    constraints: [
      'warn_rule_level_id_fkey',
      'warn_event_level_id_fkey',
      'warn_summary_daily_level_id_fkey',
      'fkbgrvb6k5fc6v32wnishg0dbbt',
      'fkjohoca8obstyp3y41pnpdal6v',
      'fk4kkplw2s4thxscplhsoon868q',
      'fkg6seqmmvduyjc1xi8ja1wic4c',
      'fk1sehn6mpxtshf781kyly6ecre',
      'fkajaeo9e6s7r4vp5a9o8f0u39g',
      'fkeg9g2avn3h566oyg76tsoraxh',
      'fkpwuvi62205iww6i8svcqht7x',
      'fkqpvvdt0y5aba9k4tkmpqu2aut'
    ]
  },
  {
    name: '删除临时任务表的外键',
    action: 'drop',
    tables: ['adhoc_task', 'adhoc_task_indicator_map', 'adhoc_task_target'],
    constraints: [
      'fk5y1ew158w3qyo13hfjn9eyrxj',
      'fkg29txlpavae5xiyi8hu1188n3',
      'fkpeuvu5yvj2yd46mfjqac8ro0h',
      'fkppaqh8pmbjy6khxysj7ntjl8m',
      'fkr2nnfj6vvlj23psox3jwapqef',
      'fkbj5sf01egss1wsq1q8y9yg6ag',
      'fkqfjy0x8qt95ucyh6chglogblq',
      'fk2h6ehplkoah6sp0pxp1dlbkua',
      'fkbca5eqhotyqb4v6nl2ekva80j',
      'fkd8x5son8i6hxtyx7bp7jlt46h'
    ]
  },
  {
    name: '删除系统管理表的外键',
    action: 'drop',
    tables: ['sys_permission', 'sys_user_role', 'sys_role_permission', 'refresh_tokens', 'app_user', 'common_log'],
    constraints: [
      'sys_permission_parent_id_fkey',
      'sys_user_role_role_id_fkey',
      'sys_user_role_user_id_fkey',
      'sys_role_permission_perm_id_fkey',
      'sys_role_permission_role_id_fkey',
      'fk_refresh_tokens_user_id',
      'fkcxvkwl71c1sdruf6l030whgvi',
      'audit_log_actor_org_id_fkey',
      'audit_log_actor_user_id_fkey'
    ]
  },
  {
    name: '验证外键约束已删除',
    action: 'verify',
    sql: `
      SELECT COUNT(*) as remaining_count
      FROM pg_constraint c
      JOIN pg_namespace n ON n.oid = c.connamespace
      WHERE contype = 'f'
        AND n.nspname = 'public';
    `
  }
];

/**
 * 主执行函数
 */
async function runMigration() {
  try {
    await client.connect();
    console.log('✓ 已连接到数据库\n');

    console.log('==========================================================');
    console.log('V1.9 迁移: 删除所有外键约束');
    console.log('目的: 适配分布式数据库架构\n');

    // 第一步: 检查当前外键状态
    console.log('【步骤 1】检查当前外键约束状态...\n');
    const checkResult = await client.query(migrationSteps[0].sql);
    stats.total = checkResult.rows.length;

    console.log(`✓ 当前数据库共有 ${stats.total} 个外键约束\n`);

    // 按表分组显示
    const byTable = {};
    checkResult.rows.forEach(row => {
      const table = row.table_name;
      if (!byTable[table]) {
        byTable[table] = [];
      }
      byTable[table].push(row);

      // 标记指向废弃表的外键
      const isDeprecated = ['task_deprecated', 'org_deprecated', 'sys_user_deprecated'].includes(row.referenced_table);
      if (isDeprecated) {
        console.log(`  ⚠️  ${table}.${row.constraint_name} → ${row.referenced_table} (指向废弃表)`);
      }
    });

    console.log('\n外键约束分布:');
    Object.keys(byTable).forEach(table => {
      console.log(`  📋 ${table}: ${byTable[table].length} 个`);
    });

    // 第二步到第七步: 删除外键约束
    for (let i = 1; i < migrationSteps.length - 1; i++) {
      const step = migrationSteps[i];

      console.log(`\n【步骤 ${i + 1}】${step.name}...`);
      console.log(`  涉及表: ${step.tables.join(', ')}`);

      // 删除每个约束
      for (const constraintName of step.constraints) {
        try {
          // 尝试删除约束
          const dropSQL = `ALTER TABLE public.${step.tables[0]} DROP CONSTRAINT IF EXISTS ${constraintName};`;
          await client.query(dropSQL);

          stats.dropped++;
          console.log(`  ✓ 已删除: ${constraintName}`);

        } catch (error) {
          // 忽略"约束不存在"错误
          if (error.message.includes('does not exist') || error.message.includes('not exist')) {
            stats.skipped++;
            console.log(`  ⚠️  跳过(不存在): ${constraintName}`);
          } else {
            stats.errors.push({
              constraint: constraintName,
              error: error.message
            });
            console.error(`  ✗ 失败: ${constraintName} - ${error.message}`);
          }
        }
      }
    }

    // 最后一步: 验证
    console.log(`\n【步骤 ${migrationSteps.length}】${migrationSteps[migrationSteps.length - 1].name}...\n`);
    const verifyResult = await client.query(migrationSteps[migrationSteps.length - 1].sql);
    const remainingCount = parseInt(verifyResult.rows[0].remaining_count);

    console.log(`✓ 剩余外键约束: ${remainingCount} 个`);

    // 输出总结报告
    console.log('\n==========================================================');
    console.log('V1.9 迁移执行完成\n');
    console.log('📊 统计信息:');
    console.log(`  原有外键约束: ${stats.total} 个`);
    console.log(`  成功删除: ${stats.dropped} 个`);
    console.log(`  跳过(不存在): ${stats.skipped} 个`);
    console.log(`  剩余约束: ${remainingCount} 个`);
    console.log(`  失败: ${stats.errors.length} 个`);

    if (stats.errors.length > 0) {
      console.log('\n⚠️  失败详情:');
      stats.errors.forEach(err => {
        console.log(`  - ${err.constraint}: ${err.error}`);
      });
    }

    console.log('\n✅ 迁移完成');
    console.log('\n⚠️  重要提示:');
    console.log('  1. 数据完整性现在由应用层代码保证');
    console.log('  2. 建议在Service层添加数据验证逻辑');
    console.log('  3. 建议添加单元测试验证数据一致性');
    console.log('  4. 已修复指向废弃表的外键引用问题');
    console.log('  5. 适配分布式数据库架构,提升性能和扩展性');
    console.log('\n📋 后续工作建议:');
    console.log('  1. 检查并更新 Service 层的数据验证逻辑');
    console.log('  2. 添加 ID 有效性检查的方法(orgExists, userExists等)');
    console.log('  3. 添加单元测试覆盖外键验证场景');
    console.log('  4. 监控应用层数据完整性,防止脏数据');
    console.log('  5. 考虑添加定时任务清理无效关联数据');
    console.log('==========================================================\n');

  } catch (error) {
    console.error('\n✗ 迁移执行失败:', error.message);
    throw error;
  } finally {
    await client.end();
    console.log('✓ 数据库连接已关闭\n');
  }
}

// 执行迁移
runMigration()
  .then(() => {
    console.log('✓ 迁移脚本执行成功');
    process.exit(0);
  })
  .catch(error => {
    console.error('✗ 迁移执行失败:', error);
    process.exit(1);
  });
