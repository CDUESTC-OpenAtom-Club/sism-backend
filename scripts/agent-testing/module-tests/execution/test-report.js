/**
 * SISM Execution模块 - 填报管理API测试
 *
 * 测试流程：
 * 1. 创建填报（草稿）
 * 2. 更新填报
 * 3. 提交审批
 * 4. 审批通过/驳回
 * 5. 查询填报列表
 * 6. 按周期查询
 */

const axios = require('axios');
const IamTester = require('../iam/test-auth');

const BASE_URL = 'http://localhost:8080/api/v1';

class ExecutionTester {
  constructor() {
    this.token = null;
    this.client = axios.create({
      baseURL: BASE_URL,
      timeout: 10000
    });
    this.createdReports = [];
  }

  /**
   * 登录获取Token（学院用户）
   */
  async login() {
    const iamTester = new IamTester();
    const result = await iamTester.testLogin('college');

    if (result.success) {
      this.token = result.token;
      console.log('✅ 已登录为学院用户');
      return true;
    }

    return false;
  }

  /**
   * 测试1: 创建填报草稿
   */
  async testCreateReport(indicatorId) {
    console.log('\n=== 测试1: 创建填报草稿 ===');

    if (!this.token) {
      console.log('❌ 未登录');
      return { success: false };
    }

    const reportData = {
      indicatorId: indicatorId || 1, // 假设有指标ID为1
      period: '2026-03',
      actualValue: 85,
      completionRate: 85,
      status: 'DRAFT',
      summary: '三月份工作总结',
      problems: '存在的问题',
      measures: '改进措施',
      nextPlan: '下步计划'
    };

    try {
      const response = await this.client.post('/reports', reportData, {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        const report = response.data.data;
        this.createdReports.push(report);

        console.log('✅ 创建填报成功');
        console.log(`   ID: ${report.id}`);
        console.log(`   指标ID: ${report.indicatorId}`);
        console.log(`   周期: ${report.period}`);
        console.log(`   完成率: ${report.completionRate}%`);
        console.log(`   状态: ${report.status}`);

        return { success: true, report: report };
      }
    } catch (error) {
      console.log('❌ 创建填报失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试2: 更新填报
   */
  async testUpdateReport(reportId) {
    console.log('\n=== 测试2: 更新填报 ===');

    if (!reportId) {
      console.log('❌ 缺少填报ID');
      return { success: false };
    }

    const updates = {
      actualValue: 90,
      completionRate: 90,
      summary: '更新后的总结'
    };

    try {
      const response = await this.client.put(`/reports/${reportId}`, updates, {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        const report = response.data.data;
        console.log('✅ 更新填报成功');
        console.log(`   新完成率: ${report.completionRate}%`);
        console.log(`   状态: ${report.status} (仍为草稿)`);

        return { success: true, report: report };
      }
    } catch (error) {
      console.log('❌ 更新填报失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试3: 提交填报审批
   */
  async testSubmitReport(reportId) {
    console.log('\n=== 测试3: 提交填报审批 ===');

    if (!reportId) {
      console.log('❌ 缺少填报ID');
      return { success: false };
    }

    try {
      const response = await this.client.post(`/reports/${reportId}/submit`, {}, {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        console.log('✅ 提交审批成功');
        console.log('   状态: DRAFT → PENDING');

        // 验证状态
        const checkResponse = await this.client.get(`/reports/${reportId}`, {
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
   * 测试4: 撤回填报
   */
  async testWithdrawReport(reportId) {
    console.log('\n=== 测试4: 撤回填报 ===');

    if (!reportId) {
      console.log('❌ 缺少填报ID');
      return { success: false };
    }

    try {
      const response = await this.client.post(`/reports/${reportId}/withdraw`, {}, {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        console.log('✅ 撤回填报成功');
        console.log('   状态: PENDING → DRAFT');

        return { success: true };
      }
    } catch (error) {
      console.log('❌ 撤回填报失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试5: 查询填报列表
   */
  async testListReports(filters = {}) {
    console.log('\n=== 测试5: 查询填报列表 ===');

    try {
      const params = {
        page: 1,
        size: 20,
        ...filters
      };

      const response = await this.client.get('/reports', {
        headers: { Authorization: `Bearer ${this.token}` },
        params: params
      });

      if (response.data.success) {
        const data = response.data.data;
        console.log('✅ 查询列表成功');
        console.log(`   总数: ${data.total}`);
        console.log(`   当前页: ${data.data.length} 个填报`);

        // 显示前3个填报
        data.data.slice(0, 3).forEach(report => {
          console.log(`   - 周期${report.period} 完成率${report.completionRate}% (${report.status})`);
        });

        return { success: true, reports: data };
      }
    } catch (error) {
      console.log('❌ 查询列表失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试6: 查询指标的历史填报
   */
  async testGetReportsByIndicator(indicatorId) {
    console.log('\n=== 测试6: 查询指标历史填报 ===');

    if (!indicatorId) {
      console.log('❌ 缺少指标ID');
      return { success: false };
    }

    try {
      const response = await this.client.get(`/indicators/${indicatorId}/reports`, {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        const reports = response.data.data;
        console.log('✅ 查询历史填报成功');
        console.log(`   数量: ${reports.length}`);

        reports.forEach(report => {
          console.log(`   - ${report.period}: ${report.actualValue} (${report.status})`);
        });

        return { success: true, reports: reports };
      }
    } catch (error) {
      console.log('❌ 查询历史失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试7: 按周期查询填报
   */
  async testGetReportsByPeriod(period) {
    console.log('\n=== 测试7: 按周期查询填报 ===');

    try {
      const response = await this.client.get('/reports', {
        headers: { Authorization: `Bearer ${this.token}` },
        params: { period: period || '2026-03' }
      });

      if (response.data.success) {
        const reports = response.data.data.data;
        console.log('✅ 按周期查询成功');
        console.log(`   周期: ${period}`);
        console.log(`   数量: ${reports.length}`);

        return { success: true, reports: reports };
      }
    } catch (error) {
      console.log('❌ 按周期查询失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 运行完整测试流程
   */
  async runFullWorkflow() {
    console.log('\n╔══════════════════════════════════════════╗');
    console.log('║   SISM Execution模块 - 填报管理测试    ║');
    console.log('╚══════════════════════════════════════════╝');

    // 1. 登录
    const loggedIn = await this.login();
    if (!loggedIn) {
      console.log('❌ 登录失败，无法继续测试');
      return;
    }

    // 2. 创建填报
    const createResult = await this.testCreateReport();
    if (!createResult.success) {
      return;
    }

    const reportId = createResult.report.id;

    // 3. 更新填报
    await this.testUpdateReport(reportId);

    // 4. 提交审批
    await this.testSubmitReport(reportId);

    // 5. 撤回（测试撤回功能）
    await this.testWithdrawReport(reportId);

    // 6. 查询列表
    await this.testListReports();

    // 7. 按周期查询
    await this.testGetReportsByPeriod('2026-03');

    console.log('\n✅ Execution模块测试完成');
  }
}

module.exports = ExecutionTester;

if (require.main === module) {
  const tester = new ExecutionTester();
  tester.runFullWorkflow()
    .catch(error => console.error('测试异常:', error));
}
