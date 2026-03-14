#!/usr/bin/env node

/**
 * 清理废弃表数据 - 谨慎执行!
 *
 * 执行前:
 * 1. 确认业务不再需要这些数据
 * 2. 已执行 V1.5, V1.7, V1.8 迁移
 * 3. strategic_task 表确实只有 4 条记录
 *
 * 执行步骤:
 * 1. 备份数据到 _backup 表
 * 2. 清空废弃表(不删除表结构,仅清空数据)
 * 3. 验证数据已清空
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
  'sys_user_deprecated',
  'task_deprecated'
];

/**
 * 主执行函数
 */
async function cleanupDeprecatedTables() {
  try {
    await client.connect();
    console.log('✓ 已连接到数据库\n');

    console.log('==========================================================');
    console.log('⚠️  警告: 即将清空废弃表数据!');
    console.log('==========================================================\n');

    // 第一步: 备份数据
    console.log('【步骤 1】备份废弃表数据...\n');

    for (const tableName of DEPRECATED_TABLES) {
      const backupTableName = `${tableName}_backup`;

      // 检查备份表是否已存在
      const checkSQL = `SELECT EXISTS (SELECT FROM pg_tables WHERE tablename = '${backupTableName}')`;
      const checkResult = await client.query(checkSQL);
      const backupExists = checkResult.rows[0].exists;

      if (backupExists) {
        console.log(`  ⚠️  跳过: ${backupTableName} 已存在,删除旧备份`);
        await client.query(`DROP TABLE IF EXISTS ${backupTableName}`);
      }

      // 创建备份表
      const backupSQL = `CREATE TABLE ${backupTableName} AS SELECT * FROM ${tableName}`;
      console.log(`  📦 备份 ${tableName} → ${backupTableName}...`);

      try {
        await client.query(backupSQL);

        // 获取备份记录数
        const countSQL = `SELECT COUNT(*) as count FROM ${backupTableName}`;
        const countResult = await client.query(countSQL);
        const count = parseInt(countResult.rows[0].count);
        console.log(`    ✓ 已备份 ${count} 条数据`);

      } catch (error) {
        if (error.message.includes('does not exist')) {
          console.log(`    ⚠️  跳过: ${tableName} 表不存在`);
        } else {
          throw error;
        }
      }
    }

    // 第二步: 清空废弃表
    console.log('\n【步骤 2】清空废弃表数据...\n');

    let totalCleaned = 0;

    for (const tableName of DEPRECATED_TABLES) {
      const truncateSQL = `TRUNCATE TABLE ${tableName}`;
      console.log(`  🗑️  清空 ${tableName}...`);

      try {
        await client.query(truncateSQL);
        console.log(`    ✓ 已清空 ${tableName}`);

      } catch (error) {
        if (error.message.includes('does not exist')) {
          console.log(`    ⚠️  跳过: ${tableName} 表不存在`);
        } else {
          throw error;
        }
      }
    }

    // 第三步: 验证数据已清空
    console.log('\n【步骤 3】验证数据已清空...\n');

    for (const tableName of DEPRECATED_TABLES) {
      const verifySQL = `SELECT COUNT(*) as count FROM ${tableName}`;
      const verifyResult = await client.query(verifySQL);
      const count = parseInt(verifyResult.rows[0].count);

      if (count === 0) {
        console.log(`  ✅ ${tableName}: 已清空 (0 条数据)`);
      } else {
        console.log(`  ⚠️  ${tableName}: 仍有 ${count} 条数据!`);
      }
    }

    // 输出总结报告
    console.log('\n==========================================================');
    console.log('✅ 废弃表数据清理完成\n');
    console.log('📊 清理统计:');
    console.log(`  已清空表: ${DEPRECATED_TABLES.join(', ')}`);
    console.log(`  已创建备份表: ${DEPRECATED_TABLES.map(t => `${t}_backup`).join(', ')}`);
    console.log('\n⚠️  后续工作:');
    console.log('  1. 验证业务系统运行正常');
    console.log('  2. 确认无需恢复备份数据后,可删除备份表');
    console.log('  3. 考虑删除废弃表结构(DROP TABLE)以释放空间');
    console.log('==========================================================\n');

  } catch (error) {
    console.error('\n✗ 清理失败:', error.message);
    throw error;
  } finally {
    await client.end();
    console.log('✓ 数据库连接已关闭\n');
  }
}

// 执行清理
cleanupDeprecatedTables()
  .then(() => {
    console.log('✓ 清理脚本执行成功');
    process.exit(0);
  })
  .catch(error => {
    console.error('✗ 清理脚本执行失败:', error);
    process.exit(1);
  });
