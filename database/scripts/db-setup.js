/**
 * SISM 数据库初始化脚本
 * 
 * 用法: node db-setup.js [--reset] [--seed] [--validate]
 *   --reset:    清空并重建数据库
 *   --seed:     插入示例数据
 *   --validate: 运行数据校验
 */
import pg from 'pg';
import fs from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';
import { config as dotenvConfig } from 'dotenv';

const { Client } = pg;
const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

dotenvConfig({ path: path.resolve(__dirname, '../.env') });

const config = {
  host: process.env.DB_HOST || 'localhost',
  port: parseInt(process.env.DB_PORT || '5432'),
  database: process.env.DB_NAME || 'strategic',
  user: process.env.DB_USER || 'postgres',
  password: process.env.DB_PASSWORD || ''
};

async function run() {
  const args = process.argv.slice(2);
  const doReset = args.includes('--reset');
  const doSeed = args.includes('--seed');
  const doValidate = args.includes('--validate');
  
  const client = new Client(config);
  
  try {
    await client.connect();
    console.log('✅ 数据库连接成功');
    
    if (doReset) {
      console.log('\n🗑️  清理现有对象...');
      const dropSql = fs.readFileSync(path.join(__dirname, 'drop-all.sql'), 'utf8');
      await client.query(dropSql);
      
      console.log('📦 创建表结构...');
      const initSql = fs.readFileSync(path.join(__dirname, 'init.sql'), 'utf8');
      await client.query(initSql);
      console.log('✅ 表结构创建完成');
    }
    
    if (doSeed) {
      console.log('\n🌱 插入种子数据...');
      const seedSql = fs.readFileSync(path.join(__dirname, 'seed-data.sql'), 'utf8');
      await client.query(seedSql);
      console.log('✅ 种子数据插入完成');
    }
    
    // 显示统计
    console.log('\n📊 数据库统计:');
    const stats = await client.query(`
      SELECT 
        (SELECT COUNT(*) FROM org) as orgs,
        (SELECT COUNT(*) FROM app_user) as users,
        (SELECT COUNT(*) FROM strategic_task) as tasks,
        (SELECT COUNT(*) FROM indicator) as indicators,
        (SELECT COUNT(*) FROM milestone) as milestones
    `);
    const s = stats.rows[0];
    console.log(`   组织: ${s.orgs} | 用户: ${s.users} | 任务: ${s.tasks} | 指标: ${s.indicators} | 里程碑: ${s.milestones}`);
    
    if (doValidate) {
      console.log('\n🔍 运行数据校验...');
      console.log('   请使用: psql -f database/validate-data.sql');
    }
    
  } catch (err) {
    console.error('❌ 错误:', err.message);
    process.exit(1);
  } finally {
    await client.end();
  }
}

run();
