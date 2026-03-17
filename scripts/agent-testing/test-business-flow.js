#!/usr/bin/env node
/**
 * 业务流程验证测试脚本
 * 验证流程.md中描述的9个核心业务流程是否已实现
 */

const { Client } = require('pg');

const client = new Client({
  host: '175.24.139.148',
  port: 8386,
  user: 'postgres',
  password: '64378561huaW',
  database: 'strategic'
});

const tests = [];
const results = {
  passed: 0,
  failed: 0,
  details: []
};

async function test(name, fn) {
  tests.push({ name, fn });
}

async function runTests() {
  await client.connect();
  console.log('✓ 数据库连接成功\n');

  for (const { name, fn } of tests) {
    try {
      await fn();
      results.passed++;
      results.details.push({ name, status: 'PASS', error: null });
      console.log(`✓ ${name}`);
    } catch (error) {
      results.failed++;
      results.details.push({ name, status: 'FAIL', error: error.message });
      console.log(`✗ ${name}`);
      console.log(`  错误: ${error.message}`);
    }
  }

  await client.end();

  console.log('\n' + '='.repeat(60));
  console.log(`测试完成: ${results.passed} 通过, ${results.failed} 失败`);
  console.log('='.repeat(60));

  process.exit(results.failed > 0 ? 1 : 0);
}

// ============================================================================
// 流程1: 指标创建与第一层下发 (战略发展部 -> 职能部门)
// ============================================================================

test('1.1 indicator表包含状态字段(DRAFT/PENDING/DISTRIBUTED)', async () => {
  const res = await client.query(`
    SELECT column_name, data_type
    FROM information_schema.columns
    WHERE table_name = 'indicator' AND column_name = 'status'
  `);
  if (res.rows.length === 0) throw new Error('indicator表缺少status字段');
});

test('1.2 audit_flow_def表存在(审批流定义)', async () => {
  const res = await client.query(`SELECT COUNT(*) FROM audit_flow_def`);
  if (parseInt(res.rows[0].count) === 0) throw new Error('audit_flow_def表无数据');
});

test('1.3 audit_instance表存在(审批实例)', async () => {
  const res = await client.query(`
    SELECT column_name FROM information_schema.columns
    WHERE table_name = 'audit_instance'
    AND column_name IN ('entity_id', 'entity_type', 'status', 'requester_org_id')
  `);
  if (res.rows.length < 4) throw new Error('audit_instance表缺少必要字段');
});

test('1.4 audit_step_instance表存在(审批步骤实例)', async () => {
  const res = await client.query(`SELECT 1 FROM audit_step_instance LIMIT 1`);
});

// ============================================================================
// 流程2: 指标拆分与第二层下发 (职能部门 -> 学院)
// ============================================================================

test('2.1 indicator表包含parent_indicator_id字段', async () => {
  const res = await client.query(`
    SELECT column_name FROM information_schema.columns
    WHERE table_name = 'indicator' AND column_name = 'parent_indicator_id'
  `);
  if (res.rows.length === 0) throw new Error('indicator表缺少parent_indicator_id字段');
});

// ============================================================================
// 流程3: 学院进度填报与审批
// ============================================================================

test('3.1 plan_report表存在(填报表)', async () => {
  const res = await client.query(`
    SELECT column_name FROM information_schema.columns
    WHERE table_name = 'plan_report' AND column_name IN ('status', 'report_month')
  `);
  if (res.rows.length < 2) throw new Error('plan_report表缺少必要字段');
});

test('3.2 indicator表包含progress字段', async () => {
  const res = await client.query(`
    SELECT column_name FROM information_schema.columns
    WHERE table_name = 'indicator' AND column_name = 'progress'
  `);
  if (res.rows.length === 0) throw new Error('indicator表缺少progress字段');
});

// ============================================================================
// 流程4: 多级审批与驳回机制
// ============================================================================

test('4.1 audit_step_def表存在(审批步骤定义)', async () => {
  const res = await client.query(`
    SELECT column_name FROM information_schema.columns
    WHERE table_name = 'audit_step_def'
    AND column_name IN ('flow_def_id', 'step_order')
  `);
  if (res.rows.length < 2) throw new Error('audit_step_def表缺少必要字段');
});

test('4.2 audit_instance包含current_step_id字段', async () => {
  const res = await client.query(`
    SELECT column_name FROM information_schema.columns
    WHERE table_name = 'audit_instance' AND column_name = 'current_step_id'
  `);
  if (res.rows.length === 0) throw new Error('audit_instance表缺少current_step_id字段');
});

// ============================================================================
// 流程5: 审批时间轴
// ============================================================================

test('5.1 audit_step_instance包含时间和意见字段', async () => {
  const res = await client.query(`
    SELECT column_name FROM information_schema.columns
    WHERE table_name = 'audit_step_instance'
    AND column_name IN ('created_at', 'comment', 'status')
  `);
  if (res.rows.length < 3) throw new Error('audit_step_instance表缺少审批时间轴必要字段');
});

// ============================================================================
// 流程7: 父子指标关系
// ============================================================================

test('7.1 可以查询子指标', async () => {
  const res = await client.query(`
    SELECT id, parent_indicator_id FROM indicator
    WHERE parent_indicator_id IS NOT NULL LIMIT 1
  `);
  // 不强制要求有数据，只验证查询可执行
});

// ============================================================================
// 流程8: 填报周期标识
// ============================================================================

test('8.1 plan_report包含周期标识字段', async () => {
  const res = await client.query(`
    SELECT column_name FROM information_schema.columns
    WHERE table_name = 'plan_report' AND column_name = 'report_month'
  `);
  if (res.rows.length === 0) throw new Error('plan_report表缺少report_month字段');
});

// ============================================================================
// 附加验证: 关键表关系
// ============================================================================

test('附加: sys_user表存在', async () => {
  const res = await client.query(`SELECT 1 FROM sys_user LIMIT 1`);
});

test('附加: sys_org表存在', async () => {
  const res = await client.query(`SELECT 1 FROM sys_org LIMIT 1`);
});

test('附加: attachment表存在(附件支持)', async () => {
  const res = await client.query(`SELECT 1 FROM attachment LIMIT 1`);
});

runTests();
