/**
 * SISM Analytics模块 - 数据分析API测试
 *
 * 测试流程：
 * 1. 获取仪表盘数据
 * 2. 查询指标进度
 * 3. 查询组织进度
 * 4. 导出数据
 */

const axios = require('axios');
const IamTester = require('../iam/test-auth');

const BASE_URL = 'http://localhost:8080/api/v1';

class AnalyticsTester {
  constructor() {
    this.token = null;
    this.client = axios.create({
      baseURL: BASE_URL,
      timeout: 15000 // 分析接口可能较慢
    });
  }

  /**
   * 登录获取Token
   */
  async login() {
    const iamTester = new IamTester();
    const result = await iamTester.testLogin('strategic');

    if (result.success) {
      this.token = result.token;
      console.log('✅ 已登录');
      return true;
    }

    return false;
  }

  /**
   * 测试1: 获取仪表盘数据
   */
  async testGetDashboard(filters = {}) {
    console.log('\n=== 测试1: 获取仪表盘数据 ===');

    if (!this.token) {
      console.log('❌ 未登录');
      return { success: false };
    }

    try {
      const params = {
        period: '2026-03',
        ...filters
      };

      const response = await this.client.get('/analytics/dashboard', {
        headers: { Authorization: `Bearer ${this.token}` },
        params: params
      });

      if (response.data.success) {
        const dashboard = response.data.data;
        console.log('✅ 获取仪表盘数据成功');
        console.log(`   周期: ${params.period}`);

        if (dashboard.totalIndicators !== undefined) {
          console.log(`   总指标数: ${dashboard.totalIndicators}`);
        }
        if (dashboard.completedIndicators !== undefined) {
          console.log(`   已完成: ${dashboard.completedIndicators}`);
        }
        if (dashboard.averageProgress !== undefined) {
          console.log(`   平均进度: ${dashboard.averageProgress}%`);
        }

        // 显示组织进度分布
        if (dashboard.orgProgress && dashboard.orgProgress.length > 0) {
          console.log('   组织进度:');
          dashboard.orgProgress.slice(0, 5).forEach(org => {
            console.log(`     - ${org.orgName}: ${org.progress}%`);
          });
        }

        return { success: true, dashboard: dashboard };
      }
    } catch (error) {
      console.log('❌ 获取仪表盘数据失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试2: 查询指标进度
   */
  async testGetIndicatorProgress(indicatorId) {
    console.log('\n=== 测试2: 查询指标进度 ===');

    if (!indicatorId) {
      console.log('❌ 缺少指标ID');
      return { success: false };
    }

    try {
      const response = await this.client.get(`/analytics/indicators/${indicatorId}/progress`, {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        const progress = response.data.data;
        console.log('✅ 查询指标进度成功');
        console.log(`   指标ID: ${indicatorId}`);
        console.log(`   当前进度: ${progress.currentProgress}%`);
        console.log(`   目标值: ${progress.targetValue}`);
        console.log(`   完成率: ${progress.completionRate}%`);

        if (progress.history && progress.history.length > 0) {
          console.log('   进度历史:');
          progress.history.slice(0, 5).forEach(h => {
            console.log(`     - ${h.period}: ${h.progress}%`);
          });
        }

        return { success: true, progress: progress };
      }
    } catch (error) {
      console.log('❌ 查询指标进度失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试3: 查询组织进度
   */
  async testGetOrgProgress(orgId) {
    console.log('\n=== 测试3: 查询组织进度 ===');

    if (!orgId) {
      console.log('❌ 缺少组织ID');
      return { success: false };
    }

    try {
      const response = await this.client.get(`/analytics/organizations/${orgId}/progress`, {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        const progress = response.data.data;
        console.log('✅ 查询组织进度成功');
        console.log(`   组织ID: ${orgId}`);
        console.log(`   平均进度: ${progress.averageProgress}%`);
        console.log(`   指标总数: ${progress.totalIndicators}`);
        console.log(`   已完成: ${progress.completedIndicators}`);

        if (progress.indicatorBreakdown) {
          console.log('   指标分布:');
          Object.entries(progress.indicatorBreakdown).forEach(([status, count]) => {
            console.log(`     - ${status}: ${count}`);
          });
        }

        return { success: true, progress: progress };
      }
    } catch (error) {
      console.log('❌ 查询组织进度失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试4: 导出数据
   */
  async testExportData(exportParams) {
    console.log('\n=== 测试4: 导出数据 ===');

    if (!this.token) {
      console.log('❌ 未登录');
      return { success: false };
    }

    try {
      const params = {
        type: 'indicator', // indicator, organization, report
        period: '2026-03',
        format: 'excel',
        ...exportParams
      };

      const response = await this.client.get('/analytics/export', {
        headers: { Authorization: `Bearer ${this.token}` },
        params: params,
        responseType: 'arraybuffer'
      });

      if (response.status === 200) {
        console.log('✅ 导出数据成功');
        console.log(`   类型: ${params.type}`);
        console.log(`   格式: ${params.format}`);
        console.log(`   数据大小: ${response.data.length} bytes`);

        return { success: true, data: response.data };
      }
    } catch (error) {
      console.log('❌ 导出数据失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试5: 获取进度统计
   */
  async testGetProgressStats(filters = {}) {
    console.log('\n=== 测试5: 获取进度统计 ===');

    if (!this.token) {
      console.log('❌ 未登录');
      return { success: false };
    }

    try {
      const params = {
        period: '2026-03',
        ...filters
      };

      const response = await this.client.get('/analytics/stats', {
        headers: { Authorization: `Bearer ${this.token}` },
        params: params
      });

      if (response.data.success) {
        const stats = response.data.data;
        console.log('✅ 获取进度统计成功');

        if (stats.byStatus) {
          console.log('   按状态统计:');
          Object.entries(stats.byStatus).forEach(([status, count]) => {
            console.log(`     - ${status}: ${count}`);
          });
        }

        if (stats.byType) {
          console.log('   按类型统计:');
          Object.entries(stats.byType).forEach(([type, count]) => {
            console.log(`     - ${type}: ${count}`);
          });
        }

        if (stats.byProgressRange) {
          console.log('   按进度区间:');
          Object.entries(stats.byProgressRange).forEach(([range, count]) => {
            console.log(`     - ${range}: ${count}`);
          });
        }

        return { success: true, stats: stats };
      }
    } catch (error) {
      console.log('❌ 获取进度统计失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 运行完整测试流程
   */
  async runFullWorkflow() {
    console.log('\n╔══════════════════════════════════════════╗');
    console.log('║    SISM Analytics模块 - 数据分析测试   ║');
    console.log('╚══════════════════════════════════════════╝');

    // 1. 登录
    const loggedIn = await this.login();
    if (!loggedIn) {
      console.log('❌ 登录失败，无法继续测试');
      return;
    }

    // 2. 获取仪表盘数据
    await this.testGetDashboard();

    // 3. 查询指标进度（假设有ID为1的指标）
    await this.testGetIndicatorProgress(1);

    // 4. 查询组织进度（假设有ID为1的组织）
    await this.testGetOrgProgress(1);

    // 5. 获取进度统计
    await this.testGetProgressStats();

    // 6. 导出数据
    await this.testExportData({ type: 'indicator' });

    console.log('\n✅ Analytics模块测试完成');
  }
}

module.exports = AnalyticsTester;

if (require.main === module) {
  const tester = new AnalyticsTester();
  tester.runFullWorkflow()
    .catch(error => console.error('测试异常:', error));
}
