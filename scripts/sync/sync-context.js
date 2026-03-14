/**
 * 同步上下文模块
 * 管理同步过程中的 ID 映射表和统计信息
 * 
 * Requirements: 1.3
 */

import pg from 'pg';
import { getPoolConfig, validateConfig } from './config.js';

/**
 * @typedef {Object} PhaseStats
 * @property {number} inserted - 新增记录数
 * @property {number} skipped - 跳过记录数
 * @property {number} errors - 错误记录数
 */

/**
 * @typedef {Object} PhaseResult
 * @property {boolean} success - 是否成功
 * @property {number} inserted - 新增记录数
 * @property {number} skipped - 跳过记录数
 * @property {Error} [error] - 错误信息（如果失败）
 */

/**
 * 同步上下文类
 * 在各同步阶段之间共享 ID 映射和统计信息
 */
export class SyncContext {
  constructor() {
    /** @type {pg.Pool | null} */
    this.pool = null;
    
    /**
     * ID 映射表
     * @type {{
     *   org: Map<string, number>,
     *   task: Map<string, number>,
     *   indicator: Map<string, number>,
     *   cycle: Map<number, number>
     * }}
     */
    this.maps = {
      org: new Map(),      // 组织名称 → org_id
      task: new Map(),     // 任务名称_年份 → task_id
      indicator: new Map(), // 指标前端ID → indicator_id
      cycle: new Map()     // 年份 → cycle_id
    };
    
    /**
     * 统计信息
     * @type {Record<string, PhaseStats>}
     */
    this.stats = {};
  }
  
  /**
   * 初始化数据库连接池
   * @returns {Promise<boolean>} 是否成功初始化
   */
  async init() {
    if (!validateConfig()) {
      return false;
    }
    
    try {
      this.pool = new pg.Pool(getPoolConfig());
      // 测试连接
      const client = await this.pool.connect();
      client.release();
      return true;
    } catch (err) {
      console.error('❌ 数据库连接失败:', err.message);
      return false;
    }
  }
  
  /**
   * 关闭数据库连接池
   */
  async close() {
    if (this.pool) {
      await this.pool.end();
      this.pool = null;
    }
  }
  
  /**
   * 获取数据库客户端
   * @returns {Promise<pg.PoolClient>}
   */
  async getClient() {
    if (!this.pool) {
      throw new Error('数据库连接池未初始化');
    }
    return this.pool.connect();
  }
  
  /**
   * 初始化阶段统计
   * @param {string} phaseName - 阶段名称
   */
  initPhaseStats(phaseName) {
    this.stats[phaseName] = {
      inserted: 0,
      skipped: 0,
      errors: 0
    };
  }
  
  /**
   * 记录新增
   * @param {string} phaseName - 阶段名称
   */
  recordInsert(phaseName) {
    if (this.stats[phaseName]) {
      this.stats[phaseName].inserted++;
    }
  }
  
  /**
   * 记录跳过
   * @param {string} phaseName - 阶段名称
   */
  recordSkip(phaseName) {
    if (this.stats[phaseName]) {
      this.stats[phaseName].skipped++;
    }
  }
  
  /**
   * 记录错误
   * @param {string} phaseName - 阶段名称
   */
  recordError(phaseName) {
    if (this.stats[phaseName]) {
      this.stats[phaseName].errors++;
    }
  }
  
  /**
   * 获取阶段统计
   * @param {string} phaseName - 阶段名称
   * @returns {PhaseStats | undefined}
   */
  getPhaseStats(phaseName) {
    return this.stats[phaseName];
  }
  
  /**
   * 获取所有统计信息
   * @returns {Record<string, PhaseStats>}
   */
  getAllStats() {
    return this.stats;
  }
  
  /**
   * 打印汇总统计表
   */
  printSummary() {
    console.log('\n╔════════════════════════════════════════════════════════╗');
    console.log('║                    同步汇总统计                        ║');
    console.log('╠════════════════════════════════════════════════════════╣');
    console.log('║  阶段          │  新增  │  跳过  │  错误  │  状态      ║');
    console.log('╠════════════════════════════════════════════════════════╣');
    
    const phases = ['org', 'cycle', 'task', 'indicator', 'milestone'];
    const phaseNames = {
      org: '组织机构',
      cycle: '考核周期',
      task: '战略任务',
      indicator: '指标',
      milestone: '里程碑'
    };
    
    let totalInserted = 0;
    let totalSkipped = 0;
    let totalErrors = 0;
    
    for (const phase of phases) {
      const stat = this.stats[phase];
      if (stat) {
        const status = stat.errors > 0 ? '❌ 失败' : '✅ 成功';
        const name = phaseNames[phase].padEnd(10, '　');
        console.log(`║  ${name}  │  ${String(stat.inserted).padStart(4)}  │  ${String(stat.skipped).padStart(4)}  │  ${String(stat.errors).padStart(4)}  │  ${status}  ║`);
        totalInserted += stat.inserted;
        totalSkipped += stat.skipped;
        totalErrors += stat.errors;
      }
    }
    
    console.log('╠════════════════════════════════════════════════════════╣');
    console.log(`║  总计          │  ${String(totalInserted).padStart(4)}  │  ${String(totalSkipped).padStart(4)}  │  ${String(totalErrors).padStart(4)}  │            ║`);
    console.log('╚════════════════════════════════════════════════════════╝');
  }
}

/**
 * 创建新的同步上下文实例
 * @returns {SyncContext}
 */
export function createSyncContext() {
  return new SyncContext();
}

export default {
  SyncContext,
  createSyncContext
};
