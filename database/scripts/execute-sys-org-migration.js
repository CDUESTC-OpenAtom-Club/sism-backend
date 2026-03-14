#!/usr/bin/env node

/**
 * Execute sys_org migration
 * This script runs the V1.5 migration to switch from org to sys_org table
 */

const { Client } = require('pg');
const fs = require('fs');
const path = require('path');

// Load environment variables
require('dotenv').config({ path: path.join(__dirname, '../../.env') });

const client = new Client({
  host: process.env.DB_HOST || 'localhost',
  port: process.env.DB_PORT || 5432,
  database: process.env.DB_NAME || 'strategic',
  user: process.env.DB_USERNAME || 'postgres',
  password: process.env.DB_PASSWORD || 'postgres'
});

async function executeMigration() {
  try {
    console.log('🔌 Connecting to database...');
    await client.connect();
    console.log('✅ Connected to database');

    // Read migration file
    const migrationPath = path.join(__dirname, '../migrations/V1.5__migrate_to_sys_org.sql');
    const migrationSQL = fs.readFileSync(migrationPath, 'utf8');

    console.log('\n📋 Executing migration V1.5: Migrate to sys_org...\n');

    // Execute migration
    await client.query(migrationSQL);

    console.log('\n✅ Migration completed successfully!');

    // Verify results
    console.log('\n🔍 Verifying migration results...\n');

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

    // Check unique constraint
    const constraintResult = await client.query(`
      SELECT constraint_name
      FROM information_schema.table_constraints
      WHERE table_name = 'sys_org' 
        AND constraint_type = 'UNIQUE'
        AND constraint_name = 'uk_sys_org_name'
    `);

    if (constraintResult.rows.length > 0) {
      console.log('✅ Unique constraint uk_sys_org_name exists');
    } else {
      console.log('❌ Unique constraint uk_sys_org_name NOT found');
    }

    // Check parent_org_id column
    const columnResult = await client.query(`
      SELECT column_name
      FROM information_schema.columns
      WHERE table_name = 'sys_org' 
        AND column_name = 'parent_org_id'
    `);

    if (columnResult.rows.length === 0) {
      console.log('✅ parent_org_id column removed');
    } else {
      console.log('❌ parent_org_id column still exists');
    }

    // Check app_user foreign key
    const fkResult = await client.query(`
      SELECT 
        tc.constraint_name,
        ccu.table_name AS foreign_table_name
      FROM information_schema.table_constraints tc
      JOIN information_schema.constraint_column_usage ccu 
        ON ccu.constraint_name = tc.constraint_name
      WHERE tc.table_name = 'app_user' 
        AND tc.constraint_type = 'FOREIGN KEY'
        AND tc.constraint_name LIKE '%org%'
    `);

    if (fkResult.rows.length > 0) {
      console.log(`✅ app_user foreign key: ${fkResult.rows[0].constraint_name} -> ${fkResult.rows[0].foreign_table_name}`);
    } else {
      console.log('❌ app_user foreign key NOT found');
    }

    // Check org_deprecated table
    const deprecatedResult = await client.query(`
      SELECT table_name
      FROM information_schema.tables
      WHERE table_name = 'org_deprecated'
    `);

    if (deprecatedResult.rows.length > 0) {
      console.log('✅ org table renamed to org_deprecated');
    } else {
      console.log('⚠️  org_deprecated table NOT found (org table may not exist)');
    }

    console.log('\n✅ All verification checks completed!');

  } catch (error) {
    console.error('❌ Migration failed:', error.message);
    console.error(error);
    process.exit(1);
  } finally {
    await client.end();
    console.log('\n🔌 Database connection closed');
  }
}

// Run migration
executeMigration();
