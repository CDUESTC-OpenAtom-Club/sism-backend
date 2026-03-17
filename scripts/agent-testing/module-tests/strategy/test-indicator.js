/**
 * SISM Strategy模块 - 指标管理API测试
 *
 * 测试流程：
 * 1. 创建草稿指标
 * 2. 更新指标
 * 3. 提交审批
 * 4. 审批通过/驳回
 * 5. 下发指标
 * 6. 查询指标列表
 */

const axios = require('axios');
const IamTester = require('../iam/test-auth');

const BASE_URL = 'http://localhost:8080/api/v1';

class StrategyTester {
  constructor() {
    this.token = null;
    this.client = axios.create({
      baseURL: BASE_URL,
      timeout: 10000
    });
    this.createdIndicators = [];
  }

  /**
   * 登录获取Token
   */
  async login() {
    const iamTester = new IamTester();
    const result = await iamTester.testLogin('strategic');

    if (result.success) {
      this.token = result.token;
      console.log('✅ 已登录为战略部门用户');
      return true;
    }

    return false;
  }

  /**
   * 测试1: 创建草稿指标
   */
  async testCreateIndicator() {
    console.log('\n=== 测试1: 创建草稿指标 ===');

    if (!this.token) {
      console.log('❌ 未登录');
      return { success: false };
    }

    const indicatorData = {
      name: `测试指标_${Date.now()}`,
      code: `TEST_${Date.now()}`,
      description: '测试指标描述',
      type: 'QUANTITATIVE',
      weight: 5,
      targetValue: 100,
      unit: '个',
      period: '2026-03',
      status: 'DRAFT'
    };

    try {
      const response = await this.client.post('/indicators', indicatorData, {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        const indicator = response.data.data;
        this.createdIndicators.push(indicator);

        console.log('✅ 创建指标成功');
        console.log(`   ID: ${indicator.id}`);
        console.log(`   名称: ${indicator.name}`);
        console.log(`   编码: ${indicator.code}`);
        console.log(`   状态: ${indicator.status}`);

        return { success: true, indicator: indicator };
      }
    } catch (error) {
      console.log('❌ 创建指标失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试2: 更新指标
   */
  async testUpdateIndicator(indicatorId) {
    console.log('\n=== 测试2: 更新指标 ===');

    if (!indicatorId) {
      console.log('❌ 缺少指标ID');
      return { success: false };
    }

    const updates = {
      name: `更新后的指标_${Date.now()}`,
      targetValue: 95
    };

    try {
      const response = await this.client.put(`/indicators/${indicatorId}`, updates, {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        const indicator = response.data.data;
        console.log('✅ 更新指标成功');
        console.log(`   新名称: ${indicator.name}`);
        console.log(`   新目标值: ${indicator.targetValue}`);

        return { success: true, indicator: indicator };
      }
    } catch (error) {
      console.log('❌ 更新指标失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试3: 查询指标详情
   */
  async testGetIndicator(indicatorId) {
    console.log('\n=== 测试3: 查询指标详情 ===');

    if (!indicatorId) {
      console.log('❌ 缺少指标ID');
      return { success: false };
    }

    try {
      const response = await this.client.get(`/indicators/${indicatorId}`, {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        const indicator = response.data.data;
        console.log('✅ 查询指标成功');
        console.log(`   名称: ${indicator.name}`);
        console.log(`   编码: ${indicator.code}`);
        console.log(`   状态: ${indicator.status}`);
        console.log(`   目标值: ${indicator.targetValue}`);
        console.log(`   当前进度: ${indicator.progress || 0}%`);

        return { success: true, indicator: indicator };
      }
    } catch (error) {
      console.log('❌ 查询指标失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试4: 提交指标审批（下发）
   */
  async testDistributeIndicator(indicatorId) {
    console.log('\n=== 测试4: 提交指标审批 ===');

    if (!indicatorId) {
      console.log('❌ 缺少指标ID');
      return { success: false };
    }

    try {
      const response = await this.client.post(`/indicators/${indicatorId}/distribute`, {}, {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        console.log('✅ 提交审批成功');
        console.log('   状态: DRAFT → PENDING');

        // 验证状态
        const checkResponse = await this.client.get(`/indicators/${indicatorId}`, {
          headers: { Authorization: `Bearer ${this.token}` }
        });

        if (checkResponse.data.success) {
          console.log(`   当前状态: ${checkResponse.data.data.status}`);
        }

        return { success: true };
      }
    } catch (error) {
      console.log('❌ 提交审批失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试5: 查询指标列表
   */
  async testListIndicators(filters = {}) {
    console.log('\n=== 测试5: 查询指标列表 ===');

    try {
      const params = {
        page: 1,
        size: 20,
        ...filters
      };

      const response = await this.client.get('/indicators', {
        headers: { Authorization: `Bearer ${this.token}` },
        params: params
      });

      if (response.data.success) {
        const data = response.data.data;
        console.log('✅ 查询列表成功');
        console.log(`   总数: ${data.total}`);
        console.log(`   当前页: ${data.data.length} 个指标`);

        if (filters.status) {
          console.log(`   筛选条件: 状态=${filters.status}`);
        }

        // 显示前3个指标
        data.data.slice(0, 3).forEach(indicator => {
          console.log(`   - ${indicator.name} (${indicator.status})`);
        });

        return { success: true, indicators: data };
      }
    } catch (error) {
      console.log('❌ 查询列表失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试6: 按状态筛选指标
   */
  async testListIndicatorsByStatus() {
    console.log('\n=== 测试6: 按状态筛选指标 ===');

    const statuses = ['DRAFT', 'PENDING', 'DISTRIBUTED'];
    const results = {};

    for (const status of statuses) {
      const result = await this.testListIndicators({ status });
      results[status] = result;
    }

    return results;
  }

  /**
   * 测试7: 创建考核周期
   */
  async testCreateCycle() {
    console.log('\n=== 测试7: 创建考核周期 ===');

    const cycleData = {
      name: '2026年度考核周期',
      year: 2026,
      startDate: '2026-01-01',
      endDate: '2026-12-31',
      status: 'DRAFT'
    };

    try {
      const response = await this.client.post('/cycles', cycleData, {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        const cycle = response.data.data;
        console.log('✅ 创建周期成功');
        console.log(`   ID: ${cycle.id}`);
        console.log(`   名称: ${cycle.name}`);
        console.log(`   年份: ${cycle.year}`);
        console.log(`   状态: ${cycle.status}`);

        return { success: true, cycle: cycle };
      }
    } catch (error) {
      console.log('❌ 创建周期失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 运行完整测试流程
   */
  async runFullWorkflow() {
    console.log('\n╔══════════════════════════════════════════╗');
    console.log('║    SISM Strategy模块 - 指标管理测试     ║');
    console.log('╚══════════════════════════════════════════╝');

    // 1. 登录
    const loggedIn = await this.login();
    if (!loggedIn) {
      console.log('❌ 登录失败，无法继续测试');
      return;
    }

    // 2. 创建指标
    const createResult = await this.testCreateIndicator();
    if (!createResult.success) {
      return;
    }

    const indicatorId = createResult.indicator.id;

    // 3. 更新指标
    await this.testUpdateIndicator(indicatorId);

    // 4. 查询指标
    await this.testGetIndicator(indicatorId);

    // 5. 提交审批
    await this.testDistributeIndicator(indicatorId);

    // 6. 查询列表
    await this.testListIndicators();

    // 7. 按状态筛选
    await this.testListIndicatorsByStatus();

    // 8. 创建周期
    await this.testCreateCycle();

    console.log('\n✅ Strategy模块测试完成');
  }
}

module.exports = StrategyTester;

if (require.main === module) {
  const tester = new StrategyTester();
  tester.runFullWorkflow()
    .catch(error => console.error('测试异常:', error));
}
