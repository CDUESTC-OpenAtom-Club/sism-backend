#!/usr/bin/env node

/**
 * 删除 app_user 表(已废弃)
 */

const { Client } = require('pg');

const client = new Client({
  host: '175.24.139.148',
  port: 8386,
  database: 'strategic',
  user: 'muzimu',
  password: '64378561huaW'
});

async function dropAppUserTable() {
  try {
    await client.connect();
    console.log('✓ 已连接到数据库\n');

    // 检查 app_user 表
    console.log('【检查】app_user 表...\n');

    const checkSQL = `SELECT EXISTS (SELECT FROM pg_tables WHERE tablename = 'app_user')`;
    const checkResult = await client.query(checkSQL);
    const exists = checkResult.rows[0].exists;

    if (!exists) {
      console.log('✅ app_user 表不存在或已删除\n');
      return;
    }

    console.log('⚠️  找到 app_user 表,准备删除...\n');

    // 删除表
    console.log('【删除】app_user 表...\n');
    const dropSQL = 'DROP TABLE IF EXISTS public.app_user CASCADE;';
    await client.query(dropSQL);

    console.log('✓ 已删除 app_user 表\n');

    // 验证
    const verifyResult = await client.query(checkSQL);
    if (verifyResult.rows[0].exists) {
      console.log('⚠️  警告: app_user 表仍然存在\n');
    } else {
      console.log('✅ 验证通过: app_user 表已删除\n');
    }

  } catch (error) {
    console.error('\n✗ 删除失败:', error.message);
    throw error;
  } finally {
    await client.end();
    console.log('✓ 数据库连接已关闭\n');
  }
}

dropAppUserTable()
  .then(() => {
    console.log('✓ 删除 app_user 表脚本执行成功');
    process.exit(0);
  })
  .catch(error => {
    console.error('✗ 删除 app_user 表脚本执行失败:', error);
    process.exit(1);
  });
