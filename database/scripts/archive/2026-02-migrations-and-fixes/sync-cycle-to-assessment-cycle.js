#!/usr/bin/env node

/**
 * 同步 cycle 表数据到 assessment_cycle 表
 * 
 * 问题：strategic_task 表的 cycle_id 外键指向 assessment_cycle 表，
 *      但实际周期数据在 cycle 表中
 * 
 * 解决：将 cycle 表的数据复制到 assessment_cycle 表
 */

const { Client } = require('pg');

const client = new Client({
  host: '175.24.139.148',
  port: 8386,
  database: 'strategic',
  user: 'muzimu',
  password: '64378561huaW',
});

async function syncCycleData() {
  try {
    await client.connect();
    console.log('✅ 数据库连接成功\n');

    // 1. 检查 cycle 表数据
    console.log('📊 检查 cycle 表数据...');
    const cycleResult = await client.query('SELECT * FROM cycle ORDER BY id');
    console.log(`   找到 ${cycleResult.rows.length} 条周期记录：`);
    cycleResult.rows.forEach(row => {
      console.log(`   - ID ${row.id}: ${row.cycle_name} (${row.year})`);
    });

    // 2. 检查 assessment_cycle 表数据
    console.log('\n📊 检查 assessment_cycle 表数据...');
    const assessmentResult = await client.query('SELECT * FROM assessment_cycle ORDER BY cycle_id');
    console.log(`   当前有 ${assessmentResult.rows.length} 条记录`);

    // 3. 同步数据
    console.log('\n🔄 开始同步数据...');
    const syncQuery = `
      INSERT INTO assessment_cycle (cycle_id, cycle_name, year, start_date, end_date, description, created_at, updated_at)
      SELECT id, cycle_name, year, start_date, end_date, description, created_at, updated_at
      FROM cycle
      ON CONFLICT (cycle_id) DO UPDATE SET
        cycle_name = EXCLUDED.cycle_name,
        year = EXCLUDED.year,
        start_date = EXCLUDED.start_date,
        end_date = EXCLUDED.end_date,
        description = EXCLUDED.description,
        updated_at = EXCLUDED.updated_at
      RETURNING cycle_id, cycle_name;
    `;
    
    const syncResult = await client.query(syncQuery);
    console.log(`✅ 成功同步 ${syncResult.rows.length} 条记录：`);
    syncResult.rows.forEach(row => {
      console.log(`   - ID ${row.cycle_id}: ${row.cycle_name}`);
    });

    // 4. 验证同步结果
    console.log('\n✅ 验证同步结果...');
    const verifyResult = await client.query('SELECT cycle_id, cycle_name, year FROM assessment_cycle ORDER BY cycle_id');
    console.log(`   assessment_cycle 表现在有 ${verifyResult.rows.length} 条记录`);

    console.log('\n✅ 周期数据同步完成！');
    console.log('   现在可以执行 V1.8 迁移了');

  } catch (error) {
    console.error('❌ 错误:', error.message);
    throw error;
  } finally {
    await client.end();
  }
}

// 执行同步
syncCycleData().catch(err => {
  console.error('同步失败:', err);
  process.exit(1);
});
