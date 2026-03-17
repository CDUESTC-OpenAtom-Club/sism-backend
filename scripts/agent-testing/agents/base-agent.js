const { ApiClient } = require('../lib/api-client');
const DataFactory = require('../lib/data-factory');
const AssertionLibrary = require('../lib/assertions');

/**
 * 基础Agent类
 * 所有Agent的父类
 */
class BaseAgent {
  constructor(name, config) {
    this.name = name;
    this.config = config;
    this.apiClient = new ApiClient(config);
    this.dataFactory = new DataFactory();
    this.assertions = AssertionLibrary;

    this.results = {
      name: name,
      status: 'pending',
      tests: {
        total: 0,
        passed: 0,
        failed: 0,
        skipped: 0
      },
      scenarios: [],
      startTime: null,
      endTime: null
    };

    this.storedData = {}; // 存储测试数据供后续使用
  }

  /**
   * 开始测试
   */
  async start() {
    this.results.status = 'running';
    this.results.startTime = new Date().toISOString();
    console.log(`\n🚀 [${this.name}] 开始测试...`);
  }

  /**
   * 结束测试
   */
  async end() {
    this.results.endTime = new Date().toISOString();

    if (this.results.tests.failed === 0) {
      this.results.status = 'completed';
    } else {
      this.results.status = 'failed';
    }

    console.log(`✅ [${this.name}] 测试完成`);
    console.log(`   总计: ${this.results.tests.total} | 通过: ${this.results.tests.passed} | 失败: ${this.results.tests.failed} | 跳过: ${this.results.tests.skipped}`);
  }

  /**
   * 运行测试场景
   */
  async runScenario(scenarioName, scenarioFn) {
    const scenario = {
      name: scenarioName,
      description: '',
      status: 'pending',
      steps: [],
      error: null
    };

    this.results.scenarios.push(scenario);
    this.results.tests.total++;

    try {
      console.log(`\n  📋 场景: ${scenarioName}`);
      await scenarioFn(scenario);
      scenario.status = 'passed';
      this.results.tests.passed++;
    } catch (error) {
      scenario.status = 'failed';
      scenario.error = error.message;
      this.results.tests.failed++;
      console.error(`  ❌ 场景失败: ${error.message}`);
    }
  }

  /**
   * 运行测试步骤
   */
  async runStep(stepName, stepFn, scenario) {
    const step = {
      name: stepName,
      status: 'pending',
      duration: null,
      error: null
    };

    scenario.steps.push(step);

    try {
      const startTime = Date.now();
      console.log(`    ⚙️  ${stepName}`);
      await stepFn();
      step.duration = Date.now() - startTime;
      step.status = 'passed';
    } catch (error) {
      step.status = 'failed';
      step.error = error.message;
      throw error;
    }
  }

  /**
   * 登录
   */
  async login(username, password) {
    try {
      const result = await this.apiClient.login(username, password);
      console.log(`    ✅ 登录成功: ${username}`);
      return result;
    } catch (error) {
      console.error(`    ❌ 登录失败: ${error.message}`);
      throw error;
    }
  }

  /**
   * 登出
   */
  async logout() {
    try {
      await this.apiClient.logout();
      console.log(`    ✅ 登出成功`);
    } catch (error) {
      console.warn(`    ⚠️  登出失败: ${error.message}`);
    }
  }

  /**
   * 存储数据
   */
  storeData(key, value) {
    this.storedData[key] = value;
  }

  /**
   * 获取存储的数据
   */
  getStoredData(key) {
    return this.storedData[key];
  }

  /**
   * 获取测试结果
   */
  getResults() {
    return this.results;
  }

  /**
   * 获取存储的所有数据
   */
  getAllStoredData() {
    return this.storedData;
  }

  /**
   * 设置存储的数据（用于Agent间数据共享）
   */
  setStoredData(data) {
    this.storedData = { ...this.storedData, ...data };
  }
}

module.exports = BaseAgent;
