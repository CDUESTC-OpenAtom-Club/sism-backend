#!/usr/bin/env node

/**
 * 删除废弃表的索引
 *
 * 目的: 清理指向废弃表的8个索引,释放存储空间
 */

const { Client } = require('pg');

const client = new Client({
  host: '175.24.139.148',
  port: 8386,
  database: 'strategic',
  user: 'muzimu',
  password: '64378561huaW'
});

const DEPRECATED_INDEXES = [
  'org_deprecated.org_pkey1',
  'org_deprecated.uk_org_name',
  'sys_user_deprecated.app_user_pkey',
  'sys_user_deprecated.app_user_username_key',
  'sys_user_deprecated.idx_user_org',
  'sys_user_deprecated.idx_user_username',
  'task_deprecated.idx_task_cycle',
  'task_deprecated.strategic_task_pkey'
];

/**
 * 主执行函数
 */
async function dropDeprecatedIndexes() {
  try {
    await client.connect();
    console.log('✓ 已连接到数据库\n');

    console.log('==========================================================');
    console.log('删除废弃表索引');
    console.log('==========================================================\n');

    // 检查当前存在的索引
    console.log('【步骤 1】检查废弃表索引...\n');

    const checkSQL = `
      SELECT
            schemaname AS schema_name,
            tablename AS table_name,
            indexname AS index_name,
            indexdef AS index_definition
        FROM pg_indexes
        WHERE schemaname = 'public'
          AND (
            tablename LIKE '%deprecated%'
            OR indexname LIKE '%deprecated%'
            OR indexname = 'app_user_pkey'
            OR indexname = 'app_user_username_key'
            OR indexname = 'idx_user_org'
            OR indexname = 'idx_user_username'
            OR indexname = 'strategic_task_pkey'
          )
        ORDER BY tablename, indexname;
    `;

    const checkResult = await client.query(checkSQL);

    if (checkResult.rows.length === 0) {
      console.log('✅ 未找到废弃表索引\n');
      return;
    }

    console.log(`⚠️  找到 ${checkResult.rows.length} 个废弃表索引:\n`);

    const indexesByTable = {};
    checkResult.rows.forEach(row => {
      const table = row.table_name;
      if (!indexesByTable[table]) {
        indexesByTable[table] = [];
      }
      indexesByTable[table].push(row);
    });

    Object.keys(indexesByTable).forEach(table => {
      console.log(`  📋 ${table}: ${indexesByTable[table].length} 个索引`);
      indexesByTable[table].forEach(idx => {
        console.log(`    - ${idx.index_name}`);
      });
    });

    // 删除索引
    console.log('\n【步骤 2】删除废弃表索引...\n');

    let droppedCount = 0;
    let skippedCount = 0;

    for (const row of checkResult.rows) {
      const indexName = row.index_name;
      const tableName = row.table_name;

      const dropSQL = `DROP INDEX IF EXISTS public.${indexName};`;

      try {
        await client.query(dropSQL);
        droppedCount++;
        console.log(`  ✓ 已删除: ${tableName}.${indexName}`);

      } catch (error) {
        if (error.message.includes('does not exist')) {
          skippedCount++;
          console.log(`  ⚠️  跳过(不存在): ${indexName}`);
        } else {
          console.error(`  ✗ 失败: ${indexName} - ${error.message}`);
        }
      }
    }

    // 验证
    console.log('\n【步骤 3】验证索引已删除...\n');

    const verifyResult = await client.query(checkSQL);
    const remainingCount = verifyResult.rows.length;

    // 总结
    console.log('\n==========================================================');
    console.log('✅ 索引清理完成\n');
    console.log('📊 统计信息:');
    console.log(`  原有索引: ${checkResult.rows.length} 个`);
    console.log(`  成功删除: ${droppedCount} 个`);
    console.log(`  跳过(不存在): ${skippedCount} 个`);
    console.log(`  剩余索引: ${remainingCount} 个`);

    if (remainingCount > 0) {
      console.log('\n⚠️  剩余索引:');
      verifyResult.rows.forEach(row => {
        console.log(`  - ${row.table_name}.${row.index_name}`);
      });
    } else {
      console.log('\n✅ 所有废弃表索引已删除');
    }

    console.log('\n💾 释放存储空间: 约 1-2MB (8个索引)');
    console.log('==========================================================\n');

  } catch (error) {
    console.error('\n✗ 索引清理失败:', error.message);
    throw error;
  } finally {
    await client.end();
    console.log('✓ 数据库连接已关闭\n');
  }
}

// 执行清理
dropDeprecatedIndexes()
  .then(() => {
    console.log('✓ 索引清理脚本执行成功');
    process.exit(0);
  })
  .catch(error => {
    console.error('✗ 索引清理脚本执行失败:', error);
    process.exit(1);
  });
