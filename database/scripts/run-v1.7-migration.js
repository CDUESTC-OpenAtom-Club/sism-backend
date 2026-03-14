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

    // Read migration file
    const migrationPath = path.join(__dirname, '../migrations/V1.7__rename_app_user_to_sys_user.sql');
    const migrationSQL = fs.readFileSync(migrationPath, 'utf8');

    console.log('\n📋 Starting V1.7 migration: Rename app_user to sys_user\n');

    // Execute migration
    await client.query(migrationSQL);
    console.log('✓ Migration executed successfully');

    // Verify results
    console.log('\n📊 Verification:');
    
    const sysUserCount = await client.query('SELECT COUNT(*) FROM sys_user');
    console.log(`  - sys_user records: ${sysUserCount.rows[0].count}`);

    const deprecatedCount = await client.query('SELECT COUNT(*) FROM sys_user_deprecated');
    console.log(`  - sys_user_deprecated records: ${deprecatedCount.rows[0].count}`);

    // Check if app_user still exists
    const appUserExists = await client.query(`
      SELECT EXISTS (
        SELECT FROM information_schema.tables 
        WHERE table_name = 'app_user'
      );
    `);
    console.log(`  - app_user table exists: ${appUserExists.rows[0].exists ? 'YES (ERROR!)' : 'NO (correct)'}`);

    console.log('\n✅ Migration V1.7 completed successfully!');

  } catch (error) {
    console.error('❌ Migration failed:', error.message);
    console.error(error);
    process.exit(1);
  } finally {
    await client.end();
  }
}

runMigration();
