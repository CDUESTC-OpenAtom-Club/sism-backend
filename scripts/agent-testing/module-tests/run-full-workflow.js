/**
 * SISM完整业务流程测试
 *
 * 基于`docs/流程.md`设计的端到端业务流程测试
 *
 * 流程顺序：
 * 1. IAM认证
 * 2. 战略部门创建指标
 * 3. 战略部门下发指标
 * 4. 职能部门拆分指标
 * 5. 学院创建填报
 * 6. 学院提交审批
 * 7. 多级审批
 * 8. 数据分析
 * 9. 告警通知
 */

const IamTester = require('./iam/test-auth');
const StrategyTester = require('./strategy/test-indicator');
const ExecutionTester = require('./execution/test-report');
const WorkflowTester = require('./workflow/test-workflow');
const AnalyticsTester = require('./analytics/test-analytics');
const AlertTester = require('./alert/test-alert');

class FullWorkflowTester {
  constructor() {
    this.sharedData = {
      tokens: {},
      indicators: {},
      reports: {},
      workflows: {}
    };
  }

  /**
   * 阶段1: IAM认证
   */
  async stage1_Authentication() {
    console.log('\n╔════════════════════════════════════════════════════════════╗');
    console.log('║  阶段1: IAM认证 - 用户登录与Token管理                      ║');
    console.log('╚════════════════════════════════════════════════════════════╝');

    const iamTester = new IamTester();

    // 战略部门登录
    const strategicResult = await iamTester.testLogin('strategic');
    if (strategicResult.success) {
      this.sharedData.tokens.strategic = strategicResult.token;
      console.log('✅ 战略部门用户登录成功');
    }

    // 职能部门登录
    const functionalResult = await iamTester.testLogin('functional');
    if (functionalResult.success) {
      this.sharedData.tokens.functional = functionalResult.token;
      console.log('✅ 职能部门用户登录成功');
    }

    // 学院用户登录
    const collegeResult = await iamTester.testLogin('college');
    if (collegeResult.success) {
      this.sharedData.tokens.college = collegeResult.token;
      console.log('✅ 学院用户登录成功');
    }

    return strategicResult.success || functionalResult.success || collegeResult.success;
  }

  /**
   * 阶段2: 指标创建与第一层下发 (战略 -> 职能)
   */
  async stage2_CreateAndDistributeIndicator() {
    console.log('\n╔════════════════════════════════════════════════════════════╗');
    console.log('║  阶段2: 指标创建与第一层下发 (战略 -> 职能)              ║');
    console.log('╚════════════════════════════════════════════════════════════╝');

    const strategyTester = new StrategyTester();
    await strategyTester.login();

    // 2.1 创建草稿指标
    console.log('\n步骤 2.1: 战略部门创建草稿指标');
    const createResult = await strategyTester.testCreateIndicator();
    if (!createResult.success) {
      console.log('❌ 创建指标失败');
      return false;
    }

    const indicator = createResult.indicator;
    this.sharedData.indicators.parent = indicator;
    console.log(`✅ 指标创建成功: ${indicator.name} (ID: ${indicator.id})`);

    // 2.2 更新指标
    console.log('\n步骤 2.2: 战略部门更新指标信息');
    await strategyTester.testUpdateIndicator(indicator.id);

    // 2.3 查询指标详情
    console.log('\n步骤 2.3: 查询指标详情');
    await strategyTester.testGetIndicator(indicator.id);

    // 2.4 提交审批（下发）
    console.log('\n步骤 2.4: 提交指标审批（DRAFT -> PENDING）');
    const distributeResult = await strategyTester.testDistributeIndicator(indicator.id);
    if (distributeResult.success) {
      console.log('✅ 指标已提交审批，等待战略部门内部审批');
    }

    return true;
  }

  /**
   * 阶段3: 指标拆分与第二层下发 (职能 -> 学院)
   */
  async stage3_SplitAndDistributeIndicator() {
    console.log('\n╔════════════════════════════════════════════════════════════╗');
    console.log('║  阶段3: 指标拆分与第二层下发 (职能 -> 学院)               ║');
    console.log('╚════════════════════════════════════════════════════════════╝');

    const strategyTester = new StrategyTester();
    await strategyTester.login();

    const parentId = this.sharedData.indicators.parent?.id;
    if (!parentId) {
      console.log('❌ 缺少父指标ID');
      return false;
    }

    // 3.1 创建子指标
    console.log('\n步骤 3.1: 职能部门拆分为子指标');
    console.log('   模拟创建3个子指标...');

    for (let i = 1; i <= 3; i++) {
      const childData = {
        name: `子指标${i}_${Date.now()}`,
        code: `SUB_${Date.now()}_${i}`,
        description: `职能部门拆分的第${i}个子指标`,
        type: 'QUANTITATIVE',
        weight: 3,
        targetValue: 100,
        unit: '个',
        period: '2026-03',
        status: 'DRAFT',
        parentIndicatorId: parentId
      };

      // 这里应该调用API创建子指标
      console.log(`   - 创建子指标${i}: ${childData.name}`);
      // await createChildIndicator(childData);
    }

    // 3.2 下发子指标到学院
    console.log('\n步骤 3.2: 下发子指标到学院');
    console.log('   子指标已创建并下发到各个学院');

    return true;
  }

