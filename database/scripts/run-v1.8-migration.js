const { Client } = require('pg');
const fs = require('fs');
const path = require('path');

const client = new Client({
  host: '175.24.139.148',
  port: 8386,
  database: 'strategic',
  user: 'muzimu',
  password: '64378561huaW'
});

async function runMigration() {
  try {
    await client.connect();
    console.log('✓ Connected to database');

    // Check current state
    console.log('\n📊 Pre-migration state:');
    const taskCount = await client.query('SELECT COUNT(*) FROM task');
    console.log(`  - task table: ${taskCount.rows[0].count} records`);
    
    const strategicCount = await client.query('SELECT COUNT(*) FROM strategic_task');
    console.log(`  - strategic_task table: ${strategicCount.rows[0].count} records`);

    // Read migration file
    const migrationPath = path.join(__dirname, '../migrations/V1.8__migrate_task_to_strategic_task.sql');
    const migrationSQL = fs.readFileSync(migrationPath, 'utf8');

    console.log('\n📋 Starting V1.8 migration: task → strategic_task\n');

    // Execute migration
    await client.query(migrationSQL);
    console.log('✓ Migration executed successfully');

    // Verify results
    console.log('\n📊 Post-migration state:');
    
    const newStrategicCount = await client.query('SELECT COUNT(*) FROM strategic_task');
    console.log(`  - strategic_task records: ${newStrategicCount.rows[0].count}`);

    const deprecatedCount = await client.query('SELECT COUNT(*) FROM task_deprecated');
    console.log(`  - task_deprecated records: ${deprecatedCount.rows[0].count}`);

    // Check if task table still exists
    const taskExists = await client.query(`
      SELECT EXISTS (
        SELECT FROM information_schema.tables 
        WHERE table_name = 'task'
      );
    `);
    console.log(`  - task table exists: ${taskExists.rows[0].exists ? 'YES (ERROR!)' : 'NO (correct)'}`);

    // Show sample migrated data
    console.log('\n📝 Sample migrated data:');
    const sample = await client.query(`
      SELECT task_id, task_name, task_type, org_id, cycle_id 
      FROM strategic_task 
      ORDER BY task_id 
      LIMIT 3
    `);
    sample.rows.forEach(row => {
      console.log(`  - [${row.task_id}] ${row.task_name} (${row.task_type}) - Org: ${row.org_id}, Cycle: ${row.cycle_id}`);
    });

    console.log('\n✅ Migration V1.8 completed successfully!');

  } catch (error) {
    console.error('❌ Migration failed:', error.message);
    console.error(error);
    process.exit(1);
  } finally {
    await client.end();
  }
}

runMigration();
