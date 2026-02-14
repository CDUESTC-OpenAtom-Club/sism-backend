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
  await client.connect();
  const result = await client.query(\`
    SELECT column_name, is_nullable
    FROM information_schema.columns
    WHERE table_name = 'indicator' AND column_name = 'task_id'
  \`);
  
  if (result.rows.length > 0) {
    const col = result.rows[0];
    console.log('task_id nullable:', col.is_nullable);
    console.log('需要先ALTER TABLE删除NOT NULL约束');
  }
  await client.end();
}

checkSchema();
