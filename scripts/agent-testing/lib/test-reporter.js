const fs = require('fs');
const path = require('path');

/**
 * 测试报告生成器
 * 负责收集测试结果并生成HTML报告
 */
class TestReporter {
  constructor(outputDir = './reports') {
    this.outputDir = outputDir;
    this.testRunId = this.generateTestRunId();
    this.results = {
      testRunId: this.testRunId,
      startTime: null,
      endTime: null,
      agents: [],
      summary: {
        totalTests: 0,
        passed: 0,
        failed: 0,
        skipped: 0
      }
    };

    // 确保输出目录存在
    if (!fs.existsSync(this.outputDir)) {
      fs.mkdirSync(this.outputDir, { recursive: true });
    }
  }

  /**
   * 生成测试运行ID
   */
  generateTestRunId() {
    const now = new Date();
    const date = now.toISOString().split('T')[0];
    const time = now.toTimeString().split(' ')[0].replace(/:/g, '-');
    return `${date}-${time}`;
  }

  /**
   * 开始测试
   */
  startTest() {
    this.results.startTime = new Date().toISOString();
  }

  /**
   * 结束测试
   */
  endTest() {
    this.results.endTime = new Date().toISOString();
  }

  /**
   * 添加Agent结果
   */
  addAgentResult(agentResult) {
    this.results.agents.push(agentResult);

    // 更新汇总
    this.results.summary.totalTests += agentResult.tests.total;
    this.results.summary.passed += agentResult.tests.passed;
    this.results.summary.failed += agentResult.tests.failed;
    this.results.summary.skipped += agentResult.tests.skipped;
  }

  /**
   * 生成JSON报告
   */
  generateJsonReport() {
    const jsonPath = path.join(this.outputDir, `test-report-${this.testRunId}.json`);
    fs.writeFileSync(jsonPath, JSON.stringify(this.results, null, 2));
    return jsonPath;
  }

  /**
   * 生成HTML报告
   */
  generateHtmlReport() {
    const html = this.generateHtmlContent();
    const htmlPath = path.join(this.outputDir, `test-report-${this.testRunId}.html`);
    fs.writeFileSync(htmlPath, html);
    return htmlPath;
  }

