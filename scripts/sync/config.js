/**
 * 同步配置模块
 * 从环境变量读取数据库连接信息，移除所有硬编码的敏感信息
 * 
 * Requirements: 1.4, 5.2
 */

import { config } from 'dotenv';
import { fileURLToPath } from 'url';
import { dirname, resolve } from 'path';

// 获取当前文件目录
const __filename = fileURLToPath(import.meta.url);
const __dirname = dirname(__filename);

// 加载 .env 文件（优先从项目根目录加载）
config({ path: resolve(__dirname, '../.env') });

/**
 * 数据库配置
 * 所有敏感信息从环境变量读取
 */
export const dbConfig = {
  host: process.env.DB_HOST || 'localhost',
  port: parseInt(process.env.DB_PORT || '5432', 10),
  database: process.env.DB_NAME || 'strategic',
  user: process.env.DB_USER || 'postgres',
  password: process.env.DB_PASSWORD || ''
};

/**
 * 验证必要的环境变量是否已配置
 * @returns {boolean} 配置是否有效
 */
export function validateConfig() {
  const required = ['DB_HOST', 'DB_PORT', 'DB_NAME', 'DB_USER', 'DB_PASSWORD'];
  const missing = required.filter(key => !process.env[key]);
  
  if (missing.length > 0) {
    console.error('❌ 缺少必要的环境变量:');
    missing.forEach(key => console.error(`   - ${key}`));
    console.error('\n请检查 .env 文件或设置环境变量');
    return false;
  }
  
  return true;
}

/**
 * 获取数据库连接池配置
 * @returns {object} pg.Pool 配置对象
 */
export function getPoolConfig() {
  return {
    ...dbConfig,
    max: 10,
    idleTimeoutMillis: 30000,
    connectionTimeoutMillis: 5000
  };
}

export default {
  dbConfig,
  validateConfig,
  getPoolConfig
};
