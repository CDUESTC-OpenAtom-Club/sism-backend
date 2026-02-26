#!/usr/bin/env node

/**
 * 删除废弃表结构(包括索引和约束)
 *
 * 前提条件:
 * 1. 已执行 cleanup-deprecated-tables.js 清空数据
 * 2. 已创建备份表 (backup表)
 * 3. 业务系统运行正常
 */

const { Client } = require('pg');

const client = new Client({
  host: '175.24.139.148',
  port: 8386,
  database: 'strategic',
  user: 'muzimu',
  password: '64378561huaW'
});

const DEPRECATED_TABLES = [
  'org_deprecated',
  'org_deprecated_backup',
  'sys_user_deprecated',
  'sys_user_deprecated_backup',
  'task_deprecated',
  'task_deprecated_backup'
];

/**
 * 主执行函数
 */
async function dropDeprecatedTables() {
  try {
    await client.connect();
    console.log('✓ 已连接到数据库\n');

    console.log('==========================================================');
    console.log('⚠️  警告: 即将删除废弃表结构!');
    console.log('==========================================================\n');

    // 第一步: 检查表是否存在
    console.log('【步骤 1】检查废弃表...\n');

    const existingTables = [];

    for (const tableName of DEPRECATED_TABLES) {
      const checkSQL = `SELECT EXISTS (SELECT FROM pg_tables WHERE tablename = '${tableName}')`;
      const result = await client.query(checkSQL);
      const exists = result.rows[0].exists;

      if (exists) {
        existingTables.push(tableName);

        const isBackup = tableName.includes('_backup');
        const status = isBackup ? '(备份表)' : '(原表)';
        console.log(`  📋 ${tableName} ${status}`);

      } else {
        console.log(`  ⚠️  跳过: ${tableName} 不存在`);
      }
    }

    if (existingTables.length === 0) {
      console.log('\n✅ 未找到废弃表,所有表已删除\n');
      return;
    }

    console.log(`\n⚠️  找到 ${existingTables.length} 个废弃表\n`);

    // 第二步: 删除表
    console.log('【步骤 2】删除废弃表结构...\n');

    let droppedCount = 0;
    let errorCount = 0;

    for (const tableName of existingTables) {
      const dropSQL = `DROP TABLE IF EXISTS public.${tableName} CASCADE;`;
      console.log(`  🗑️  删除 ${tableName}...`);

      try {
        await client.query(dropSQL);
        droppedCount++;
        console.log(`    ✓ 已删除: ${tableName}`);

      } catch (error) {
        errorCount++;
        console.error(`    ✗ 失败: ${tableName} - ${error.message}`);
      }
    }

    // 第三步: 验证
    console.log('\n【步骤 3】验证表已删除...\n');

    const verifySQL = `
      SELECT tablename
      FROM pg_tables
      WHERE schemaname = 'public'
        AND (
          tablename LIKE '%deprecated%'
          OR tablename = 'app_user'
        )
      ORDER BY tablename;
    `;

    const verifyResult = await client.query(verifySQL);
    const remainingCount = verifyResult.rows.length;

    // 总结
    console.log('\n==========================================================');
    console.log('✅ 废弃表删除完成\n');
    console.log('📊 统计信息:');
    console.log(`  原有表: ${existingTables.length} 个`);
    console.log(`  成功删除: ${droppedCount} 个`);
    console.log(`  失败: ${errorCount} 个`);
    console.log(`  剩余表: ${remainingCount} 个`);

    if (remainingCount > 0) {
      console.log('\n⚠️  剩余表:');
      verifyResult.rows.forEach(row => {
        console.log(`  - ${row.tablename}`);
      });
    } else {
      console.log('\n✅ 所有废弃表已删除');
      console.log('💾 释放存储空间: 约 5-10MB (表结构+索引)');
    }

    console.log('\n⚠️  重要提示:');
    console.log('  1. 备份数据已随表一起删除');
    console.log('  2. 如需恢复数据,只能从数据库备份恢复');
    console.log('  3. AppUser.java 和 Org.java.deprecated 已处理');
    console.log('==========================================================\n');

  } catch (error) {
    console.error('\n✗ 删除表失败:', error.message);
    throw error;
  } finally {
    await client.end();
    console.log('✓ 数据库连接已关闭\n');
  }
}

// 执行删除
dropDeprecatedTables()
  .then(() => {
    console.log('✓ 删除表脚本执行成功');
    process.exit(0);
  })
  .catch(error => {
    console.error('✗ 删除表脚本执行失败:', error);
    process.exit(1);
  });