  /**
   * 生成HTML内容
   */
  generateHtmlContent() {
    const passRate = this.results.summary.totalTests > 0
      ? ((this.results.summary.passed / this.results.summary.totalTests) * 100).toFixed(1)
      : 0;

    const duration = this.results.startTime && this.results.endTime
      ? Math.round((new Date(this.results.endTime) - new Date(this.results.startTime)) / 1000)
      : 0;

    return `
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>SISM API测试报告 - ${this.testRunId}</title>
    <style>
        * { margin: 0; padding: 0; box-sizing: border-box; }
        body {
            font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
            background-color: #f5f5f5;
            padding: 20px;
            line-height: 1.6;
        }
        .container {
            max-width: 1200px;
            margin: 0 auto;
            background: white;
            padding: 30px;
            border-radius: 8px;
            box-shadow: 0 2px 10px rgba(0,0,0,0.1);
        }
        h1 { color: #333; margin-bottom: 10px; font-size: 28px; }
        .meta-info { color: #666; margin-bottom: 30px; font-size: 14px; }
        .summary {
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(200px, 1fr));
            gap: 20px;
            margin-bottom: 30px;
        }
        .summary-card {
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            color: white;
            padding: 20px;
            border-radius: 8px;
            box-shadow: 0 4px 6px rgba(0,0,0,0.1);
        }
        .summary-card h3 { font-size: 14px; opacity: 0.9; margin-bottom: 10px; }
        .summary-card .value { font-size: 32px; font-weight: bold; }
        .summary-card.passed { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); }
        .summary-card.failed { background: linear-gradient(135deg, #f093fb 0%, #f5576c 100%); }
        .summary-card.rate { background: linear-gradient(135deg, #4facfe 0%, #00f2fe 100%); }
        .summary-card.duration { background: linear-gradient(135deg, #43e97b 0%, #38f9d7 100%); }
        .agent-section { margin-bottom: 40px; }
        .agent-header {
            display: flex;
            align-items: center;
            justify-content: space-between;
            padding: 15px 20px;
            background: #f8f9fa;
            border-left: 4px solid #667eea;
            border-radius: 4px;
            margin-bottom: 15px;
        }
        .agent-name { font-size: 20px; font-weight: bold; color: #333; }
        .agent-status {
            padding: 5px 15px;
            border-radius: 20px;
            font-size: 14px;
            font-weight: 500;
        }
        .agent-status.completed { background: #d4edda; color: #155724; }
        .agent-status.failed { background: #f8d7da; color: #721c24; }
        .agent-stats {
            display: flex;
            gap: 20px;
            color: #666;
            font-size: 14px;
        }
        .scenario { margin-bottom: 20px; border: 1px solid #e0e0e0; border-radius: 8px; overflow: hidden; }
        .scenario-header {
            padding: 15px 20px;
            background: #fafafa;
            border-bottom: 1px solid #e0e0e0;
            display: flex;
            justify-content: space-between;
            align-items: center;
        }
        .scenario-name { font-weight: 600; color: #333; }
        .scenario-status {
            padding: 4px 12px;
            border-radius: 12px;
            font-size: 12px;
            font-weight: 500;
        }
        .scenario-status.passed { background: #d4edda; color: #155724; }
        .scenario-status.failed { background: #f8d7da; color: #721c24; }
        .scenario-steps { padding: 15px 20px; }
        .step {
            display: flex;
            align-items: center;
            padding: 8px 0;
            font-size: 14px;
        }
        .step-icon {
            width: 24px;
            height: 24px;
            border-radius: 50%;
            display: flex;
            align-items: center;
            justify-content: center;
            margin-right: 10px;
            font-size: 12px;
            font-weight: bold;
        }
        .step.passed .step-icon { background: #d4edda; color: #155724; }
        .step.failed .step-icon { background: #f8d7da; color: #721c24; }
        .step-name { flex: 1; }
        .step-duration { color: #999; font-size: 12px; }
        .error-message {
            background: #fff3cd;
            border-left: 4px solid #ffc107;
            padding: 12px;
            margin: 10px 0;
            border-radius: 4px;
            color: #856404;
            font-size: 13px;
        }
        .footer {
            margin-top: 40px;
            padding-top: 20px;
            border-top: 1px solid #e0e0e0;
            text-align: center;
            color: #999;
            font-size: 14px;
        }
    </style>
</head>
<body>
    <div class="container">
        <h1>🧪 SISM API测试报告</h1>
        <div class="meta-info">
            测试运行ID: ${this.testRunId} |
            开始时间: ${this.results.startTime} |
            结束时间: ${this.results.endTime}
        </div>

        <div class="summary">
            <div class="summary-card passed">
                <h3>通过</h3>
                <div class="value">${this.results.summary.passed}</div>
            </div>
            <div class="summary-card failed">
                <h3>失败</h3>
                <div class="value">${this.results.summary.failed}</div>
            </div>
            <div class="summary-card rate">
                <h3>通过率</h3>
                <div class="value">${passRate}%</div>
            </div>
            <div class="summary-card duration">
                <h3>总耗时</h3>
                <div class="value">${duration}s</div>
            </div>
        </div>

        ${this.results.agents.map(agent => this.generateAgentSection(agent)).join('')}

        <div class="footer">
            SISM Multi-Agent API Testing Framework v1.0
        </div>
    </div>
</body>
</html>
    `;
  }

  /**
   * 生成Agent部分HTML
   */
  generateAgentSection(agent) {
    const agentPassRate = agent.tests.total > 0
      ? ((agent.tests.passed / agent.tests.total) * 100).toFixed(1)
      : 0;

    return `
      <div class="agent-section">
          <div class="agent-header">
              <div class="agent-name">${agent.name}</div>
              <div class="agent-status ${agent.status}">${this.getStatusText(agent.status)}</div>
          </div>
          <div class="agent-stats">
              <span>总测试: ${agent.tests.total}</span>
              <span>通过: ${agent.tests.passed}</span>
              <span>失败: ${agent.tests.failed}</span>
              <span>跳过: ${agent.tests.skipped}</span>
              <span>通过率: ${agentPassRate}%</span>
          </div>
          ${agent.scenarios ? agent.scenarios.map(scenario => this.generateScenarioSection(scenario)).join('') : ''}
      </div>
    `;
  }

