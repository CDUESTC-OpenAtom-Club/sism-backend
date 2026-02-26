/**
 * 添加二级学院测试用户脚本
 * 用于在生产数据库中创建二级学院测试账号
 * 
 * 执行: node scripts/add-college-users.js
 */

import pg from 'pg';
import { dbConfig, validateConfig } from './config.js';

const { Pool } = pg;

// 密码: 123456 的 bcrypt 哈希值
const PASSWORD_HASH = '$2a$10$N9qo8uLOickgx2ZMRZoMy.MqrqQlBNe/YnHOl4EKMry1xLzJypGGi';

// 二级学院用户配置
const collegeUsers = [
  { username: 'makesi', realName: '马克思主义学院测试用户', orgId: 55 },
  { username: 'gongxue', realName: '工学院测试用户', orgId: 56 },
  { username: 'jisuanji', realName: '计算机学院测试用户', orgId: 57 },
  { username: 'shangxue', realName: '商学院测试用户', orgId: 58 },
  { username: 'wenli', realName: '文理学院测试用户', orgId: 59 },
  { username: 'yishu', realName: '艺术与科技学院测试用户', orgId: 60 },
  { username: 'hangkong', realName: '航空学院测试用户', orgId: 61 },
  { username: 'guojijiaoyu', realName: '国际教育学院测试用户', orgId: 62 }
];

async function addCollegeUsers() {
  console.log('========================================');
  console.log('  二级学院用户添加脚本');
  console.log('========================================\n');

  // 验证配置
  if (!validateConfig()) {
    process.exit(1);
  }

  const pool = new Pool(dbConfig);

  try {
    // 测试连接
    console.log('[1/4] 连接数据库...');
    const client = await pool.connect();
    console.log('✓ 数据库连接成功\n');

    // 检查二级学院组织
    console.log('[2/4] 检查二级学院组织...');
    const orgResult = await client.query(
      "SELECT org_id, org_name FROM org WHERE org_type = 'COLLEGE' ORDER BY org_id"
    );
    console.log(`✓ 找到 ${orgResult.rows.length} 个二级学院\n`);

    // 检查现有用户
    console.log('[3/4] 检查现有用户...');
    const existingUsers = await client.query(
      "SELECT username FROM app_user WHERE username = ANY($1)",
      [collegeUsers.map(u => u.username)]
    );
    const existingUsernames = new Set(existingUsers.rows.map(r => r.username));
    console.log(`✓ 已存在 ${existingUsernames.size} 个用户\n`);

    // 添加用户
    console.log('[4/4] 添加二级学院用户...');
    let addedCount = 0;
    let skippedCount = 0;

    for (const user of collegeUsers) {
      if (existingUsernames.has(user.username)) {
        console.log(`  ⏭ ${user.username} (${user.realName}) - 已存在，跳过`);
        skippedCount++;
        continue;
      }

      try {
        await client.query(
          `INSERT INTO app_user (username, real_name, org_id, password_hash, is_active)
           VALUES ($1, $2, $3, $4, TRUE)`,
          [user.username, user.realName, user.orgId, PASSWORD_HASH]
        );
        console.log(`  ✓ ${user.username} (${user.realName}) - 添加成功`);
        addedCount++;
      } catch (err) {
        console.log(`  ✗ ${user.username} - 添加失败: ${err.message}`);
      }
    }

    client.release();

    // 验证结果
    console.log('\n========================================');
    console.log('  执行结果');
    console.log('========================================');
    console.log(`新增用户: ${addedCount}`);
    console.log(`跳过用户: ${skippedCount}`);
    console.log(`总计: ${addedCount + skippedCount}\n`);

    // 显示所有二级学院用户
    const verifyResult = await pool.query(`
      SELECT u.user_id, u.username, u.real_name, o.org_name, u.is_active
      FROM app_user u
      JOIN org o ON u.org_id = o.org_id
      WHERE o.org_type = 'COLLEGE'
      ORDER BY u.user_id
    `);

    console.log('当前二级学院用户列表:');
    console.table(verifyResult.rows);

    console.log('\n测试账号信息:');
    console.log('密码: 123456');
    console.log('登录地址: https://blackevil.cn/login\n');

  } catch (err) {
    console.error('执行失败:', err.message);
    process.exit(1);
  } finally {
    await pool.end();
  }
}

addCollegeUsers();
