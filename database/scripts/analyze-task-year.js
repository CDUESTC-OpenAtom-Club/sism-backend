/**
 * 分析 sys_task 表的年份管理方式
 */

const { Client } = require('pg');

async function analyzeTaskYear() {
  const client = new Client({
    host: '175.24.139.148',
    port: 8386,
    database: 'strategic',
    user: 'postgres',
    password: '64378561huaW'
  });

  try {
    await client.connect();
    console.log('✅ 数据库连接成功\n');

    // 1. 查看 sys_task 表结构
    console.log('📋 sys_task 表完整结构:');
    console.log('='.repeat(80));
    const columns = await client.query(`
      SELECT 
        column_name, 
        data_type, 
        character_maximum_length,
        is_nullable,
        column_default
      FROM information_schema.columns
      WHERE table_name = 'sys_task'
      ORDER BY ordinal_position
    `);
    
    columns.rows.forEach(row => {
      const length = row.character_maximum_length ? `(${row.character_maximum_length})` : '';
      const nullable = row.is_nullable === 'YES' ? 'NULL' : 'NOT NULL';
      const defaultVal = row.column_default ? ` DEFAULT ${row.column_default}` : '';
      console.log(`  ${row.column_name.padEnd(25)} ${(row.data_type + length).padEnd(25)} ${nullable}${defaultVal}`);
    });

    // 2. 查看现有任务数据
    console.log('\n📊 现有任务数据:');
    console.log('='.repeat(80));
    const tasks = await client.query(`
      SELECT 
        task_id,
        task_name,
        task_type,
        cycle_id,
        plan_id,
        created_at,
        remark
      FROM sys_task
      ORDER BY task_id
    `);
    
    console.log(`总任务数: ${tasks.rows.length}\n`);
    tasks.rows.forEach((task, index) => {
      console.log(`${index + 1}. [ID: ${task.task_id}] ${task.task_name}`);
      console.log(`   类型: ${task.task_type} | cycle_id: ${task.cycle_id} | plan_id: ${task.plan_id}`);
      console.log(`   创建时间: ${task.created_at} | 备注: ${task.remark || '无'}`);
      console.log('');
    });

    // 3. 检查 cycle 表（可能包含年份信息）
    console.log('📋 cycle 表结构:');
    console.log('='.repeat(80));
    const cycleColumns = await client.query(`
      SELECT column_name, data_type, is_nullable
      FROM information_schema.columns
      WHERE table_name = 'cycle'
      ORDER BY ordinal_position
    `);
    
    if (cycleColumns.rows.length > 0) {
      cycleColumns.rows.forEach(row => {
        console.log(`  ${row.column_name.padEnd(30)} ${row.data_type.padEnd(20)} ${row.is_nullable === 'YES' ? 'NULL' : 'NOT NULL'}`);
      });
      
      // 查看 cycle 数据
      const cycles = await client.query('SELECT * FROM cycle ORDER BY cycle_id');
      console.log(`\n📊 cycle 表数据 (${cycles.rows.length} 条):`);
      cycles.rows.forEach(cycle => {
        console.log(`  [${cycle.cycle_id}] ${cycle.cycle_name || cycle.name || 'N/A'} - ${JSON.stringify(cycle).substring(0, 100)}...`);
      });
    } else {
      console.log('  ⚠️  cycle 表不存在或无列');
    }

    // 4. 检查 plan 表（可能包含年份信息）
    console.log('\n📋 plan 表结构:');
    console.log('='.repeat(80));
    const planColumns = await client.query(`
      SELECT column_name, data_type, is_nullable
      FROM information_schema.columns
      WHERE table_name = 'plan'
      ORDER BY ordinal_position
    `);
    
    if (planColumns.rows.length > 0) {
      planColumns.rows.forEach(row => {
        console.log(`  ${row.column_name.padEnd(30)} ${row.data_type.padEnd(20)} ${row.is_nullable === 'YES' ? 'NULL' : 'NOT NULL'}`);
      });
      
      // 查看 plan 数据
      const plans = await client.query('SELECT * FROM plan ORDER BY plan_id LIMIT 10');
      console.log(`\n📊 plan 表数据 (前 10 条):`);
      plans.rows.forEach(plan => {
        console.log(`  [${plan.plan_id}] ${JSON.stringify(plan).substring(0, 150)}...`);
      });
    } else {
      console.log('  ⚠️  plan 表不存在或无列');
    }

    // 5. 检查 task 和 indicator 的关联
    console.log('\n📊 任务与指标的关联分析:');
    console.log('='.repeat(80));
    const taskIndicatorRelation = await client.query(`
      SELECT 
        t.task_id,
        t.task_name,
        COUNT(i.indicator_id) as indicator_count,
        COUNT(DISTINCT i.year) as year_count,
        STRING_AGG(DISTINCT i.year::text, ', ' ORDER BY i.year::text) as years
      FROM sys_task t
      LEFT JOIN indicator i ON i.task_id = t.task_id
      WHERE i.is_deleted = false OR i.is_deleted IS NULL
      GROUP BY t.task_id, t.task_name
      ORDER BY t.task_id
    `);
    
    taskIndicatorRelation.rows.forEach(row => {
      console.log(`任务 [${row.task_id}] ${row.task_name}`);
      console.log(`  关联指标: ${row.indicator_count} 个`);
      console.log(`  涉及年份: ${row.years || '无'} (${row.year_count} 个不同年份)`);
      console.log('');
    });

    // 6. 检查后端实体定义
    console.log('📝 建议检查的后端文件:');
    console.log('='.repeat(80));
    console.log('  1. sism-backend/src/main/java/com/sism/entity/StrategicTask.java');
    console.log('  2. sism-backend/src/main/java/com/sism/entity/Cycle.java');
    console.log('  3. sism-backend/src/main/java/com/sism/entity/Plan.java');
    console.log('  4. sism-backend/src/main/resources/db/migration/V*.sql (Flyway 迁移文件)');

  } catch (error) {
    console.error('❌ 错误:', error.message);
    console.error(error);
  } finally {
    await client.end();
    console.log('\n🔌 数据库连接已关闭');
  }
}

analyzeTaskYear();
