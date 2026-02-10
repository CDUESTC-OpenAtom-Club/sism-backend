#!/usr/bin/env node

const { Client } = require('pg');

const client = new Client({
  host: '175.24.139.148',
  port: 8386,
  database: 'strategic',
  user: 'muzimu',
  password: '64378561huaW'
});

async function checkSchema() {
  try {
    await client.connect();
    
    // 获取indicator表的所有列
    const result = await client.query(`
      SELECT column_name, data_type, is_nullable, column_default
      FROM information_schema.columns
      WHERE table_name = 'indicator'
      ORDER BY ordinal_position
    `);
    
    console.log('indicator 表的当前结构:\n');
    result.rows.forEach(row => {
      console.log(`  ${row.column_name.padEnd(30)} ${row.data_type.padEnd(25)} nullable: ${row.is_nullable}`);
    });
    
    // 检查代码中需要的列
    const requiredColumns = [
      'indicator_id', 'task_id', 'parent_indicator_id', 'level',
      'owner_org_id', 'target_org_id', 'indicator_desc', 'weight_percent',
      'sort_order', 'year', 'status', 'remark', 'is_qualitative',
      'type1', 'type2', 'can_withdraw', 'target_value', 'actual_value',
      'unit', 'responsible_person', 'progress', 'status_audit',
      'progress_approval_status', 'pending_progress', 'pending_remark',
      'pending_attachments', 'created_at', 'updated_at'
    ];
    
    const existingColumns = result.rows.map(r => r.column_name);
    const missingColumns = requiredColumns.filter(col => !existingColumns.includes(col));
    
    if (missingColumns.length > 0) {
      console.log('\n缺失的列:');
      missingColumns.forEach(col => console.log(`  - ${col}`));
    } else {
      console.log('\n✓ 所有必需的列都存在');
    }
    
  } catch (error) {
    console.error('错误:', error.message);
  } finally {
    await client.end();
  }
}

checkSchema();
