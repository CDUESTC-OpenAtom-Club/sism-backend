/**
 * 统一数据验证脚本
 * 合并了 check-data.js, test-api.js, verify-frontend-data.js 的功能
 * 
 * 用法: node scripts/verify.js [--db] [--api] [--all]
 *   --db   仅检查数据库数据
 *   --api  检查 API 连接和数据
 *   --all  完整验证（默认）
 */
import pg from 'pg';
import { config } from 'dotenv';
import { fileURLToPath } from 'url';
import { dirname, resolve } from 'path';

const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);
config({ path: resolve(__dirname, '../.env') });

const { Client } = pg;

const dbConfig = {
  host: process.env.DB_HOST || 'localhost',
  port: parseInt(process.env.DB_PORT || '5432'),
  database: process.env.DB_NAME || 'strategic',
  user: process.env.DB_USER || 'postgres',
  password: process.env.DB_PASSWORD || '',
  connectionTimeoutMillis: 30000,
};

const API_BASE = process.env.VITE_API_BASE_URL || 'http://localhost:8080/api';

// ==================== 数据库验证 ====================

async function verifyDatabase() {
  console.log('\n📊 数据库数据验证');
  console.log('═'.repeat(50));
  
  const client = new Client(dbConfig);
  
  try {
    await client.connect();
    console.log('✅ 数据库连接成功\n');
    
    // 表记录统计
    const stats = await client.query(`
      SELECT 'org' AS table_name, COUNT(*) AS count FROM org
      UNION ALL SELECT 'app_user', COUNT(*) FROM app_user
      UNION ALL SELECT 'assessment_cycle', COUNT(*) FROM assessment_cycle
      UNION ALL SELECT 'strategic_task', COUNT(*) FROM strategic_task
      UNION ALL SELECT 'indicator', COUNT(*) FROM indicator
      UNION ALL SELECT 'milestone', COUNT(*) FROM milestone
      UNION ALL SELECT 'progress_report', COUNT(*) FROM progress_report
      UNION ALL SELECT 'approval_record', COUNT(*) FROM approval_record
      ORDER BY table_name
    `);
    
    console.log('表记录统计:');
    stats.rows.forEach(row => {
      console.log(`  ${row.table_name.padEnd(20)} ${row.count} 条`);
    });
    
    // 按年度统计
    console.log('\n按年度统计:');
    const yearStats = await client.query(`
      SELECT 
        ac.year,
        COUNT(DISTINCT st.task_id) AS tasks,
        COUNT(DISTINCT i.indicator_id) AS indicators,
        COUNT(DISTINCT m.milestone_id) AS milestones
      FROM assessment_cycle ac
      LEFT JOIN strategic_task st ON ac.cycle_id = st.cycle_id
      LEFT JOIN indicator i ON st.task_id = i.task_id
      LEFT JOIN milestone m ON i.indicator_id = m.indicator_id
      GROUP BY ac.year
      ORDER BY ac.year
    `);
    
    yearStats.rows.forEach(row => {
      console.log(`  ${row.year}年: ${row.tasks}任务, ${row.indicators}指标, ${row.milestones}里程碑`);
    });
    
    // 指标层级统计
    console.log('\n指标层级统计:');
    const levelStats = await client.query(`
      SELECT level, COUNT(*) as count FROM indicator GROUP BY level
    `);
    levelStats.rows.forEach(row => {
      console.log(`  ${row.level}: ${row.count} 个`);
    });
    
    return true;
  } catch (error) {
    console.error('❌ 数据库验证失败:', error.message);
    return false;
  } finally {
    await client.end();
  }
}

// ==================== API 验证 ====================

async function httpRequest(url, options = {}) {
  try {
    const response = await fetch(url, {
      ...options,
      headers: { 'Content-Type': 'application/json', ...options.headers },
    });
    const text = await response.text();
    let data;
    try { data = JSON.parse(text); } catch { data = text; }
    return { status: response.status, data, ok: response.ok };
  } catch (err) {
    return { status: 0, data: null, ok: false, error: err.message };
  }
}

async function verifyApi() {
  console.log('\n🌐 API 连接验证');
  console.log('═'.repeat(50));
  
  // 1. 尝试登录
  console.log(`API 地址: ${API_BASE}`);
  const loginResponse = await httpRequest(`${API_BASE}/auth/login`, {
    method: 'POST',
    body: JSON.stringify({ username: 'admin', password: '123456' })
  });
  
  if (!loginResponse.ok) {
    console.log('❌ 登录失败:', loginResponse.error || loginResponse.data?.message || loginResponse.status);
    console.log('   请确保后端服务正在运行');
    return false;
  }
  
  const token = loginResponse.data?.token;
  console.log('✅ 登录成功\n');
  
  // 2. 测试各 API 端点
  const endpoints = [
    { name: '组织机构', path: '/orgs' },
    { name: '战略任务', path: '/tasks' },
    { name: '指标列表', path: '/indicators' },
  ];
  
  console.log('API 端点测试:');
  for (const ep of endpoints) {
    const res = await httpRequest(`${API_BASE}${ep.path}`, {
      headers: { 'Authorization': `Bearer ${token}` }
    });
    const count = Array.isArray(res.data) ? res.data.length : (res.data?.data?.length || 0);
    console.log(`  ${ep.name.padEnd(12)} ${res.ok ? '✅' : '❌'} ${count} 条`);
  }
  
  return true;
}

// ==================== 主函数 ====================

async function main() {
  const args = process.argv.slice(2);
  const checkDb = args.includes('--db') || args.includes('--all') || args.length === 0;
  const checkApi = args.includes('--api') || args.includes('--all') || args.length === 0;
  
  console.log('╔════════════════════════════════════════════════════════╗');
  console.log('║           SISM 数据验证工具                            ║');
  console.log('╚════════════════════════════════════════════════════════╝');
  
  let success = true;
  
  if (checkDb) {
    success = await verifyDatabase() && success;
  }
  
  if (checkApi) {
    success = await verifyApi() && success;
  }
  
  console.log('\n' + '═'.repeat(50));
  console.log(success ? '✅ 验证完成' : '⚠️ 部分验证失败');
  
  process.exit(success ? 0 : 1);
}

main().catch(console.error);
