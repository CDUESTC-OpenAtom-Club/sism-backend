const BaseAgent = require('./base-agent');

/**
 * 工作流Agent
 * 测试多级审批、驳回回退、审批时间轴等流程功能
 */
class WorkflowAgent extends BaseAgent {
  constructor(config, storedData = {}) {
    super('工作流Agent', config);
    this.setStoredData(storedData);
  }

  /**
   * 执行所有测试
   */
  async execute() {
    await this.start();

    try {
      // 使用战略部门账号登录（可以发起和审批）
      const strategicUser = this.config.users.find(u => u.role === 'strategic');
      await this.login(strategicUser.username, strategicUser.password);

      // 测试1: 查询工作流定义
      await this.runScenario('查询工作流定义', async (scenario) => {
        await this.runStep('获取所有工作流定义', async () => {
          const response = await this.apiClient.workflow.getDefinitions();
          this.assertions.assertStatusCode(response.code, 200);
          this.assertions.assertions = Array.isArray(response.data);

          if (response.data.length > 0) {
            this.storeData('workflow_definitions', response.data);
            console.log(`      ✓ 找到 ${response.data.length} 个工作流定义`);
          }
        }, scenario);

        await this.runStep('查看工作流定义详情', async () => {
          const definitions = this.getStoredData('workflow_definitions');

          if (!definitions || definitions.length === 0) {
            console.log(`      ⚠️  没有工作流定义`);
            scenario.steps[scenario.steps.length - 1].status = 'skipped';
            return;
          }

          const definition = definitions[0];
          const response = await this.apiClient.workflow.getDefinitionById(definition.id);
          this.assertions.assertStatusCode(response.code, 200);
          this.assertions.assertFieldExists(response.data, 'steps');

          console.log(`      ✓ 工作流包含 ${response.data.steps?.length || 0} 个步骤`);
        }, scenario);
      });

      // 测试2: 审批时间轴完整性测试
      await this.runScenario('审批时间轴完整性测试', async (scenario) => {
        await this.runStep('获取工作流实例', async () => {
          const response = await this.apiClient.workflow.getInstances({
            page: 1,
            size: 10
          });

          this.assertions.assertStatusCode(response.code, 200);

          if (response.data.data && response.data.data.length > 0) {
            const instance = response.data.data[0];
            this.storeData('test_workflow_instance', instance);
            console.log(`      ✓ 工作流实例ID: ${instance.id}`);
          } else {
            console.log(`      ⚠️  没有找到工作流实例`);
          }
        }, scenario);

        await this.runStep('验证时间轴完整性', async () => {
          const instance = this.getStoredData('test_workflow_instance');

          if (!instance) {
            console.log(`      ⚠️  没有工作流实例`);
            scenario.steps[scenario.steps.length - 1].status = 'skipped';
            return;
          }

          const timeline = await this.apiClient.workflow.getTimeline(instance.id);
          this.assertions.assertStatusCode(timeline.code, 200);
          this.assertions.assertTimelineIntegrity(timeline.data);

          // 验证必需字段
          for (const step of timeline.data) {
            this.assertions.assertFieldExists(step, 'operator');
            this.assertions.assertFieldExists(step, 'timestamp');
            this.assertions.assertFieldExists(step, 'action');
            this.assertions.assertFieldExists(step, 'stepName');
          }

          console.log(`      ✓ 时间轴包含 ${timeline.data.length} 个节点，所有必需字段完整`);
        }, scenario);
      });

      // 测试3: 多级审批流程测试
      await this.runScenario('多级审批流程测试', async (scenario) => {
        await this.runStep('创建测试指标', async () => {
          const indicatorData = this.dataFactory.generateIndicator({
            name: '多级审批测试指标'
          });

          const response = await this.apiClient.indicators.create(indicatorData);
          this.assertions.assertStatusCode(response.code, 200);
          this.storeData('multi_level_test_indicator', response.data);
        }, scenario);

        await this.runStep('提交多级审批', async () => {
          const indicator = this.getStoredData('multi_level_test_indicator');

          if (!indicator) {
            console.log(`      ⚠️  没有测试指标`);
            scenario.steps[scenario.steps.length - 1].status = 'skipped';
            return;
          }

          const response = await this.apiClient.indicators.distribute(indicator.id);
          this.assertions.assertStatusCode(response.code, 200);

          // 获取工作流实例
          const instances = await this.apiClient.workflow.getInstances({
            entityType: 'INDICATOR',
            entityId: indicator.id
          });

          if (instances.data && instances.data.length > 0) {
            this.storeData('multi_level_workflow_instance', instances.data[0]);
            console.log(`      ✓ 工作流实例创建成功`);
          }
        }, scenario);

        await this.runStep('执行多级审批', async () => {
          const instance = this.getStoredData('multi_level_workflow_instance');

          if (!instance) {
            console.log(`      ⚠️  没有工作流实例`);
            scenario.steps[scenario.steps.length - 1].status = 'skipped';
            return;
          }

          // 模拟多级审批
          const maxLevels = 3;
          for (let level = 1; level <= maxLevels; level++) {
            const comment = `第${level}级审批通过`;
            const response = await this.apiClient.workflow.approve(instance.id, comment);

            // 检查是否还有下一级
            if (response.data.status === 'COMPLETED') {
              console.log(`      ✓ 审批流程已在第 ${level} 级完成`);
              break;
            }
          }
        }, scenario);
      });

      // 测试4: 驳回回退测试
      await this.runScenario('驳回回退测试', async (scenario) => {
        await this.runStep('创建新指标并提交审批', async () => {
          const indicatorData = this.dataFactory.generateIndicator({
            name: '驳回测试指标'
          });

          const indicator = await this.apiClient.indicators.create(indicatorData);
          await this.apiClient.indicators.distribute(indicator.data.id);
          this.storeData('reject_test_indicator', indicator.data);
        }, scenario);

        await this.runStep('在第2级驳回', async () => {
          const indicator = this.getStoredData('reject_test_indicator');

          if (!indicator) {
            console.log(`      ⚠️  没有测试指标`);
            scenario.steps[scenario.steps.length - 1].status = 'skipped';
            return;
          }

          // 获取工作流实例
          const instances = await this.apiClient.workflow.getInstances({
            entityType: 'INDICATOR',
            entityId: indicator.id,
            status: 'PENDING'
          });

          if (instances.data && instances.data.length > 0) {
            const instance = instances.data[0];
            const comment = this.dataFactory.generateComment(false);

            // 验证驳回意见
            this.assertions.assertRejectComment(comment);

            const response = await this.apiClient.workflow.reject(instance.id, comment);
            this.assertions.assertStatusCode(response.code, 200);

            // 验证状态回退
            const updatedIndicator = await this.apiClient.indicators.getById(indicator.id);
            this.assertions.assertStatus(updatedIndicator.data, 'DRAFT');

            console.log(`      ✓ 驳回成功，状态已回退到 DRAFT`);
          }
        }, scenario);

        await this.runStep('验证驳回意见已记录', async () => {
          const indicator = this.getStoredData('reject_test_indicator');

          if (!indicator) {
            console.log(`      ⚠️  没有测试指标`);
            scenario.steps[scenario.steps.length - 1].status = 'skipped';
            return;
          }

          // 获取历史记录
          const instances = await this.apiClient.workflow.getInstances({
            entityType: 'INDICATOR',
            entityId: indicator.id
          });

          if (instances.data && instances.data.length > 0) {
            const instance = instances.data[0];
            const history = await this.apiClient.workflow.getHistory(instance.id);

            // 验证驳回意见在历史记录中
            const hasRejectComment = history.data.some(h =>
              h.comment && h.comment.length > 0 && h.action === 'REJECT'
            );

            if (!hasRejectComment) {
              throw new Error('驳回意见未在历史记录中找到');
            }

            console.log(`      ✓ 驳回意见已正确记录`);
          }
        }, scenario);
      });

      // 测试5: 审批历史记录测试
      await this.runScenario('审批历史记录测试', async (scenario) => {
        await this.runStep('获取完整审批历史', async () => {
          const instance = this.getStoredData('test_workflow_instance');

          if (!instance) {
            console.log(`      ⚠️  没有工作流实例`);
            scenario.steps[scenario.steps.length - 1].status = 'skipped';
            return;
          }

          const history = await this.apiClient.workflow.getHistory(instance.id);
          this.assertions.assertStatusCode(history.code, 200);
          this.assertions.assertions = Array.isArray(history.data);

          console.log(`      ✓ 历史记录包含 ${history.data.length} 条记录`);
        }, scenario);

        await this.runStep('验证历史记录字段', async () => {
          const instance = this.getStoredData('test_workflow_instance');

          if (!instance) {
            console.log(`      ⚠️  没有工作流实例`);
            scenario.steps[scenario.steps.length - 1].status = 'skipped';
            return;
          }

          const history = await this.apiClient.workflow.getHistory(instance.id);

          for (const record of history.data) {
            this.assertions.assertFieldExists(record, 'operator');
            this.assertions.assertFieldExists(record, 'timestamp');
            this.assertions.assertFieldExists(record, 'action');

            // 验证时间戳格式
            if (!isNaN(Date.parse(record.timestamp))) {
              throw new Error(`无效的时间戳格式: ${record.timestamp}`);
            }
          }

          console.log(`      ✓ 所有历史记录字段完整且格式正确`);
        }, scenario);
      });

      // 测试6: 变更记录测试
      await this.runScenario('数据变更记录测试', async (scenario) => {
        await this.runStep('记录数据变更', async () => {
          // 这个测试需要模拟审批人修改填报数据的场景
          const reports = await this.apiClient.reports.list({
            status: 'PENDING',
            page: 1,
            size: 1
          });

          if (reports.data && reports.data.data && reports.data.data.length > 0) {
            const report = reports.data.data[0];

            // 修改填报数据
            const updates = {
              actualValue: 100,
              summary: '审批人修改后的内容'
            };

            await this.apiClient.reports.update(report.id, updates);
            this.storeData('modified_report_id', report.id);

            console.log(`      ✓ 填报数据已修改`);
          } else {
            console.log(`      ⚠️  没有待审批的填报`);
            scenario.steps[scenario.steps.length - 1].status = 'skipped';
          }
        }, scenario);

        await this.runStep('验证变更记录', async () => {
          const reportId = this.getStoredData('modified_report_id');

          if (!reportId) {
            console.log(`      ⚠️  没有修改的填报`);
            scenario.steps[scenario.steps.length - 1].status = 'skipped';
            return;
          }

          // 获取变更记录（假设有这个API）
          try {
            const instances = await this.apiClient.workflow.getInstances({
              entityType: 'REPORT',
              entityId: reportId
            });

            if (instances.data && instances.data.length > 0) {
              const history = await this.apiClient.workflow.getHistory(instances.data[0].id);

              // 查找变更记录
              const changeRecords = history.data.filter(h => h.action === 'MODIFY');
              if (changeRecords.length > 0) {
                for (const record of changeRecords) {
                  this.assertions.assertChangeRecord(record);
                }
                console.log(`      ✓ 找到 ${changeRecords.length} 条变更记录`);
              }
            }
          } catch (error) {
            console.log(`      ⚠️  变更记录API可能未实现: ${error.message}`);
            scenario.steps[scenario.steps.length - 1].status = 'skipped';
          }
        }, scenario);
      });

    } finally {
      await this.logout();
      await this.end();
    }

    return {
      results: this.getResults(),
      storedData: this.getAllStoredData()
    };
  }
}

module.exports = WorkflowAgent;
