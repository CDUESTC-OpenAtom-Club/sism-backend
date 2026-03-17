/**
 * SISM Task模块 - 任务管理API测试
 *
 * 测试流程：
 * 1. 创建任务
 * 2. 更新任务状态
 * 3. 查询任务列表
 * 4. 分配任务
 * 5. 完成任务
 */

const axios = require('axios');
const IamTester = require('../iam/test-auth');

const BASE_URL = 'http://localhost:8080/api/v1';

class TaskTester {
  constructor() {
    this.token = null;
    this.client = axios.create({
      baseURL: BASE_URL,
      timeout: 10000
    });
    this.createdTasks = [];
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
   * 测试1: 创建任务
   */
  async testCreateTask() {
    console.log('\n=== 测试1: 创建任务 ===');

    if (!this.token) {
      console.log('❌ 未登录');
      return { success: false };
    }

    const taskData = {
      title: `测试任务_${Date.now()}`,
      description: '这是一个测试任务',
      type: 'INDICATOR_COLLECTION',
      priority: 'HIGH',
      dueDate: '2026-03-31',
      assigneeId: 1, // 假设有ID为1的用户
      indicatorId: 1, // 假设有ID为1的指标
      status: 'PENDING'
    };

    try {
      const response = await this.client.post('/tasks', taskData, {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        const task = response.data.data;
        this.createdTasks.push(task);

        console.log('✅ 创建任务成功');
        console.log(`   ID: ${task.id}`);
        console.log(`   标题: ${task.title}`);
        console.log(`   类型: ${task.type}`);
        console.log(`   优先级: ${task.priority}`);
        console.log(`   状态: ${task.status}`);
        console.log(`   截止日期: ${task.dueDate}`);

        return { success: true, task: task };
      }
    } catch (error) {
      console.log('❌ 创建任务失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试2: 更新任务
   */
  async testUpdateTask(taskId) {
    console.log('\n=== 测试2: 更新任务 ===');

    if (!taskId) {
      console.log('❌ 缺少任务ID');
      return { success: false };
    }

    const updates = {
      title: `更新后的任务_${Date.now()}`,
      priority: 'MEDIUM'
    };

    try {
      const response = await this.client.put(`/tasks/${taskId}`, updates, {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        const task = response.data.data;
        console.log('✅ 更新任务成功');
        console.log(`   新标题: ${task.title}`);
        console.log(`   新优先级: ${task.priority}`);

        return { success: true, task: task };
      }
    } catch (error) {
      console.log('❌ 更新任务失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试3: 更新任务状态
   */
  async testUpdateTaskStatus(taskId, newStatus) {
    console.log('\n=== 测试3: 更新任务状态 ===');

    if (!taskId) {
      console.log('❌ 缺少任务ID');
      return { success: false };
    }

    try {
      const response = await this.client.patch(`/tasks/${taskId}/status`, {
        status: newStatus || 'IN_PROGRESS'
      }, {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        const task = response.data.data;
        console.log('✅ 更新任务状态成功');
        console.log(`   新状态: ${task.status}`);

        return { success: true, task: task };
      }
    } catch (error) {
      console.log('❌ 更新任务状态失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试4: 查询任务列表
   */
  async testListTasks(filters = {}) {
    console.log('\n=== 测试4: 查询任务列表 ===');

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

      const response = await this.client.get('/tasks', {
        headers: { Authorization: `Bearer ${this.token}` },
        params: params
      });

      if (response.data.success) {
        const data = response.data.data;
        console.log('✅ 查询任务列表成功');
        console.log(`   总数: ${data.total}`);
        console.log(`   当前页: ${data.data.length} 个任务`);

        // 显示前5个任务
        data.data.slice(0, 5).forEach(task => {
          console.log(`   - ${task.title} (${task.status})`);
          console.log(`     优先级: ${task.priority} | 截止: ${task.dueDate}`);
        });

        return { success: true, tasks: data };
      }
    } catch (error) {
      console.log('❌ 查询任务列表失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试5: 分配任务
   */
  async testAssignTask(taskId, assigneeId) {
    console.log('\n=== 测试5: 分配任务 ===');

    if (!taskId) {
      console.log('❌ 缺少任务ID');
      return { success: false };
    }

    try {
      const response = await this.client.post(`/tasks/${taskId}/assign`, {
        assigneeId: assigneeId || 1
      }, {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        const task = response.data.data;
        console.log('✅ 分配任务成功');
        console.log(`   分配给: ${task.assigneeName || task.assigneeId}`);

        return { success: true, task: task };
      }
    } catch (error) {
      console.log('❌ 分配任务失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试6: 完成任务
   */
  async testCompleteTask(taskId, completionData) {
    console.log('\n=== 测试6: 完成任务 ===');

    if (!taskId) {
      console.log('❌ 缺少任务ID');
      return { success: false };
    }

    try {
      const response = await this.client.post(`/tasks/${taskId}/complete`, {
        notes: completionData?.notes || '任务已完成',
        result: completionData?.result || '任务结果'
      }, {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        const task = response.data.data;
        console.log('✅ 完成任务成功');
        console.log(`   状态: ${task.status}`);
        console.log(`   完成时间: ${task.completedAt}`);

        return { success: true, task: task };
      }
    } catch (error) {
      console.log('❌ 完成任务失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 测试7: 查询我的任务
   */
  async testGetMyTasks() {
    console.log('\n=== 测试7: 查询我的任务 ===');

    if (!this.token) {
      console.log('❌ 未登录');
      return { success: false };
    }

    try {
      const response = await this.client.get('/tasks/my', {
        headers: { Authorization: `Bearer ${this.token}` }
      });

      if (response.data.success) {
        const tasks = response.data.data;
        console.log('✅ 查询我的任务成功');
        console.log(`   任务数量: ${tasks.length}`);

        tasks.slice(0, 5).forEach(task => {
          console.log(`   - ${task.title} (${task.status})`);
        });

        return { success: true, tasks: tasks };
      }
    } catch (error) {
      console.log('❌ 查询我的任务失败:', error.response?.data?.message || error.message);
      return { success: false, error: error.message };
    }
  }

  /**
   * 运行完整测试流程
   */
  async runFullWorkflow() {
    console.log('\n╔══════════════════════════════════════════╗');
    console.log('║     SISM Task模块 - 任务管理测试      ║');
    console.log('╚══════════════════════════════════════════╝');

    // 1. 登录
    const loggedIn = await this.login();
    if (!loggedIn) {
      console.log('❌ 登录失败，无法继续测试');
      return;
    }

    // 2. 创建任务
    const createResult = await this.testCreateTask();
    if (!createResult.success) {
      return;
    }

    const taskId = createResult.task.id;

    // 3. 更新任务
    await this.testUpdateTask(taskId);

    // 4. 更新任务状态
    await this.testUpdateTaskStatus(taskId, 'IN_PROGRESS');

    // 5. 分配任务
    await this.testAssignTask(taskId, 1);

    // 6. 查询任务列表
    await this.testListTasks();

    // 7. 查询我的任务
    await this.testGetMyTasks();

    // 8. 完成任务
    await this.testCompleteTask(taskId);

    console.log('\n✅ Task模块测试完成');
  }
}

module.exports = TaskTester;

if (require.main === module) {
  const tester = new TaskTester();
  tester.runFullWorkflow()
    .catch(error => console.error('测试异常:', error));
}