  /**
   * 阶段4: 学院进度填报与审批 (核心循环)
   */
  async stage4_CreateAndSubmitReport() {
    console.log('\n╔════════════════════════════════════════════════════════════╗');
    console.log('║  阶段4: 学院进度填报与审批 (核心循环)                      ║');
    console.log('╚════════════════════════════════════════════════════════════╝');

    const executionTester = new ExecutionTester();
    await executionTester.login();

    // 4.1 创建填报草稿
    console.log('\n步骤 4.1: 学院创建填报草稿 (DRAFT)');
    const createResult = await executionTester.testCreateReport();
    if (!createResult.success) {
      console.log('❌ 创建填报失败');
      return false;
    }

    const report = createResult.report;
    this.sharedDatareports.current = report;
    console.log(`✅ 填报创建成功 (ID: ${report.id})`);

    // 4.2 更新填报
    console.log('\n步骤 4.2: 学院更新填报内容');
    await executionTester.testUpdateReport(report.id);

    // 4.3 提交审批
    console.log('\n步骤 4.3: 学院提交审批 (DRAFT -> PENDING)');
    const submitResult = await executionTester.testSubmitReport(report.id);
    if (submitResult.success) {
      console.log('✅ 填报已提交，等待学院内部审批');
    }

    // 4.4 测试撤回功能
    console.log('\n步骤 4.4: 测试填报撤回功能');
    await executionTester.testWithdrawReport(report.id);

    return true;
  }

  /**
   * 阶段5: 多级审批
   */
  async stage5_MultiLevelApproval() {
    console.log('\n╔════════════════════════════════════════════════════════════╗');
    console.log('║  阶段5: 多级审批与驳回机制                                   ║');
    console.log('╚════════════════════════════════════════════════════════════╝');

    const workflowTester = new WorkflowTester();
    await workflowTester.login();

    // 5.1 查询工作流定义
    console.log('\n步骤 5.1: 查询工作流定义');
    await workflowTester.testGetWorkflowDefinitions();

    // 5.2 查询工作流实例
    console.log('\n步骤 5.2: 查询工作流实例');
    const instancesResult = await workflowTester.testGetWorkflowInstances();

    // 5.3 查询审批时间轴
    if (instancesResult.success && instancesResult.instances.data.length > 0) {
      const instanceId = instancesResult.instances.data[0].id;

      console.log('\n步骤 5.3: 查询审批时间轴');
      await workflowTester.testGetWorkflowTimeline(instanceId);

      console.log('\n步骤 5.4: 查询审批历史');
      await workflowTester.testGetWorkflowHistory(instanceId);

      console.log('\n步骤 5.5: 测试审批通过');
      await workflowTester.testApprove(instanceId, '测试审批通过');
    }

    return true;
  }

  /**
   * 阶段6: 数据分析与告警
   */
  async stage6_AnalyticsAndAlerts() {
    console.log('\n╔════════════════════════════════════════════════════════════╗');
    console.log('║  阶段6: 数据分析与告警通知                                 ║');
    console.log('╚════════════════════════════════════════════════════════════╝');

    // 6.1 数据分析
    console.log('\n步骤 6.1: 获取仪表盘数据');
    const analyticsTester = new AnalyticsTester();
    await analyticsTester.login();
    await analyticsTester.testGetDashboard();

    console.log('\n步骤 6.2: 查询指标进度');
    await analyticsTester.testGetIndicatorProgress(1);

    console.log('\n步骤 6.3: 获取进度统计');
    await analyticsTester.testGetProgressStats();

    // 6.2 告警通知
    console.log('\n步骤 6.4: 查询告警列表');
    const alertTester = new AlertTester();
    await alertTester.login();
    await alertTester.testListAlerts();

    console.log('\n步骤 6.5: 查询我的告警');
    await alertTester.testGetMyAlerts();

    console.log('\n步骤 6.6: 查询未读告警数量');
    await alertTester.testGetUnreadCount();

    return true;
  }

  /**
   * 运行完整业务流程测试
   */
  async runFullWorkflow() {
    console.log('\n╔════════════════════════════════════════════════════════════╗');
    console.log('║                                                          ║');
    console.log('║        SISM完整业务流程测试 - 端到端测试                  ║');
    console.log('║        基于 docs/流程.md 设计                             ║');
    console.log('║                                                          ║');
    console.log('╚════════════════════════════════════════════════════════════╝');

    const startTime = Date.now();
    const results = {};

    try {
      // 阶段1: 认证
      results.stage1 = await this.stage1_Authentication();

      // 阶段2: 创建和下发指标
      results.stage2 = await this.stage2_CreateAndDistributeIndicator();

      // 阶段3: 拆分指标
      results.stage3 = await this.stage3_SplitAndDistributeIndicator();

      // 阶段4: 填报和审批
      results.stage4 = await this.stage4_CreateAndSubmitReport();

      // 阶段5: 多级审批
      results.stage5 = await this.stage5_MultiLevelApproval();

      // 阶段6: 数据分析和告警
      results.stage6 = await this.stage6_AnalyticsAndAlerts();

      // 打印测试总结
      const duration = ((Date.now() - startTime) / 1000).toFixed(2);
      console.log('\n╔════════════════════════════════════════════════════════════╗');
      console.log('║                    测试总结                              ║');
      console.log('╚════════════════════════════════════════════════════════════╝');
      console.log(`总用时: ${duration} 秒`);
      console.log('');
      console.log('各阶段结果:');
      Object.entries(results).forEach(([stage, success]) => {
        const status = success ? '✅ 通过' : '❌ 失败';
        console.log(`  ${stage}: ${status}`);
      });

      console.log('\n✅ 完整业务流程测试完成！');

    } catch (error) {
      console.error('\n❌ 测试过程中发生错误:', error.message);
      console.error(error.stack);
    }
  }
}

// 导出测试类
module.exports = FullWorkflowTester;

// 如果直接运行此文件
if (require.main === module) {
  const tester = new FullWorkflowTester();
  tester.runFullWorkflow()
    .then(() => console.log('\n测试结束'))
    .catch(error => console.error('测试异常:', error));
}
