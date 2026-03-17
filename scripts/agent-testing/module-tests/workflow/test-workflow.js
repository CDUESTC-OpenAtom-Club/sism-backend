/**
 * SISM Workflow模块 - 工作流API测试
 *
 * 测试流程：
 * 1. 查询工作流定义
 * 2. 创建工作流实例
 * 3. 执行审批步骤
 * 4. 审批通过/驳回
 * 5. 查询审批时间轴
 * 6. 查询审批历史
 */

const axios = require('axios');
const IamTester = require('../iam/test-auth');

const BASE_URL = 'http://localhost:8080/api/v1';

class WorkflowTester {
  constructor() {
    this.token = null;
    this.client = axios.create({
      baseURL: BASE_URL,
      timeout: 10000
    });
    this.createdInstances = [];
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
   * 测试1: 查询工作流定义
   */
  async testGetWorkflowDefinitions() {
    console.log('\n=== 测试1: 查询工作流定义 ===');

    if (!this.token) {
      console.log('❌ 未登录');
      return { success: false };
    }

    try {
      const response = await this.client.get('/workflow/definitions', {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        const definitions = response.data.data;
        console.log('✅ 查询工作流定义成功');
        console.log(`   数量: ${definitions.length}`);

        definitions.forEach(def => {
          console.log(`   - ${def.name}: ${def.description || '无描述'}`);
          console.log(`     实体类型: ${def.entityType}`);
          console.log(`     状态: ${def.status}`);
        });

        return { success: true, definitions: definitions };
      }
    } catch (error) {
      console.log('❌ 查询工作流定义失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试2: 查询工作流实例列表
   */
  async testGetWorkflowInstances(filters = {}) {
    console.log('\n=== 测试2: 查询工作流实例 ===');

    if (!this.token) {
      console.log('❌ 未登录');
      return { success: false };
    }

    try {
      const params = {
        page: 1,
        size: 10,
        ...filters
      };

      const response = await this.client.get('/workflow/instances', {
        headers: { Authorization: `Bearer ${this.token}` },
        params: params
      });

      if (response.data.success) {
        const instances = response.data.data;
        console.log('✅ 查询工作流实例成功');
        console.log(`   总数: ${instances.total}`);
        console.log(`   当前页: ${instances.data.length} 个实例`);

        instances.data.slice(0, 3).forEach(instance => {
          console.log(`   - 实例${instance.id}: ${instance.status}`);
          console.log(`     实体: ${instance.entityType}/${instance.entityId}`);
        });

        return { success: true, instances: instances };
      }
    } catch (error) {
      console.log('❌ 查询工作流实例失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试3: 获取工作流实例详情
   */
  async testGetWorkflowInstance(instanceId) {
    console.log('\n=== 测试3: 获取工作流实例详情 ===');

    if (!instanceId) {
      console.log('❌ 缺少实例ID');
      return { success: false };
    }

    try {
      const response = await this.client.get(`/workflow/instances/${instanceId}`, {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        const instance = response.data.data;
        console.log('✅ 获取实例详情成功');
        console.log(`   ID: ${instance.id}`);
        console.log(`   定义ID: ${instance.definitionId}`);
        console.log(`   当前步骤: ${instance.currentStepId}`);
        console.log(`   状态: ${instance.status}`);
        console.log(`   发起人: ${instance.initiatorId}`);

        return { success: true, instance: instance };
      }
    } catch (error) {
      console.log('❌ 获取实例详情失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试4: 查询审批时间轴
   */
  async testGetWorkflowTimeline(instanceId) {
    console.log('\n=== 测试4: 查询审批时间轴 ===');

    if (!instanceId) {
      console.log('❌ 缺少实例ID');
      return { success: false };
    }

    try {
      const response = await this.client.get(`/workflow/instances/${instanceId}/timeline`, {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        const timeline = response.data.data;
        console.log('✅ 查询审批时间轴成功');
        console.log(`   节点数量: ${timeline.length}`);

        timeline.forEach((step, index) => {
          console.log(`   ${index + 1}. ${step.stepName}`);
          console.log(`      操作人: ${step.operator}`);
          console.log(`      时间: ${step.timestamp}`);
          console.log(`      动作: ${step.action}`);
          if (step.comment) {
            console.log(`      意见: ${step.comment}`);
          }
        });

        return { success: true, timeline: timeline };
      }
    } catch (error) {
      console.log('❌ 查询审批时间轴失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试5: 查询审批历史
   */
  async testGetWorkflowHistory(instanceId) {
    console.log('\n=== 测试5: 查询审批历史 ===');

    if (!instanceId) {
      console.log('❌ 缺少实例ID');
      return { success: false };
    }

    try {
      const response = await this.client.get(`/workflow/instances/${instanceId}/history`, {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        const history = response.data.data;
        console.log('✅ 查询审批历史成功');
        console.log(`   记录数量: ${history.length}`);

        history.slice(0, 5).forEach(record => {
          console.log(`   - ${record.action} by ${record.operator} at ${record.timestamp}`);
        });

        return { success: true, history: history };
      }
    } catch (error) {
      console.log('❌ 查询审批历史失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试6: 审批通过
   */
  async testApprove(instanceId, comment) {
    console.log('\n=== 测试6: 审批通过 ===');

    if (!instanceId) {
      console.log('❌ 缺少实例ID');
      return { success: false };
    }

    try {
      const response = await this.client.post(`/workflow/instances/${instanceId}/approve`, {
        comment: comment || '审批通过'
      }, {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        console.log('✅ 审批通过成功');
        console.log(`   意见: ${comment || '审批通过'}`);

        // 检查是否完成
        if (response.data.data.status === 'COMPLETED') {
          console.log('   工作流已完成');
        } else {
          console.log(`   进入下一步: ${response.data.data.currentStepId}`);
        }

        return { success: true, result: response.data.data };
      }
    } catch (error) {
      console.log('❌ 审批通过失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试7: 审批驳回
   */
  async testReject(instanceId, comment) {
    console.log('\n=== 测试7: 审批驳回 ===');

    if (!instanceId) {
      console.log('❌ 缺少实例ID');
      return { success: false };
    }

    if (!comment) {
      console.log('❌ 驳回必须填写意见');
      return { success: false };
    }

    try {
      const response = await this.client.post(`/workflow/instances/${instanceId}/reject`, {
        comment: comment
      }, {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        console.log('✅ 审批驳回成功');
        console.log(`   驳回意见: ${comment}`);
        console.log('   流程已回退');

        return { success: true, result: response.data.data };
      }
    } catch (error) {
      console.log('❌ 审批驳回失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 运行完整测试流程
   */
  async runFullWorkflow() {
    console.log('\n╔══════════════════════════════════════════╗');
    console.log('║     SISM Workflow模块 - 工作流测试     ║');
    console.log('╚══════════════════════════════════════════╝');

    // 1. 登录
    const loggedIn = await this.login();
    if (!loggedIn) {
      console.log('❌ 登录失败，无法继续测试');
      return;
    }

    // 2. 查询工作流定义
    await this.testGetWorkflowDefinitions();

    // 3. 查询工作流实例
    const instancesResult = await this.testGetWorkflowInstances();

    // 如果有实例，测试后续流程
    if (instancesResult.success && instancesResult.instances.data.length > 0) {
      const instanceId = instancesResult.instances.data[0].id;

      // 4. 获取实例详情
      await this.testGetWorkflowInstance(instanceId);

      // 5. 查询时间轴
      await this.testGetWorkflowTimeline(instanceId);

      // 6. 查询历史
      await this.testGetWorkflowHistory(instanceId);

      // 7. 测试审批通过（如果是待审批状态）
      const instance = await this.testGetWorkflowInstance(instanceId);
      if (instance.success && instance.instance.status === 'PENDING') {
        await this.testApprove(instanceId, '测试审批通过');
      }
    } else {
      console.log('⚠️  没有找到工作流实例，跳过实例相关测试');
    }

    console.log('\n✅ Workflow模块测试完成');
  }
}

module.exports = WorkflowTester;

if (require.main === module) {
  const tester = new WorkflowTester();
  tester.runFullWorkflow()
    .catch(error => console.error('测试异常:', error));
}
