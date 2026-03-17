/**
 * SISM Organization模块 - 组织管理API测试
 *
 * 测试流程：
 * 1. 查询组织树
 * 2. 查询组织详情
 * 3. 查询组织成员
 * 4. 创建子组织
 */

const axios = require('axios');
const IamTester = require('../iam/test-auth');

const BASE_URL = 'http://localhost:8080/api/v1';

class OrganizationTester {
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
   * 测试1: 查询组织树
   */
  async testGetOrganizationTree() {
    console.log('\n=== 测试1: 查询组织树 ===');

    if (!this.token) {
      console.log('❌ 未登录');
      return { success: false };
    }

    try {
      const response = await this.client.get('/organizations/tree', {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        const tree = response.data.data;
        console.log('✅ 查询组织树成功');
        this.printOrgTree(tree, 0);

        return { success: true, tree: tree };
      }
    } catch (error) {
      console.log('❌ 查询组织树失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 打印组织树
   */
  printOrgTree(org, level) {
    const indent = '  '.repeat(level);
    console.log(`${indent}├─ ${org.name} (${org.code})`);
    console.log(`${indent}   类型: ${org.type}`);
    console.log(`${indent}   状态: ${org.status}`);

    if (org.children && org.children.length > 0) {
      org.children.forEach(child => {
        this.printOrgTree(child, level + 1);
      });
    }
  }

  /**
   * 测试2: 查询组织详情
   */
  async testGetOrganization(orgId) {
    console.log('\n=== 测试2: 查询组织详情 ===');

    if (!orgId) {
      console.log('❌ 缺少组织ID');
      return { success: false };
    }

    try {
      const response = await this.client.get(`/organizations/${orgId}`, {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        const org = response.data.data;
        console.log('✅ 查询组织详情成功');
        console.log(`   ID: ${org.id}`);
        console.log(`   名称: ${org.name}`);
        console.log(`   编码: ${org.code}`);
        console.log(`   类型: ${org.type}`);
        console.log(`   父组织ID: ${org.parentId || '无'}`);
        console.log(`   状态: ${org.status}`);
        console.log(`   描述: ${org.description || '无'}`);

        return { success: true, org: org };
      }
    } catch (error) {
      console.log('❌ 查询组织详情失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试3: 查询组织成员
   */
  async testGetOrganizationUsers(orgId) {
    console.log('\n=== 测试3: 查询组织成员 ===');

    if (!orgId) {
      console.log('❌ 缺少组织ID');
      return { success: false };
    }

    try {
      const response = await this.client.get(`/organizations/${orgId}/users`, {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        const users = response.data.data;
        console.log('✅ 查询组织成员成功');
        console.log(`   成员数量: ${users.length}`);

        users.slice(0, 5).forEach(user => {
          console.log(`   - ${user.realName} (${user.username})`);
        });

        return { success: true, users: users };
      }
    } catch (error) {
      console.log('❌ 查询组织成员失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试4: 查询子组织
   */
  async testGetChildOrganizations(parentId) {
    console.log('\n=== 测试4: 查询子组织 ===');

    if (!parentId) {
      console.log('❌ 缺少父组织ID');
      return { success: false };
    }

    try {
      const response = await this.client.get('/organizations', {
        headers: { Authorization: `Bearer ${this.token}` },
        params: { parentId: parentId }
      });

      if (response.data.success) {
        const children = response.data.data;
        console.log('✅ 查询子组织成功');
        console.log(`   子组织数量: ${children.length}`);

        children.forEach(child => {
          console.log(`   - ${child.name} (${child.type})`);
        });

        return { success: true, children: children };
      }
    } catch (error) {
      console.log('❌ 查询子组织失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 运行完整测试流程
   */
  async runFullWorkflow() {
    console.log('\n╔══════════════════════════════════════════╗');
    console.log('║   SISM Organization模块 - 组织管理测试 ║');
    console.log('╚══════════════════════════════════════════╝');

    // 1. 登录
    const loggedIn = await this.login();
    if (!loggedIn) {
      console.log('❌ 登录失败，无法继续测试');
      return;
    }

    // 2. 查询组织树
    const treeResult = await this.testGetOrganizationTree();

    // 3. 如果有组织，测试详情和成员查询
    if (treeResult.success && treeResult.tree) {
      const orgId = treeResult.tree.id;

      // 查询组织详情
      await this.testGetOrganization(orgId);

      // 查询组织成员
      await this.testGetOrganizationUsers(orgId);

      // 查询子组织
      await this.testGetChildOrganizations(orgId);
    }

    console.log('\n✅ Organization模块测试完成');
  }
}

module.exports = OrganizationTester;

if (require.main === module) {
  const tester = new OrganizationTester();
  tester.runFullWorkflow()
    .catch(error => console.error('测试异常:', error));
}
