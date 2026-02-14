/**
 * 修复职能部门用户密码
 * 问题: 职能部门账号全部无法登录
 * 原因: 密码哈希与 admin 不一致
 * 解决: 统一使用与 admin 相同的密码哈希 (密码: 123456)
 */

import pg from 'pg';
import dotenv from 'dotenv';
import { fileURLToPath } from 'url';
import { dirname, join } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

// 加载环境变量
dotenv.config({ path: join(__dirname, '../../.env') });

const { Pool } = pg;

// 正确的密码哈希 (123456) - 与 admin 相同
const CORRECT_PASSWORD_HASH = '$2a$10$UF.UUADlBmXZU1tU3iec3OK5lfK4TOvVxErggE0HGPguRTiOO/dmi';

// 职能部门用户名列表
const functionalDeptUsernames = [
  'jiaowu',      // 教务处
  'xuegong',     // 学工处  
  'keyan',       // 科研处
  'renshichu',   // 人事处
  'caiwuchu',    // 财务处
  'zichan',      // 资产处
  'houqin',      // 后勤处
  'guoji',       // 国际处
  'xuanchuan',   // 宣传部
  'zuzhi'        // 组织部
];

async function fixPasswords() {
  console.log('🔧 修复职能部门用户密码...\n');

  const dbConfig = {
    host: process.env.DB_HOST || '175.24.139.148',
    port: parseInt(process.env.DB_PORT || '8386'),
    database: process.env.DB_NAME || 'strategic',
    user: process.env.DB_USERNAME || 'postgres',
    password: process.env.DB_PASSWORD
  };

  if (!dbConfig.password) {
    console.error('❌ 错误: 未配置数据库密码 (DB_PASSWORD)');
    process.exit(1);
  }

  const pool = new Pool(dbConfig);

  try {
    // 1. 检查当前密码状态
    console.log('📊 检查当前密码状态...');
    const checkResult = await pool.query(
      `SELECT username, real_name, LEFT(password_hash, 30) as pwd_prefix 
       FROM app_user 
       WHERE username = ANY($1)
       ORDER BY username`,
      [functionalDeptUsernames]
    );

    console.log('\n当前状态:');
    console.table(checkResult.rows);

    // 2. 更新密码
    console.log('\n🔄 更新密码哈希...');
    const updateResult = await pool.query(
      `UPDATE app_user 
       SET password_hash = $1, 
           updated_at = CURRENT_TIMESTAMP
       WHERE username = ANY($2)
       RETURNING username, real_name`,
      [CORRECT_PASSWORD_HASH, functionalDeptUsernames]
    );

    console.log(`\n✅ 成功更新 ${updateResult.rowCount} 个用户的密码`);
    updateResult.rows.forEach(r => console.log(`   - ${r.username} (${r.real_name})`));

    // 3. 验证更新结果
    console.log('\n🔍 验证更新结果...');
    const verifyResult = await pool.query(
      `SELECT username, real_name, LEFT(password_hash, 30) as pwd_prefix, updated_at
       FROM app_user 
       WHERE username = ANY($1)
       ORDER BY username`,
      [functionalDeptUsernames]
    );

    console.log('\n更新后状态:');
    console.table(verifyResult.rows);

    // 4. 检查是否所有用户都使用正确的密码哈希
    const incorrectUsers = verifyResult.rows.filter(
      row => !row.pwd_prefix.startsWith('$2a$10$UF.UUADlBmXZU1tU3iec')
    );

    if (incorrectUsers.length > 0) {
      console.log('\n⚠️  警告: 以下用户的密码哈希仍然不正确:');
      console.table(incorrectUsers);
    } else {
      console.log('\n✅ 所有职能部门用户密码已统一');
      console.log('📝 密码: 123456');
    }

  } catch (err) {
    console.error('\n❌ 执行失败:', err.message);
    console.error(err.stack);
    process.exit(1);
  } finally {
    await pool.end();
  }
}

fixPasswords();
