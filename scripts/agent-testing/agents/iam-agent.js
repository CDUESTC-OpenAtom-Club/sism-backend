const BaseAgent = require('./base-agent');

/**
 * IAM认证Agent
 * 负责测试认证、授权、用户管理等功能
 */
class IamAgent extends BaseAgent {
  constructor(config) {
    super('IAM认证Agent', config);
  }

  /**
   * 执行所有测试
   */
  async execute() {
    await this.start();

    try {
      // 测试1: 用户登录
      await this.runScenario('用户登录测试', async (scenario) => {
        await this.runStep('使用正确凭证登录', async () => {
          const user = this.config.users[0];
          const result = await this.login(user.username, user.password);

          this.assertions.assertFieldValue(result, 'token', result.token);
          this.assertions.assertFieldValue(result, 'refreshToken', result.refreshToken);

          this.storeData('auth_token', result.token);
          this.storeData('refresh_token', result.refreshToken);
        }, scenario);

        await this.runStep('验证Token有效性', async () => {
          const profile = await this.apiClient.get(this.apiClient.endpoints.iam.getUserProfile);
          this.assertions.assertStatusCode(profile.code, 200);
          this.assertions.assertFieldExists(profile.data, 'username');
        }, scenario);
      });

      // 测试2: 错误凭证登录
      await this.runScenario('错误凭证登录测试', async (scenario) => {
        await this.runStep('使用错误密码登录', async () => {
          try {
            await this.apiClient.login('admin', 'wrong_password');
            throw new Error('应该抛出错误');
          } catch (error) {
            if (error.httpStatus === 401) {
              console.log('      ✓ 正确返回401未授权');
            } else {
              throw error;
            }
          }
        }, scenario);
      });

      // 测试3: Token刷新
      await this.runScenario('Token刷新测试', async (scenario) => {
        await this.runStep('刷新访问令牌', async () => {
          const newToken = await this.apiClient.refreshAccessToken();
          this.assertions.assertFieldExists(newToken, 'token');
          this.assertions.assertFieldExists(newToken, 'refreshToken');

          // 新Token应该与旧Token不同
          const oldToken = this.getStoredData('auth_token');
          if (newToken.token === oldToken) {
            throw new Error('新Token应该与旧Token不同');
          }

          this.storeData('auth_token', newToken.token);
          this.storeData('refresh_token', newToken.refreshToken);
        }, scenario);
      });

      // 测试4: 获取用户列表
      await this.runScenario('获取用户列表测试', async (scenario) => {
        await this.runStep('获取所有用户', async () => {
          const users = await this.apiClient.get(this.apiClient.endpoints.iam.getUsers);
          this.assertions.assertStatusCode(users.code, 200);
          this.assertions.assertDataExists(users);
          this.assertions.assertions = Array.isArray(users.data);

          // 存储用户信息供其他Agent使用
          if (users.data && users.data.length > 0) {
            this.storeData('all_users', users.data);

            // 按角色分类用户
            const usersByRole = {
              strategic: [],
              functional: [],
              college: []
            };

            for (const user of users.data) {
              // 根据用户名或其他属性判断角色
              if (user.username === 'admin') {
                usersByRole.strategic.push(user);
              } else if (user.username.startsWith('func')) {
                usersByRole.functional.push(user);
              } else if (user.username.startsWith('college')) {
                usersByRole.college.push(user);
              }
            }

            this.storeData('users_by_role', usersByRole);
          }
        }, scenario);
      });

      // 测试5: 用户登出
      await this.runScenario('用户登出测试', async (scenario) => {
        await this.runStep('登出当前用户', async () => {
          await this.logout();
          // Token应该被清除
          if (this.apiClient.token) {
            throw new Error('登出后Token应该被清除');
          }
        }, scenario);
      });

    } finally {
      await this.end();
    }

    return this.getResults();
  }
}

module.exports = IamAgent;
