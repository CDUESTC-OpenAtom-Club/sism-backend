const { Client } = require('pg');
const client = new Client({
  host: '175.24.139.148',
  port: 8386,
  database: 'strategic',
  user: 'muzimu',
  password: '64378561huaW'
});
(async () => {
  await client.connect();
  const result = await client.query("SELECT is_nullable FROM information_schema.columns WHERE table_name = 'indicator' AND column_name = 'task_id'");
  console.log('task_id nullable:', result.rows[0]?.is_nullable);
  if (result.rows[0]?.is_nullable === 'NO') {
    console.log('需要先ALTER TABLE删除NOT NULL约束');
  }
  await client.end();
})()
