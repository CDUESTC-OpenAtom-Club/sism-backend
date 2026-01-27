/**
 * 主入口同步脚本
 * 按顺序执行 org → cycle → task → indicator → milestone 同步
 * 
 * 执行方式: node scripts/sync-all.js
 * 
 * Requirements: 1.1, 1.2, 1.3
 */

import { createSyncContext } from './sync-context.js';
import { syncOrg } from './phases/sync-org.js';
import { syncCycle } from './phases/sync-cycle.js';
import { syncTask } from './phases/sync-task.js';
import { syncIndicator } from './phases/sync-indicator.js';
import { syncMilestone } from './phases/sync-milestone.js';

/**
 * 同步阶段定义
 * 顺序很重要：后续阶段依赖前序阶段的 ID 映射
 */
const PHASES = [
  { name: 'org', fn: syncOrg, label: '组织机构' },
  { name: 'cycle', fn: syncCycle, label: '考核周期' },
  { name: 'task', fn: syncTask, label: '战略任务' },
  { name: 'indicator', fn: syncIndicator, label: '指标' },
  { name: 'milestone', fn: syncMilestone, label: '里程碑' }
];

/**
 * 执行所有同步阶段
 * @param {import('./sync-context.js').SyncContext} [existingCtx] - 可选的外部上下文（用于测试）
 * @returns {Promise<{success: boolean, ctx: import('./sync-context.js').SyncContext}>}
 */
export async function runAllSync(existingCtx = null) {
  const ctx = existingCtx || createSyncContext();
  
  console.log('╔════════════════════════════════════════════════════════╗');
  console.log('║           战略指标管理系统 - 数据同步工具              ║');
  console.log('╚════════════════════════════════════════════════════════╝\n');
  
  // 初始化数据库连接
  if (!existingCtx) {
    console.log('🔌 正在连接数据库...');
    const initialized = await ctx.init();
    if (!initialized) {
      console.error('\n❌ 数据库连接失败，同步终止');
      return { success: false, ctx };
    }
    console.log('✅ 数据库连接成功\n');
  }
  
  let success = true;
  let lastError = null;
  
  // 按顺序执行各阶段
  for (const phase of PHASES) {
    console.log(`\n${'═'.repeat(50)}`);
    console.log(`📦 开始同步: ${phase.label}`);
    console.log('═'.repeat(50));
    
    ctx.initPhaseStats(phase.name);
    
    try {
      const result = await phase.fn(ctx);
      
      if (!result.success) {
        console.error(`\n❌ ${phase.label}同步失败: ${result.error?.message || '未知错误'}`);
        success = false;
        lastError = result.error;
        // 错误中断：停止后续同步
        break;
      }
      
      const stats = ctx.getPhaseStats(phase.name);
      console.log(`\n✅ ${phase.label}同步完成: 新增 ${stats.inserted}, 跳过 ${stats.skipped}`);
      
    } catch (err) {
      console.error(`\n❌ ${phase.label}同步异常:`, err.message);
      ctx.recordError(phase.name);
      success = false;
      lastError = err;
      // 错误中断：停止后续同步
      break;
    }
  }
  
  // 输出汇总统计
  ctx.printSummary();
  
  // 关闭数据库连接（仅当不是外部传入的上下文时）
  if (!existingCtx) {
    await ctx.close();
  }
  
  if (success) {
    console.log('\n🎉 所有同步任务完成！');
  } else {
    console.log('\n⚠️ 同步过程中发生错误，部分数据可能未同步');
    if (lastError) {
      console.log(`   错误详情: ${lastError.message}`);
    }
  }
  
  return { success, ctx };
}

/**
 * 获取同步阶段列表（用于测试）
 * @returns {Array<{name: string, fn: Function, label: string}>}
 */
export function getPhases() {
  return PHASES;
}

// 如果直接运行此脚本
const isMainModule = import.meta.url === `file://${process.argv[1]}`;
if (isMainModule) {
  runAllSync()
    .then(({ success }) => {
      process.exit(success ? 0 : 1);
    })
    .catch(err => {
      console.error('同步脚本执行失败:', err);
      process.exit(1);
    });
}
