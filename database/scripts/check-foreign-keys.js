#!/usr/bin/env node

/**
 * 检查数据库外键约束状态
 * 验证 indicator 表的 owner_org_id 和 target_org_id 是否有外键约束
 */

const { Client } = require('pg');

const client = new Client({
  host: '175.24.139.148',
  port: 8386,
  database: 'strategic',
  user: 'muzimu',
  password: '64378561huaW'
});

async function checkForeignKeys() {
  try {
    await client.connect();
    console.log('✓ 已连接到数据库\n');

    // 检查 indicator 表的外键约束
    const checkFKSQL = `
      SELECT
          conname AS constraint_name,
          conrelid::regclass AS table_name,
          confrelid::regclass AS referenced_table,
          pg_get_constraintdef(c.oid) AS constraint_definition
      FROM pg_constraint c
      JOIN pg_namespace n ON n.oid = c.connamespace
      WHERE conrelid = 'indicator'::regclass
        AND contype = 'f'
      ORDER BY conname;
    `;

    console.log('🔍 检查 indicator 表的外键约束...\n');
    const result = await client.query(checkFKSQL);

    if (result.rows.length === 0) {
      console.log('⚠️  警告: indicator 表没有外键约束!\n');
      console.log('字段状态:');
      await checkColumnStatus(client);
    } else {
      console.log(`✓ 找到 ${result.rows.length} 个外键约束:\n`);
      result.rows.forEach(row => {
        console.log(`  约束名: ${row.constraint_name}`);
        console.log(`  表: ${row.table_name}`);
        console.log(`  引用表: ${row.referenced_table}`);
        console.log(`  定义: ${row.constraint_definition}`);
        console.log('');
      });
    }

    // 检查其他表的外键约束
    console.log('\n🔍 检查其他表的外键约束...\n');
    const allFKSQL = `
      SELECT
          conname AS constraint_name,
          conrelid::regclass AS table_name,
          confrelid::regclass AS referenced_table,
          pg_get_constraintdef(c.oid) AS constraint_definition
      FROM pg_constraint c
      JOIN pg_namespace n ON n.oid = c.connamespace
      WHERE contype = 'f'
        AND n.nspname = 'public'
      ORDER BY conrelid::regclass, conname;
    `;

    const allResult = await client.query(allFKSQL);
    console.log(`✓ 数据库共有 ${allResult.rows.length} 个外键约束\n`);

    // 按表分组显示
    const byTable = {};
    allResult.rows.forEach(row => {
      const table = row.table_name;
      if (!byTable[table]) {
        byTable[table] = [];
      }
      byTable[table].push(row);
    });

    Object.keys(byTable).forEach(table => {
      console.log(`\n📋 ${table}:`);
      byTable[table].forEach(fk => {
        console.log(`  → ${fk.referenced_table} (${fk.constraint_name})`);
      });
    });

  } catch (error) {
    console.error('\n✗ 检查失败:', error.message);
    throw error;
  } finally {
    await client.end();
    console.log('\n✓ 数据库连接已关闭\n');
  }
}

async function checkColumnStatus(client) {
  const columnSQL = `
    SELECT
        column_name,
        data_type,
        is_nullable,
        column_default
    FROM information_schema.columns
    WHERE table_name = 'indicator'
      AND column_name IN ('owner_org_id', 'target_org_id')
    ORDER BY ordinal_position;
  `;

  const result = await client.query(columnSQL);
  result.rows.forEach(col => {
    console.log(`\n  ${col.column_name}:`);
    console.log(`    类型: ${col.data_type}`);
    console.log(`    可空: ${col.is_nullable === 'YES' ? '是' : '否'}`);
    if (col.column_default) {
      console.log(`    默认值: ${col.column_default}`);
    }
  });
}

checkForeignKeys()
  .then(() => {
    console.log('✓ 外键约束检查完成');
    process.exit(0);
  })
  .catch(error => {
    console.error('✗ 检查失败:', error);
    process.exit(1);
  });
