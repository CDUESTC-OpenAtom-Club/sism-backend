const TestReporter = require('../lib/test-reporter');
const IamAgent = require('./iam-agent');
const StrategyAgent = require('./strategy-agent');
const FunctionalAgent = require('./functional-agent');
const CollegeAgent = require('./college-agent');
const WorkflowAgent = require('./workflow-agent');

/**
 * 主控Agent
 * 负责协调所有测试Agent的执行顺序、收集结果、生成报告
 */
class MasterAgent {
  constructor(configPath) {
    const fs = require('fs');
    const path = require('path');

    // 加载配置
    const configContent = fs.readFileSync(configPath, 'utf8');
    this.config = JSON.parse(configContent);

    // 创建报告生成器
    this.reporter = new TestReporter('./reports');

    // Agent执行顺序（按依赖关系）
    this.agentExecutionOrder = [
      { name: 'IAM', Agent: IamAgent, dependencies: [] },
      { name: 'Strategy', Agent: StrategyAgent, dependencies: ['IAM'] },
      { name: 'Functional', Agent: FunctionalAgent, dependencies: ['IAM', 'Strategy'] },
      { name: 'College', Agent: CollegeAgent, dependencies: ['IAM', 'Strategy'] },
      { name: 'Workflow', Agent: WorkflowAgent, dependencies: ['IAM', 'Strategy', 'Functional', 'College'] }
    ];

    this.sharedData = {};
    this.results = [];
  }

  /**
   * 执行所有测试
   */
  async execute(mode = 'sequential') {
    console.log('\n' + '='.repeat(80));
    console.log('🚀 SISM 多Agent API测试框架');
    console.log('='.repeat(80));
    console.log(`执行模式: ${mode === 'sequential' ? '顺序执行' : '并行执行'}`);
    console.log(`测试时间: ${new Date().toLocaleString('zh-CN')}`);
    console.log('='.repeat(80));

    this.reporter.startTest();

    try {
      if (mode === 'sequential') {
        await this.executeSequential();
      } else {
        await this.executeParallel();
      }

      // 生成测试报告
      const reportPaths = this.reporter.generateReports();

      console.log('\n' + '='.repeat(80));
      console.log('✅ 测试完成');
      console.log('='.repeat(80));
      console.log(`JSON报告: ${reportPaths.json}`);
      console.log(`HTML报告: ${reportPaths.html}`);
      console.log('='.repeat(80) + '\n');

      return {
        success: true,
        reportPaths
      };

    } catch (error) {
      console.error('\n❌ 测试执行失败:', error.message);
      this.reporter.endTest();
      throw error;
    }
  }

  /**
   * 顺序执行Agent
   */
  async executeSequential() {
    for (const agentConfig of this.agentExecutionOrder) {
      try {
        console.log(`\n📌 准备执行: ${agentConfig.name}Agent`);

        // 验证依赖
        this.checkDependencies(agentConfig);

        // 创建Agent实例
        const agent = new agentConfig.Agent(this.config, this.sharedData);

        // 执行测试
        const result = await agent.execute();

        // 收集结果
        if (result.results) {
          this.reporter.addAgentResult(result.results);
        }

        // 更新共享数据
        if (result.storedData) {
          this.sharedData = { ...this.sharedData, ...result.storedData };
        }

        this.results.push({
          name: agentConfig.name,
          result: result.results
        });

        // 检查是否应该继续
        if (result.results && result.results.tests.failed > 0) {
          console.warn(`⚠️  ${agentConfig.name}Agent 有失败的测试，但继续执行后续Agent`);
        }

      } catch (error) {
        console.error(`❌ ${agentConfig.name}Agent 执行失败:`, error.message);

        // 记录失败结果
        this.reporter.addAgentResult({
          name: agentConfig.name,
          status: 'failed',
          tests: { total: 0, passed: 0, failed: 1, skipped: 0 },
          scenarios: [{
            name: 'Agent执行',
            status: 'failed',
            error: error.message
          }]
        });

        // 关键Agent失败则停止
        if (agentConfig.name === 'IAM') {
          throw new Error(`关键Agent ${agentConfig.name} 执行失败，停止测试`);
        }
      }
    }
  }

