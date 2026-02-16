#!/usr/bin/env node

/**
 * 检查数据库中与废弃表相关的混乱逻辑
 *
 * 检查项:
 * 1. 表字段中是否还有引用废弃表的外键(虽然外键已删除,但数据可能混乱)
 * 2. 是否还有表名包含 deprecated 但还在使用的表
 * 3. 数据中是否存在引用废弃表ID的记录
 * 4. Repository/Entity 层是否还有引用废弃表的代码
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
  'task_deprecated',
  'app_user'  // 已重命名为 sys_user
];

const DEPRECATED_PREFIXES = [
  'deprecated',
  '_deprecated'
];

/**
 * 主检查函数
 */
async function checkDeprecatedTableReferences() {
  try {
    await client.connect();
    console.log('✓ 已连接到数据库\n');

    console.log('==========================================================');
    console.log('数据库废弃表混乱逻辑检查');
    console.log('==========================================================\n');

    // 检查1: 查找所有包含 deprecated 的表
    await checkDeprecatedTables();

    // 检查2: 检查数据中是否存在引用废弃表ID的记录
    await checkDataIntegrity();

    // 检查3: 检查表结构中是否有混乱的字段命名
    await checkColumnNamingConflicts();

    // 检查4: 检查序列(sequence)是否指向废弃表
    await checkSequences();

    // 检查5: 检查索引是否指向废弃表
    await checkIndexes();

  } catch (error) {
    console.error('\n✗ 检查失败:', error.message);
    throw error;
  } finally {
    await client.end();
    console.log('\n✓ 数据库连接已关闭');
  }
}

/**
 * 检查1: 查找所有包含 deprecated 的表
 */
async function checkDeprecatedTables() {
  console.log('【检查 1】查找废弃表...\n');

  const sql = `
    SELECT
        tablename AS table_name,
        schemaname AS schema_name
    FROM pg_tables
    WHERE schemaname = 'public'
      AND (
        tablename LIKE '%deprecated%'
        OR tablename = 'app_user'
      )
    ORDER BY tablename;
  `;

  const result = await client.query(sql);

  if (result.rows.length === 0) {
    console.log('✅ 未找到废弃表\n');
    return;
  }

  console.log(`⚠️  找到 ${result.rows.length} 个废弃表:\n`);

  for (const row of result.rows) {
    const status = isTableInUse(row.table_name) ? '🔴 可能仍在使用' : '✅ 应该已废弃';
    console.log(`  ${status} ${row.table_name}`);

    // 检查表是否有数据
    const countSQL = `SELECT COUNT(*) as count FROM public.${row.table_name}`;
    const countResult = await client.query(countSQL);
    const count = parseInt(countResult.rows[0].count);

    if (count > 0) {
      console.log(`    ⚠️  表中还有 ${count} 条数据!`);
    }
  }
  console.log('');
}

/**
 * 检查2: 检查数据完整性 - 是否有记录引用废弃表ID
 */
