/**
 * SISM IAM模块 - 认证授权API测试
 *
 * 测试流程：
 * 1. 用户登录
 * 2. Token刷新
 * 3. 获取用户信息
 * 4. 获取用户列表
 * 5. 用户登出
 */

const axios = require('axios');

const BASE_URL = 'http://localhost:8080/api/v1';

// 测试用户配置
const TEST_USERS = {
  strategic: {
    username: 'admin',
    password: 'admin123',
    description: '战略发展部 - 系统管理员'
  },
  functional: {
    username: 'func_user',
    password: 'func123',
    description: '职能部门 - 中层管理'
  },
  college: {
    username: 'college_user',
    password: 'college123',
    description: '学院 - 执行层'
  }
};

class IamTester {
  constructor() {
    this.token = null;
    this.refreshToken = null;
    this.client = axios.create({
      baseURL: BASE_URL,
      timeout: 10000
    });
  }

  /**
   * 测试1: 用户登录
   */
  async testLogin(userType = 'strategic') {
    console.log('\n=== 测试1: 用户登录 ===');
    const user = TEST_USERS[userType];

    try {
      const response = await this.client.post('/auth/login', {
        username: user.username,
        password: user.password
      });

      if (response.data.success) {
        this.token = response.data.data.token;
        this.refreshToken = response.data.data.refreshToken;

        console.log('✅ 登录成功');
        console.log(`   用户: ${user.username}`);
        console.log(`   角色: ${user.description}`);
        console.log(`   Token: ${this.token.substring(0, 20)}...`);

        return {
          success: true,
          token: this.token,
          refreshToken: this.refreshToken,
          user: response.data.data
        };
      } else {
        console.log('❌ 登录失败:', response.data.message);
        return { success: false, message: response.data.message };
      }
    } catch (error) {
      console.log('❌ 登录异常:', error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试2: 获取当前用户信息
   */
  async testGetUserProfile() {
    console.log('\n=== 测试2: 获取用户信息 ===');

    if (!this.token) {
      console.log('❌ 未登录，请先调用testLogin');
      return { success: false };
    }

    try {
      const response = await this.client.get('/users/profile', {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        const userProfile = response.data.data;
        console.log('✅ 获取用户信息成功');
        console.log(`   用户名: ${userProfile.username}`);
        console.log(`   真实姓名: ${userProfile.realName}`);
        console.log(`   组织ID: ${userProfile.orgId}`);
        console.log(`   状态: ${userProfile.isActive ? '激活' : '未激活'}`);

        return { success: true, profile: userProfile };
      }
    } catch (error) {
      console.log('❌ 获取用户信息失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试3: 获取用户列表
   */
  async testGetUserList() {
    console.log('\n=== 测试3: 获取用户列表 ===');

    if (!this.token) {
      console.log('❌ 未登录，请先调用testLogin');
      return { success: false };
    }

    try {
      const response = await this.client.get('/users', {
        headers: { Authorization: `Bearer ${this.token}` },
        params: { page: 1, size: 20 }
      });

      if (response.data.success) {
        const users = response.data.data;
        console.log('✅ 获取用户列表成功');
        console.log(`   总数: ${users.total}`);
        console.log(`   当前页: ${users.data.length} 个用户`);

        // 按角色统计
        const roleStats = {};
        users.data.forEach(user => {
          // 假设有role字段
          const role = user.role || 'unknown';
          roleStats[role] = (roleStats[role] || 0) + 1;
        });

        console.log('   角色分布:', roleStats);

        return { success: true, users: users };
      }
    } catch (error) {
      console.log('❌ 获取用户列表失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试4: Token刷新
   */
  async testRefreshToken() {
    console.log('\n=== 测试4: Token刷新 ===');

    if (!this.refreshToken) {
      console.log('❌ 无refreshToken');
      return { success: false };
    }

    try {
      const response = await this.client.post('/auth/refresh', {
        refreshToken: this.refreshToken
      });

      if (response.data.success) {
        this.token = response.data.data.token;
        this.refreshToken = response.data.data.refreshToken;

        console.log('✅ Token刷新成功');
        console.log(`   新Token: ${this.token.substring(0, 20)}...`);

        return { success: true, token: this.token };
      }
    } catch (error) {
      console.log('❌ Token刷新失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试5: 获取角色列表
   */
  async testGetRoles() {
    console.log('\n=== 测试5: 获取角色列表 ===');

    if (!this.token) {
      console.log('❌ 未登录');
      return { success: false };
    }

    try {
      const response = await this.client.get('/roles', {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        const roles = response.data.data;
        console.log('✅ 获取角色列表成功');
        console.log(`   角色数量: ${roles.length}`);

        roles.forEach(role => {
          console.log(`   - ${role.roleName}: ${role.description || '无描述'}`);
        });

        return { success: true, roles: roles };
      }
    } catch (error) {
      console.log('❌ 获取角色列表失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试6: 用户登出
   */
  async testLogout() {
    console.log('\n=== 测试6: 用户登出 ===');

    if (!this.token) {
      console.log('❌ 未登录');
      return { success: false };
    }

    try {
      const response = await this.client.post('/auth/logout', {}, {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        console.log('✅ 登出成功');
        this.token = null;
        this.refreshToken = null;
        return { success: true };
      }
    } catch (error) {
      console.log('❌ 登出失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 运行所有测试
   */
  async runAllTests() {
    console.log('\n╔══════════════════════════════════════════╗');
    console.log('║      SISM IAM模块 - 认证授权测试       ║');
    console.log('╚══════════════════════════════════════════╝');

    const results = {
      login: await this.testLogin('strategic'),
      profile: await this.testGetUserProfile(),
      users: await this.testGetUserList(),
      roles: await this.testGetRoles(),
      refresh: await this.testRefreshToken(),
      logout: await this.testLogout()
    };

    console.log('\n╔══════════════════════════════════════════╗');
    console.log('║            测试结果汇总                ║');
    console.log('╚══════════════════════════════════════════╝');

    Object.entries(results).forEach(([test, result]) => {
      const status = result.success ? '✅ 通过' : '❌ 失败';
      console.log(`${test.padEnd(10)}: ${status}`);
    });

    return results;
  }
}

// 导出测试类
module.exports = IamTester;

// 如果直接运行此文件
if (require.main === module) {
  const tester = new IamTester();
  tester.runAllTests()
    .then(() => console.log('\n测试完成'))
    .catch(error => console.error('测试异常:', error));
}