  /**
   * 并行执行Agent（仅执行无依赖关系的Agent）
   */
  async executeParallel() {
    // 分组：可以并行执行的Agent
    const parallelGroups = [
      ['IAM'], // 第一组：IAM必须先执行
      ['Strategy'], // 第二组：依赖IAM
      ['Functional', 'College'], // 第三组：依赖Strategy，可以并行
      ['Workflow'] // 第四组：依赖所有前面的Agent
    ];

    for (const group of parallelGroups) {
      console.log(`\n📌 并行执行组: ${group.join(', ')}`);

      const promises = group.map(agentName => {
        const agentConfig = this.agentExecutionOrder.find(a => a.name === agentName);
        return this.executeAgent(agentConfig);
      });

      const results = await Promise.allSettled(promises);

      // 处理结果
      for (let i = 0; i < results.length; i++) {
        const result = results[i];
        const agentName = group[i];

        if (result.status === 'fulfilled') {
          console.log(`✅ ${agentName}Agent 完成`);

          if (result.value.results) {
            this.reporter.addAgentResult(result.value.results);
          }

          if (result.value.storedData) {
            this.sharedData = { ...this.sharedData, ...result.value.storedData };
          }
        } else {
          console.error(`❌ ${agentName}Agent 失败:`, result.reason.message);

          // 关键Agent失败则停止
          if (agentName === 'IAM') {
            throw new Error(`关键Agent ${agentName} 执行失败，停止测试`);
          }
        }
      }
    }
  }

  /**
   * 执行单个Agent
   */
  async executeAgent(agentConfig) {
    const Agent = agentConfig.Agent;
    const agent = new Agent(this.config, this.sharedData);
    return await agent.execute();
  }

  /**
   * 检查依赖
   */
  checkDependencies(agentConfig) {
    for (const dep of agentConfig.dependencies) {
      const depResult = this.results.find(r => r.name === dep);
      if (!depResult) {
        throw new Error(`${agentConfig.name}Agent 依赖 ${dep}Agent，但它还未执行`);
      }

      if (depResult.result && depResult.result.tests.failed > 0) {
        console.warn(`⚠️  依赖 ${dep}Agent 有失败的测试`);
      }
    }
  }

  /**
   * 执行指定的工作流场景
   */
  async executeWorkflow(workflowId) {
    console.log(`\n🎯 执行指定工作流: ${workflowId}`);

    // 根据workflowId选择要执行的Agent
    const workflowAgents = {
      'indicator-distribution': ['IAM', 'Strategy'],
      'indicator-split': ['IAM', 'Strategy', 'Functional'],
      'report-submission': ['IAM', 'Strategy', 'College'],
      'multi-level-approval': ['IAM', 'Strategy', 'College', 'Workflow'],
      'approval-rejection': ['IAM', 'Strategy', 'Workflow'],
      'approval-timeline': ['IAM', 'Strategy', 'Workflow'],
      'report-withdrawal': ['IAM', 'Strategy', 'College'],
      'parent-child-relationship': ['IAM', 'Strategy', 'Functional'],
      'period-identification': ['IAM', 'Strategy'],
      'data-change-tracking': ['IAM', 'Strategy', 'College', 'Workflow']
    };

    const agentsToRun = workflowAgents[workflowId];
    if (!agentsToRun) {
      throw new Error(`未知的工作流ID: ${workflowId}`);
    }

    this.reporter.startTest();

    for (const agentName of agentsToRun) {
      const agentConfig = this.agentExecutionOrder.find(a => a.name === agentName);
      if (agentConfig) {
        const result = await this.executeAgent(agentConfig);

        if (result.results) {
          this.reporter.addAgentResult(result.results);
        }

        if (result.storedData) {
          this.sharedData = { ...this.sharedData, ...result.storedData };
        }
      }
    }

    const reportPaths = this.reporter.generateReports();
    return reportPaths;
  }
}

// CLI入口
if (require.main === module) {
  const args = process.argv.slice(2);
  const configPath = './config/test-users.json';
  const mode = args.includes('--parallel') ? 'parallel' : 'sequential';

  const master = new MasterAgent(configPath);

  // 检查是否指定了工作流
  const workflowIndex = args.indexOf('--workflow');
  if (workflowIndex !== -1 && args[workflowIndex + 1]) {
    const workflowId = args[workflowIndex + 1];
    master.executeWorkflow(workflowId)
      .then(() => process.exit(0))
      .catch(error => {
        console.error('执行失败:', error);
        process.exit(1);
      });
  } else {
    master.execute(mode)
      .then(() => process.exit(0))
      .catch(error => {
        console.error('执行失败:', error);
        process.exit(1);
      });
  }
}

module.exports = MasterAgent;