async function checkDataIntegrity() {
  console.log('【检查 2】检查数据完整性...\n');

  const issues = [];

  // 检查 indicator 表的数据
  console.log('🔍 检查 indicator 表数据...');
  const indicatorChecks = [
    {
      name: 'indicator.owner_org_id',
      sql: `SELECT COUNT(*) FROM indicator WHERE owner_org_id IS NULL`
    },
    {
      name: 'indicator.target_org_id',
      sql: `SELECT COUNT(*) FROM indicator WHERE target_org_id IS NULL`
    },
    {
      name: 'indicator.task_id',
      sql: `
        SELECT COUNT(*)
        FROM indicator i
        LEFT JOIN strategic_task st ON st.task_id = i.task_id
        WHERE i.task_id IS NOT NULL AND st.task_id IS NULL
      `
    }
  ];

  for (const check of indicatorChecks) {
    const result = await client.query(check.sql);
    const count = parseInt(result.rows[0].count || 0);

    if (count > 0) {
      issues.push({
        table: 'indicator',
        field: check.name,
        issue: '发现无效引用',
        count: count
      });
      console.log(`  ⚠️  ${check.name}: ${count} 条记录可能无效`);
    } else {
      console.log(`  ✅ ${check.name}: 数据完整`);
    }
  }

  // 检查 sys_user 表数据
  console.log('\n🔍 检查 sys_user 表数据...');
  const userChecks = [
    {
      name: 'sys_user.org_id',
      sql: `SELECT COUNT(*) FROM sys_user WHERE org_id IS NULL`
    }
  ];

  for (const check of userChecks) {
    const result = await client.query(check.sql);
    const count = parseInt(result.rows[0].count || 0);

    if (count > 0) {
      issues.push({
        table: 'sys_user',
        field: check.name,
        issue: '发现NULL值',
        count: count
      });
      console.log(`  ⚠️  ${check.name}: ${count} 条记录为NULL`);
    } else {
      console.log(`  ✅ ${check.name}: 数据完整`);
    }
  }

  // 检查是否还有数据引用废弃表的ID
  console.log('\n🔍 检查是否还有引用废弃表ID的数据...');
  const deprecatedRefChecks = [
    {
      table: 'org_deprecated',
      sql: `SELECT COUNT(*) FROM indicator WHERE owner_org_id IN (SELECT id FROM org_deprecated)`
    },
    {
      table: 'sys_user_deprecated',
      sql: `SELECT COUNT(*) FROM sys_user WHERE id IN (SELECT id FROM sys_user_deprecated)`
    }
  ];

  for (const check of deprecatedRefChecks) {
    try {
      const result = await client.query(check.sql);
      const count = parseInt(result.rows[0].count || 0);

      if (count > 0) {
        issues.push({
          table: check.table,
          issue: '当前表数据引用废弃表ID',
          count: count
        });
        console.log(`  ⚠️  发现 ${count} 条记录引用 ${check.table}`);
      }
    } catch (error) {
      // 表可能不存在,忽略
    }
  }

  if (issues.length === 0) {
    console.log('\n✅ 数据完整性检查通过,无异常\n');
  } else {
    console.log(`\n⚠️  发现 ${issues.length} 个数据完整性问题:\n`);
    issues.forEach(issue => {
      console.log(`  - ${issue.table}.${issue.field}: ${issue.issue} (${issue.count} 条)`);
    });
    console.log('');
  }
}

/**
 * 检查3: 检查字段命名冲突
 */
async function checkColumnNamingConflicts() {
  console.log('【检查 3】检查字段命名冲突...\n');

  const sql = `
    SELECT
          table_name,
          column_name,
          data_type
    FROM information_schema.columns
    WHERE table_schema = 'public'
      AND (
        column_name LIKE '%org%'
        OR column_name LIKE '%user%'
        OR column_name LIKE '%task%'
      )
      AND table_name NOT LIKE '%deprecated%'
    ORDER BY table_name, ordinal_position;
  `;

  const result = await client.query(sql);
  const conflicts = [];

  for (const row of result.rows) {
    // 检查是否有可能混淆的字段名
    if (row.column_name.includes('org') &&
        !row.column_name.includes('org_id') &&
        !row.column_name.includes('org_')) {
      conflicts.push(row);
    }
  }

  if (conflicts.length > 0) {
    console.log(`⚠️  发现 ${conflicts.length} 个可能混淆的字段:\n`);
    conflicts.forEach(conflict => {
      console.log(`  - ${conflict.table_name}.${conflict.column_name} (${conflict.data_type})`);
    });
    console.log('');
  } else {
    console.log('✅ 未发现字段命名冲突\n');
  }
}

/**
 * 检查4: 检查序列(sequence)是否指向废弃表
 */