  /**
   * 生成场景部分HTML
   */
  generateScenarioSection(scenario) {
    return `
      <div class="scenario">
          <div class="scenario-header">
              <div class="scenario-name">${scenario.name}</div>
              <div class="scenario-status ${scenario.status}">${this.getStatusText(scenario.status)}</div>
          </div>
          ${scenario.description ? `<div style="padding: 10px 20px; color: #666; font-size: 13px;">${scenario.description}</div>` : ''}
          ${scenario.steps ? `
              <div class="scenario-steps">
                  ${scenario.steps.map(step => this.generateStepSection(step)).join('')}
              </div>
          ` : ''}
          ${scenario.error ? `<div class="error-message">${scenario.error}</div>` : ''}
      </div>
    `;
  }

  /**
   * 生成步骤部分HTML
   */
  generateStepSection(step) {
    const icon = step.status === 'passed' ? '✓' : '✗';
    const duration = step.duration ? `<span class="step-duration">${step.duration}ms</span>` : '';

    return `
      <div class="step ${step.status}">
          <div class="step-icon">${icon}</div>
          <div class="step-name">${step.name}</div>
          ${duration}
      </div>
      ${step.error ? `<div class="error-message">${step.error}</div>` : ''}
    `;
  }

  /**
   * 获取状态文本
   */
  getStatusText(status) {
    const statusMap = {
      'passed': '通过',
      'failed': '失败',
      'completed': '已完成',
      'skipped': '跳过',
      'pending': '待执行'
    };
    return statusMap[status] || status;
  }

  /**
   * 生成控制台输出
   */
  generateConsoleReport() {
    console.log('\n' + '='.repeat(80));
    console.log('🧪 SISM API测试报告');
    console.log('='.repeat(80));
    console.log(`测试运行ID: ${this.testRunId}`);
    console.log(`开始时间: ${this.results.startTime}`);
    console.log(`结束时间: ${this.results.endTime}`);
    console.log('\n' + '-'.repeat(80));
    console.log('测试汇总:');
    console.log(`  总测试数: ${this.results.summary.totalTests}`);
    console.log(`  通过: ${this.results.summary.passed}`);
    console.log(`  失败: ${this.results.summary.failed}`);
    console.log(`  跳过: ${this.results.summary.skipped}`);
    console.log(`  通过率: ${this.results.summary.totalTests > 0 ? ((this.results.summary.passed / this.results.summary.totalTests) * 100).toFixed(1) : 0}%`);
    console.log('\n' + '-'.repeat(80));

    for (const agent of this.results.agents) {
      console.log(`\n【${agent.name}】`);
      console.log(`  状态: ${this.getStatusText(agent.status)}`);
      console.log(`  测试: ${agent.tests.total} | 通过: ${agent.tests.passed} | 失败: ${agent.tests.failed} | 跳过: ${agent.tests.skipped}`);

      if (agent.scenarios) {
        for (const scenario of agent.scenarios) {
          console.log(`\n  场景: ${scenario.name}`);
          console.log(`    状态: ${this.getStatusText(scenario.status)}`);

          if (scenario.steps) {
            for (const step of scenario.steps) {
              const icon = step.status === 'passed' ? '✓' : '✗';
              console.log(`    ${icon} ${step.name} ${step.duration ? `(${step.duration}ms)` : ''}`);
            }
          }

          if (scenario.error) {
            console.log(`    错误: ${scenario.error}`);
          }
        }
      }
    }

    console.log('\n' + '='.repeat(80));
    console.log(`报告已生成: ${path.join(this.outputDir, `test-report-${this.testRunId}.html`)}`);
    console.log('='.repeat(80) + '\n');
  }

  /**
   * 生成所有报告
   */
  generateReports() {
    const jsonPath = this.generateJsonReport();
    const htmlPath = this.generateHtmlReport();
    this.generateConsoleReport();

    return {
      json: jsonPath,
      html: htmlPath
    };
  }
}

module.exports = TestReporter;
