const { Pool } = require('pg');

const pool = new Pool({
  host: '175.24.139.148',
  port: 8386,
  database: 'strategic',
  user: 'postgres',
  password: '64378561huaW',
});

async function queryUsers() {
  try {
    // 先查询表结构
    const columnsResult = await pool.query(`
      SELECT column_name, data_type
      FROM information_schema.columns
      WHERE table_name = 'sys_user'
      ORDER BY ordinal_position
    `);

    console.log('sys_user 表结构：');
    console.log('==========================================');
    columnsResult.rows.forEach(col => {
      console.log(`${col.column_name}: ${col.data_type}`);
    });
    console.log('==========================================');
    console.log('');

    // 查询所有用户数据
    const result = await pool.query(`
      SELECT
        id,
        username,
        password_hash,
        real_name,
        org_id,
        is_active,
        created_at
      FROM sys_user
      ORDER BY id
    `);

    console.log('数据库中的用户列表：');
    console.log('==========================================');
    console.log('');

    result.rows.forEach(user => {
      console.log(`ID: ${user.id}`);
      console.log(`用户名: ${user.username}`);
      console.log(`姓名: ${user.real_name}`);
      console.log(`组织ID: ${user.org_id}`);
      console.log(`是否激活: ${user.is_active ? '是' : '否'}`);
      console.log(`密码哈希: ${user.password_hash}`);
      console.log(`创建时间: ${user.created_at}`);
      console.log('------------------------------------------');
      console.log('');
    });

    console.log(`总计: ${result.rows.length} 个用户`);
  } catch (err) {
    console.error('查询失败:', err);
  } finally {
    await pool.end();
  }
}

queryUsers();