async function checkSequences() {
  console.log('【检查 4】检查序列是否指向废弃表...\n');

  const sql = `
    SELECT
          sequencename AS sequence_name
    FROM pg_sequences
    WHERE schemaname = 'public'
      AND (
        sequencename LIKE '%deprecated%'
        OR sequencename LIKE '%org_deprecated%'
        OR sequencename LIKE '%user_deprecated%'
        OR sequencename LIKE '%task_deprecated%'
      )
    ORDER BY sequencename;
  `;

  const result = await client.query(sql);

  if (result.rows.length > 0) {
    console.log(`⚠️  发现 ${result.rows.length} 个指向废弃表的序列:\n`);
    result.rows.forEach(row => {
      console.log(`  - ${row.sequence_name}`);
    });
    console.log('');
  } else {
    console.log('✅ 未发现指向废弃表的序列\n');
  }
}

/**
 * 检查5: 检查索引是否指向废弃表
 */
async function checkIndexes() {
  console.log('【检查 5】检查索引是否指向废弃表...\n');

  const sql = `
    SELECT
          tablename AS table_name,
          indexname AS index_name,
          indexdef AS index_definition
    FROM pg_indexes
    WHERE schemaname = 'public'
      AND (
        tablename LIKE '%deprecated%'
        OR indexname LIKE '%deprecated%'
      )
    ORDER BY tablename, indexname;
  `;

  const result = await client.query(sql);

  if (result.rows.length > 0) {
    console.log(`⚠️  发现 ${result.rows.length} 个指向废弃表的索引:\n`);
    for (const row of result.rows) {
      console.log(`  表: ${row.table_name}`);
      console.log(`  索引: ${row.index_name}`);
      console.log(`  定义: ${row.index_definition}`);
      console.log('');
    }
  } else {
    console.log('✅ 未发现指向废弃表的索引\n');
  }
}

/**
 * 辅助函数: 判断表是否还在使用
 */
function isTableInUse(tableName) {
  // 这些表虽然名字包含 deprecated,但可能还有数据
  const tablesWithData = [
    'org_deprecated',
    'sys_user_deprecated',
    'task_deprecated'
  ];
  return tablesWithData.includes(tableName);
}

/**
 * 生成总结报告
 */
async function generateSummaryReport() {
  console.log('\n==========================================================');
  console.log('检查总结');
  console.log('==========================================================\n');

  // 统计废弃表数据量
  const summarySQL = `
    SELECT
          'org_deprecated' as table_name,
          (SELECT COUNT(*) FROM org_deprecated) as row_count
    UNION ALL
    SELECT
          'sys_user_deprecated',
          (SELECT COUNT(*) FROM sys_user_deprecated)
    UNION ALL
    SELECT
          'task_deprecated',
          (SELECT COUNT(*) FROM task_deprecated)
  `;

  try {
    const result = await client.query(summarySQL);

    console.log('📊 废弃表数据统计:\n');
    let totalData = 0;
    for (const row of result.rows) {
      const count = parseInt(row.row_count || 0);
      totalData += count;
      const status = count > 0 ? '⚠️  ' : '✅ ';
      console.log(`  ${status}${row.table_name}: ${count} 条数据`);
    }
    console.log(`\n  总计: ${totalData} 条废弃数据\n`);

    if (totalData > 0) {
      console.log('⚠️  建议:');
      console.log('  1. 确认业务是否还需要这些数据');
      console.log('  2. 如不需要,可清理废弃表以释放空间');
      console.log('  3. 如需保留,确保应用层不再引用这些表\n');
    } else {
      console.log('✅ 废弃表均已清空,可以考虑删除表结构\n');
    }
  } catch (error) {
    console.log('⚠️  无法统计废弃表数据:', error.message);
  }
}

// 执行检查
checkDeprecatedTableReferences()
  .then(async () => {
    await client.connect(); // 重新连接执行总结
    await generateSummaryReport();
    await client.end();
    console.log('✓ 检查完成');
    process.exit(0);
  })
  .catch(error => {
    console.error('✗ 检查失败:', error);
    process.exit(1);
  });
