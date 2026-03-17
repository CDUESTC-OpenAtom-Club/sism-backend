/**
 * SISM Alert模块 - 告警管理API测试
 *
 * 测试流程：
 * 1. 创建告警规则
 * 2. 触发告警
 * 3. 查询告警列表
 * 4. 标记告警为已读
 * 5. 关闭告警
 */

const axios = require('axios');
const IamTester = require('../iam/test-auth');

const BASE_URL = 'http://localhost:8080/api/v1';

class AlertTester {
  constructor() {
    this.token = null;
    this.client = axios.create({
      baseURL: BASE_URL,
      timeout: 10000
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
   * 测试1: 查询告警列表
   */
  async testListAlerts(filters = {}) {
    console.log('\n=== 测试1: 查询告警列表 ===');

    if (!this.token) {
      console.log('❌ 未登录');
      return { success: false };
    }

    try {
      const params = {
        page: 1,
        size: 20,
        ...filters
      };

      const response = await this.client.get('/alerts', {
        headers: { Authorization: `Bearer ${this.token}` },
        params: params
      });

      if (response.data.success) {
        const data = response.data.data;
        console.log('✅ 查询告警列表成功');
        console.log(`   总数: ${data.total}`);
        console.log(`   当前页: ${data.data.length} 个告警`);

        // 显示前5个告警
        data.data.slice(0, 5).forEach(alert => {
          console.log(`   - ${alert.title} (${alert.level})`);
          console.log(`     类型: ${alert.type} | 状态: ${alert.status}`);
          console.log(`     时间: ${alert.createdAt}`);
        });

        return { success: true, alerts: data };
      }
    } catch (error) {
      console.log('❌ 查询告警列表失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试2: 查询我的告警
   */
  async testGetMyAlerts() {
    console.log('\n=== 测试2: 查询我的告警 ===');

    if (!this.token) {
      console.log('❌ 未登录');
      return { success: false };
    }

    try {
      const response = await this.client.get('/alerts/my', {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        const alerts = response.data.data;
        console.log('✅ 查询我的告警成功');
        console.log(`   告警数量: ${alerts.length}`);

        // 按级别统计
        const levelStats = {};
        alerts.forEach(alert => {
          levelStats[alert.level] = (levelStats[alert.level] || 0) + 1;
        });

        console.log('   告警级别分布:', levelStats);

        // 显示前3个告警
        alerts.slice(0, 3).forEach(alert => {
          console.log(`   - ${alert.title} (${alert.level})`);
        });

        return { success: true, alerts: alerts };
      }
    } catch (error) {
      console.log('❌ 查询我的告警失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试3: 查询告警详情
   */
  async testGetAlert(alertId) {
    console.log('\n=== 测试3: 查询告警详情 ===');

    if (!alertId) {
      console.log('❌ 缺少告警ID');
      return { success: false };
    }

    try {
      const response = await this.client.get(`/alerts/${alertId}`, {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        const alert = response.data.data;
        console.log('✅ 查询告警详情成功');
        console.log(`   ID: ${alert.id}`);
        console.log(`   标题: ${alert.title}`);
        console.log(`   类型: ${alert.type}`);
        console.log(`   级别: ${alert.level}`);
        console.log(`   状态: ${alert.status}`);
        console.log(`   消息: ${alert.message}`);
        console.log(`   创建时间: ${alert.createdAt}`);

        return { success: true, alert: alert };
      }
    } catch (error) {
      console.log('❌ 查询告警详情失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试4: 标记告警为已读
   */
  async testMarkAlertAsRead(alertId) {
    console.log('\n=== 测试4: 标记告警为已读 ===');

    if (!alertId) {
      console.log('❌ 缺少告警ID');
      return { success: false };
    }

    try {
      const response = await this.client.post(`/alerts/${alertId}/read`, {}, {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        const alert = response.data.data;
        console.log('✅ 标记告警为已读成功');
        console.log(`   状态: ${alert.status}`);

        return { success: true, alert: alert };
      }
    } catch (error) {
      console.log('❌ 标记告警为已读失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试5: 批量标记告警为已读
   */
  async testMarkMultipleAlertsAsRead(alertIds) {
    console.log('\n=== 测试5: 批量标记告警为已读 ===');

    if (!alertIds || alertIds.length === 0) {
      console.log('❌ 缺少告警ID列表');
      return { success: false };
    }

    try {
      const response = await this.client.post('/alerts/batch/read', {
        alertIds: alertIds
      }, {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        console.log('✅ 批量标记告警为已读成功');
        console.log(`   标记数量: ${alertIds.length}`);

        return { success: true };
      }
    } catch (error) {
      console.log('❌ 批量标记告警为已读失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试6: 关闭告警
   */
  async testCloseAlert(alertId, note) {
    console.log('\n=== 测试6: 关闭告警 ===');

    if (!alertId) {
      console.log('❌ 缺少告警ID');
      return { success: false };
    }

    try {
      const response = await this.client.post(`/alerts/${alertId}/close`, {
        note: note || '告警已处理'
      }, {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        const alert = response.data.data;
        console.log('✅ 关闭告警成功');
        console.log(`   状态: ${alert.status}`);
        console.log(`   处理备注: ${note || '告警已处理'}`);

        return { success: true, alert: alert };
      }
    } catch (error) {
      console.log('❌ 关闭告警失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试7: 按级别查询告警
   */
  async testGetAlertsByLevel(level) {
    console.log('\n=== 测试7: 按级别查询告警 ===');

    const levels = ['INFO', 'WARNING', 'ERROR', 'CRITICAL'];
    const testLevel = level || levels[1];

    try {
      const response = await this.client.get('/alerts', {
        headers: { Authorization: `Bearer ${this.token}` },
        params: { level: testLevel }
      });

      if (response.data.success) {
        const alerts = response.data.data.data;
        console.log('✅ 按级别查询告警成功');
        console.log(`   级别: ${testLevel}`);
        console.log(`   数量: ${alerts.length}`);

        alerts.slice(0, 3).forEach(alert => {
          console.log(`   - ${alert.title}`);
        });

        return { success: true, alerts: alerts };
      }
    } catch (error) {
      console.log('❌ 按级别查询告警失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试8: 查询未读告警数量
   */
  async testGetUnreadCount() {
    console.log('\n=== 测试8: 查询未读告警数量 ===');

    if (!this.token) {
      console.log('❌ 未登录');
      return { success: false };
    }

    try {
      const response = await this.client.get('/alerts/unread/count', {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        const count = response.data.data;
        console.log('✅ 查询未读告警数量成功');
        console.log(`   未读数量: ${count}`);

        return { success: true, count: count };
      }
    } catch (error) {
      console.log('❌ 查询未读告警数量失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 运行完整测试流程
   */
  async runFullWorkflow() {
    console.log('\n╔══════════════════════════════════════════╗');
    console.log('║      SISM Alert模块 - 告警管理测试      ║');
    console.log('╚══════════════════════════════════════════╝');

    // 1. 登录
    const loggedIn = await this.login();
    if (!loggedIn) {
      console.log('❌ 登录失败，无法继续测试');
      return;
    }

    // 2. 查询告警列表
    const listResult = await this.testListAlerts();

    // 3. 查询我的告警
    await this.testGetMyAlerts();

    // 4. 查询未读数量
    await this.testGetUnreadCount();

    // 5. 如果有告警，测试详情和操作
    if (listResult.success && listResult.alerts.data.length > 0) {
      const alertId = listResult.alerts.data[0].id;

      // 查询详情
      await this.testGetAlert(alertId);

      // 标记为已读
      await this.testMarkAlertAsRead(alertId);

      // 按级别查询
      await this.testGetAlertsByLevel('WARNING');

      // 关闭告警（使用另一个告警ID，因为已经标记为已读）
      if (listResult.alerts.data.length > 1) {
        const alertId2 = listResult.alerts.data[1].id;
        await this.testCloseAlert(alertId2, '测试关闭告警');
      }
    } else {
      console.log('⚠️  没有找到告警，跳过部分测试');
    }

    // 6. 测试批量标记
    if (listResult.success && listResult.alerts.data.length > 0) {
      const alertIds = listResult.alerts.data.slice(0, 3).map(a => a.id);
      await this.testMarkMultipleAlertsAsRead(alertIds);
    }

    console.log('\n✅ Alert模块测试完成');
  }
}

module.exports = AlertTester;

if (require.main === module) {
  const tester = new AlertTester();
  tester.runFullWorkflow()
    .catch(error => console.error('测试异常:', error));
}
