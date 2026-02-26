#!/usr/bin/env node

/**
 * 修复 indicator 表中 target_org_id 为 NULL 的记录
 * 将 NULL 值设置为 owner_org_id（假设目标组织与所有者组织相同）
 */

const { Client } = require('pg');

const client = new Client({
  host: '175.24.139.148',
  port: 8386,
  database: 'strategic',
  user: 'muzimu',
  password: '64378561huaW'
});

async function fixNullTargetOrg() {
  try {
    await client.connect();
    console.log('✓ 已连接到数据库');

    // 1. 检查有多少记录的 target_org_id 为 NULL
    const checkResult = await client.query(`
      SELECT COUNT(*) as null_count
      FROM indicator
      WHERE target_org_id IS NULL
    `);
    
    const nullCount = parseInt(checkResult.rows[0].null_count);
    console.log(`\n发现 ${nullCount} 条记录的 target_org_id 为 NULL`);

    if (nullCount === 0) {
      console.log('✓ 无需修复');
      return;
    }

    // 2. 显示这些记录的详情
    const detailsResult = await client.query(`
      SELECT indicator_id, indicator_desc, owner_org_id, target_org_id
      FROM indicator
      WHERE target_org_id IS NULL
      LIMIT 10
    `);
    
    console.log('\n前10条需要修复的记录:');
    detailsResult.rows.forEach(row => {
      console.log(`  - ID: ${row.indicator_id}, 描述: ${row.indicator_desc}, owner_org_id: ${row.owner_org_id}`);
    });

    // 3. 修复：将 target_org_id 设置为 owner_org_id
    console.log('\n开始修复...');
    const updateResult = await client.query(`
      UPDATE indicator
      SET target_org_id = owner_org_id
      WHERE target_org_id IS NULL
      RETURNING indicator_id, indicator_desc, owner_org_id, target_org_id
    `);

    console.log(`✓ 已修复 ${updateResult.rowCount} 条记录`);
    
    if (updateResult.rowCount > 0 && updateResult.rowCount <= 10) {
      console.log('\n修复后的记录:');
      updateResult.rows.forEach(row => {
        console.log(`  - ID: ${row.indicator_id}, target_org_id: ${row.target_org_id}`);
      });
    }

    // 4. 验证修复结果
    const verifyResult = await client.query(`
      SELECT COUNT(*) as null_count
      FROM indicator
      WHERE target_org_id IS NULL
    `);
    
    const remainingNull = parseInt(verifyResult.rows[0].null_count);
    console.log(`\n验证: 剩余 ${remainingNull} 条 NULL 记录`);
    
    if (remainingNull === 0) {
      console.log('✓ 所有 NULL 值已修复');
    } else {
      console.log('⚠ 仍有 NULL 值需要手动处理');
    }

  } catch (error) {
    console.error('✗ 错误:', error.message);
    throw error;
  } finally {
    await client.end();
    console.log('\n✓ 数据库连接已关闭');
  }
}

// 执行修复
fixNullTargetOrg()
  .then(() => {
    console.log('\n✓ 修复完成');
    process.exit(0);
  })
  .catch(error => {
    console.error('\n✗ 修复失败:', error);
    process.exit(1);
  });
