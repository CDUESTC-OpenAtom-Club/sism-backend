/**
 * 修复二级学院用户密码
 * 使用与admin相同的密码哈希
 */

import pg from 'pg';
import { dbConfig, validateConfig } from './config.js';

const { Pool } = pg;

// 使用与admin相同的密码哈希 (123456)
const CORRECT_PASSWORD_HASH = '$2a$10$UF.UUADlBmXZU1tU3iec3OK5lfK4TOvVxErggE0HGPguRTiOO/dmi';

const collegeUsernames = ['makesi', 'gongxue', 'jisuanji', 'shangxue', 'wenli', 'yishu', 'hangkong', 'guojijiaoyu'];

async function fixPasswords() {
  console.log('修复二级学院用户密码...\n');

  if (!validateConfig()) {
    process.exit(1);
  }

  const pool = new Pool(dbConfig);

  try {
    const result = await pool.query(
      `UPDATE app_user 
       SET password_hash = $1 
       WHERE username = ANY($2)
       RETURNING username`,
      [CORRECT_PASSWORD_HASH, collegeUsernames]
    );

    console.log(`✓ 已更新 ${result.rowCount} 个用户的密码\n`);
    result.rows.forEach(r => console.log(`  - ${r.username}`));

    // 验证
    const verify = await pool.query(
      `SELECT username, LEFT(password_hash, 30) as pwd_prefix 
       FROM app_user WHERE username = ANY($1)`,
      [collegeUsernames]
    );
    console.log('\n验证结果:');
    console.table(verify.rows);

  } catch (err) {
    console.error('执行失败:', err.message);
    process.exit(1);
  } finally {
    await pool.end();
  }
}

fixPasswords();
