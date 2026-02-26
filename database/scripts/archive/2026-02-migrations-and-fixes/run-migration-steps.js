#!/usr/bin/env node

/**
 * Execute sys_org migration in steps
 */

const { Client } = require('pg');

const client = new Client({
  host: '175.24.139.148',
  port: 8386,
  database: 'strategic',
  user: 'postgres',
  password: '64378561huaW'
});

async function executeMigration() {
  try {
    console.log('🔌 Connecting to database...');
    await client.connect();
    console.log('✅ Connected to database\n');

    // Step 1: Add unique constraint to sys_org.name
    console.log('📋 Step 1: Adding unique constraint to sys_org.name...');
    try {
      await client.query(`ALTER TABLE sys_org ADD CONSTRAINT uk_sys_org_name UNIQUE (name)`);
      console.log('✅ Unique constraint added\n');
    } catch (error) {
      if (error.code === '42P07') {
        console.log('⚠️  Constraint already exists, skipping\n');
      } else {
        throw error;
      }
    }

    // Step 2: Remove parent_org_id from sys_org
    console.log('📋 Step 2: Removing parent_org_id from sys_org...');
    try {
      await client.query(`ALTER TABLE sys_org DROP COLUMN IF EXISTS parent_org_id`);
      console.log('✅ parent_org_id column removed\n');
    } catch (error) {
      console.log('⚠️  Column may not exist, continuing\n');
    }

    // Step 3: Drop existing foreign key constraint on app_user.org_id
    console.log('📋 Step 3: Dropping existing foreign key constraint...');
    const fkResult = await client.query(`
      SELECT 
        tc.constraint_name,
        ccu.table_name AS foreign_table_name
      FROM information_schema.table_constraints tc
      JOIN information_schema.constraint_column_usage ccu 
        ON ccu.constraint_name = tc.constraint_name
      WHERE tc.table_name = 'app_user' 
        AND tc.constraint_type = 'FOREIGN KEY'
        AND ccu.table_name = 'org'
    `);
    
    if (fkResult.rows.length > 0) {
      const constraintName = fkResult.rows[0].constraint_name;
      await client.query(`ALTER TABLE app_user DROP CONSTRAINT ${constraintName}`);
      console.log(`✅ Dropped constraint: ${constraintName}\n`);
    } else {
      console.log('⚠️  No foreign key constraint found\n');
    }

    // Step 4: Update app_user.org_id to reference sys_org.id
    console.log('📋 Step 4: Updating app_user.org_id to reference sys_org...');
    
    // Get mapping
    const mappingResult = await client.query(`
      SELECT 
        o.org_id as old_org_id,
        s.id as new_org_id,
        o.org_name as org_name
      FROM org o
      INNER JOIN sys_org s ON o.org_name = s.name
    `);
    
    console.log('Mapping found:');
    mappingResult.rows.forEach(row => {
      console.log(`  org_id ${row.old_org_id} (${row.org_name}) -> sys_org.id ${row.new_org_id}`);
    });
    
    // Update app_user
    for (const mapping of mappingResult.rows) {
      await client.query(`
        UPDATE app_user
        SET org_id = $1
        WHERE org_id = $2
      `, [mapping.new_org_id, mapping.old_org_id]);
    }
    console.log('✅ app_user.org_id updated\n');

    // Step 5: Add new foreign key constraint
    console.log('📋 Step 5: Adding new foreign key constraint...');
    await client.query(`
      ALTER TABLE app_user 
        ADD CONSTRAINT fk_app_user_sys_org 
        FOREIGN KEY (org_id) 
        REFERENCES sys_org(id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE
    `);
    console.log('✅ New foreign key constraint added\n');

    // Step 6: Rename org table to org_deprecated
    console.log('📋 Step 6: Renaming org table to org_deprecated...');
    try {
      await client.query(`ALTER TABLE org RENAME TO org_deprecated`);
      console.log('✅ org table renamed to org_deprecated\n');
    } catch (error) {
      if (error.code === '42P01') {
        console.log('⚠️  org table does not exist, skipping\n');
      } else {
        throw error;
      }
    }

    // Step 7: Add indexes to sys_org
    console.log('📋 Step 7: Adding indexes to sys_org...');
    const indexes = [
      { name: 'idx_sys_org_type', column: 'type' },
      { name: 'idx_sys_org_active', column: 'is_active' },
      { name: 'idx_sys_org_sort', column: 'sort_order' }
    ];
    
    for (const index of indexes) {
      try {
        await client.query(`CREATE INDEX IF NOT EXISTS ${index.name} ON sys_org(${index.column})`);
        console.log(`✅ Index ${index.name} created`);
      } catch (error) {
        console.log(`⚠️  Index ${index.name} may already exist`);
      }
    }
    console.log('');

    // Verification
    console.log('🔍 Verifying migration results...\n');

    // Check sys_org table
    const orgResult = await client.query(`
      SELECT 
        type,
        COUNT(*) as count
      FROM sys_org
      GROUP BY type
      ORDER BY type
    `);

    console.log('📊 sys_org table statistics:');
    orgResult.rows.forEach(row => {
      console.log(`   ${row.type}: ${row.count} organizations`);
    });

    // Check app_user
    const userResult = await client.query(`
      SELECT u.user_id, u.username, u.org_id, s.name as org_name
      FROM app_user u
      LEFT JOIN sys_org s ON u.org_id = s.id
    `);
    
    console.log('\n📊 app_user verification:');
    userResult.rows.forEach(row => {
      console.log(`   User ${row.username} (id=${row.user_id}) -> org_id=${row.org_id} (${row.org_name})`);
    });

    console.log('\n✅ Migration completed successfully!');

  } catch (error) {
    console.error('\n❌ Migration failed:', error.message);
    console.error(error);
    process.exit(1);
  } finally {
    await client.end();
    console.log('\n🔌 Database connection closed');
  }
}

// Run migration
executeMigration();
