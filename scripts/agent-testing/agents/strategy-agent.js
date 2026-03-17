const BaseAgent = require('./base-agent');

/**
 * 战略部门Agent
 * 测试指标创建、下发、审批等第一阶段流程
 */
class StrategyAgent extends BaseAgent {
  constructor(config, storedData = {}) {
    super('战略部门Agent', config);
    this.setStoredData(storedData);
  }

  /**
   * 执行所有测试
   */
  async execute() {
    await this.start();

    try {
      // 登录战略部门账号
      const strategicUser = this.config.users.find(u => u.role === 'strategic');
      await this.login(strategicUser.username, strategicUser.password);

      // 测试1: 创建草稿指标
      await this.runScenario('创建草稿指标', async (scenario) => {
        await this.runStep('创建单个指标', async () => {
          const indicatorData = this.dataFactory.generateIndicator({
            name: '2026年度核心指标',
            code: 'CORE_2026_001'
          });

          const response = await this.apiClient.indicators.create(indicatorData);
          this.assertions.assertStatusCode(response.code, 200);
          this.assertions.assertFieldExists(response.data, 'id');
          this.assertions.assertStatus(response.data, 'DRAFT');

          this.storeData('created_indicator', response.data);
          console.log(`      ✓ 指标ID: ${response.data.id}`);
        }, scenario);

        await this.runStep('批量创建指标', async () => {
          const indicators = this.dataFactory.generateIndicators(3);
          const createdIndicators = [];

          for (const indicator of indicators) {
            const response = await this.apiClient.indicators.create(indicator);
            this.assertions.assertStatusCode(response.code, 200);
            createdIndicators.push(response.data);
          }

          this.storeData('batch_indicators', createdIndicators);
          console.log(`      ✓ 创建了 ${createdIndicators.length} 个指标`);
        }, scenario);
      });

      // 测试2: 更新指标
      await this.runScenario('更新指标信息', async (scenario) => {
        await this.runStep('修改草稿状态的指标', async () => {
          const indicator = this.getStoredData('created_indicator');
          const updates = {
            name: '2026年度核心指标（修订版）',
            targetValue: 95
          };

          const response = await this.apiClient.indicators.update(indicator.id, updates);
          this.assertions.assertStatusCode(response.code, 200);
          this.assertions.assertFieldValue(response.data, 'name', updates.name);
        }, scenario);
      });

      // 测试3: 下发指标（触发审批）
      await this.runScenario('指标下发审批流程', async (scenario) => {
        await this.runStep('提交指标下发审批', async () => {
          const indicator = this.getStoredData('created_indicator');

          // 验证状态转换
          this.assertions.assertIndicatorStatusTransition('DRAFT', 'PENDING', 'distribute');

          const response = await this.apiClient.indicators.distribute(indicator.id);
          this.assertions.assertStatusCode(response.code, 200);

          // 验证状态已变更
          const updatedIndicator = await this.apiClient.indicators.getById(indicator.id);
          this.assertions.assertStatus(updatedIndicator.data, 'PENDING');

          // 存储工作流实例ID（如果返回）
          if (response.data.workflowInstanceId) {
            this.storeData('workflow_instance_id', response.data.workflowInstanceId);
          }

          console.log(`      ✓ 指标状态已更新为 PENDING`);
        }, scenario);

        await this.runStep('查询审批时间轴', async () => {
          const indicator = this.getStoredData('created_indicator');

          // 获取工作流实例
          const instances = await this.apiClient.workflow.getInstances({
            entityType: 'INDICATOR',
            entityId: indicator.id
          });

          if (instances.data && instances.data.length > 0) {
            const instance = instances.data[0];
            this.storeData('workflow_instance', instance);

            // 获取时间轴
            const timeline = await this.apiClient.workflow.getTimeline(instance.id);
            this.assertions.assertTimelineIntegrity(timeline.data);
            console.log(`      ✓ 审批时间轴完整`);
          }
        }, scenario);
      });

      // 测试4: 审批通过下发
      await this.runScenario('审批通过指标下发', async (scenario) => {
        await this.runStep('审批人审批通过', async () => {
          const instance = this.getStoredData('workflow_instance');

          if (!instance) {
            console.log(`      ⚠️  没有工作流实例，跳过审批步骤`);
            scenario.steps[scenario.steps.length - 1].status = 'skipped';
            return;
          }

          const comment = this.dataFactory.generateComment(true);
          const response = await this.apiClient.workflow.approve(instance.id, comment);
          this.assertions.assertStatusCode(response.code, 200);

          // 验证指标状态变为已下发
          const indicator = this.getStoredData('created_indicator');
          const updatedIndicator = await this.apiClient.indicators.getById(indicator.id);
          this.assertions.assertStatus(updatedIndicator.data, 'DISTRIBUTED');

          console.log(`      ✓ 指标状态已更新为 DISTRIBUTED`);
        }, scenario);
      });

      // 测试5: 查询指标列表
      await this.runScenario('查询指标列表', async (scenario) => {
        await this.runStep('获取所有指标', async () => {
          const response = await this.apiClient.indicators.list({
            page: 1,
            size: 20
          });

          this.assertions.assertStatusCode(response.code, 200);
          this.assertions.assertPagination(response.data);
          console.log(`      ✓ 共找到 ${response.data.total} 个指标`);
        }, scenario);

        await this.runStep('按状态筛选指标', async () => {
          const response = await this.apiClient.indicators.list({
            status: 'DISTRIBUTED'
          });

          this.assertions.assertStatusCode(response.code, 200);

          // 验证所有返回的指标都是DISTRIBUTED状态
          for (const indicator of response.data.data) {
            this.assertions.assertStatus(indicator, 'DISTRIBUTED');
          }

          console.log(`      ✓ 找到 ${response.data.data.length} 个已下发指标`);
        }, scenario);
      });

      // 测试6: 指标驳回流程
      await this.runScenario('指标审批驳回流程', async (scenario) => {
        await this.runStep('创建新指标并提交审批', async () => {
          const indicatorData = this.dataFactory.generateIndicator({
            name: '待驳回测试指标'
          });

          const indicator = await this.apiClient.indicators.create(indicatorData);
          this.storeData('test_indicator_for_rejection', indicator.data);

          // 提交审批
          await this.apiClient.indicators.distribute(indicator.data.id);
          console.log(`      ✓ 测试指标已提交审批`);
        }, scenario);

        await this.runStep('审批人驳回指标', async () => {
          const indicator = this.getStoredData('test_indicator_for_rejection');

          // 获取工作流实例
          const instances = await this.apiClient.workflow.getInstances({
            entityType: 'INDICATOR',
            entityId: indicator.id,
            status: 'PENDING'
          });

          if (instances.data && instances.data.length > 0) {
            const instance = instances.data[0];
            const comment = this.dataFactory.generateComment(false);

            const response = await this.apiClient.workflow.reject(instance.id, comment);
            this.assertions.assertStatusCode(response.code, 200);

            // 验证指标状态回退到草稿
            const updatedIndicator = await this.apiClient.indicators.getById(indicator.id);
            this.assertions.assertStatus(updatedIndicator.data, 'DRAFT');

            console.log(`      ✓ 指标已驳回，状态回退到 DRAFT`);
          }
        }, scenario);
      });

      // 测试7: 删除草稿指标
      await this.runScenario('删除草稿指标', async (scenario) => {
        await this.runStep('删除草稿状态的指标', async () => {
          const indicatorData = this.dataFactory.generateIndicator();
          const indicator = await this.apiClient.indicators.create(indicatorData);

          const response = await this.apiClient.indicators.delete(indicator.data.id);
          this.assertions.assertStatusCode(response.code, 204);

          // 验证指标已删除
          try {
            await this.apiClient.indicators.getById(indicator.data.id);
            throw new Error('指标应该已被删除');
          } catch (error) {
            if (error.httpStatus === 404) {
              console.log(`      ✓ 指标已成功删除`);
            } else {
              throw error;
            }
          }
        }, scenario);
      });

      // 测试8: 创建考核周期
      await this.runScenario('创建考核周期', async (scenario) => {
        await this.runStep('创建2026年度周期', async () => {
          const cycleData = this.dataFactory.generateCycle({
            name: '2026年度考核周期',
            year: 2026
          });

          const response = await this.apiClient.cycles.create(cycleData);
          this.assertions.assertStatusCode(response.code, 200);
          this.assertions.assertFieldExists(response.data, 'id');

          this.storeData('assessment_cycle', response.data);
          console.log(`      ✓ 周期ID: ${response.data.id}`);
        }, scenario);

        await this.runStep('激活考核周期', async () => {
          const cycle = this.getStoredData('assessment_cycle');

          if (!cycle) {
            console.log(`      ⚠️  没有考核周期，跳过激活步骤`);
            scenario.steps[scenario.steps.length - 1].status = 'skipped';
            return;
          }

          const response = await this.apiClient.cycles.activate(cycle.id);
          this.assertions.assertStatusCode(response.code, 200);

          const updatedCycle = await this.apiClient.cycles.getById(cycle.id);
          this.assertions.assertStatus(updatedCycle.data, 'ACTIVE');

          console.log(`      ✓ 周期已激活`);
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

module.exports = StrategyAgent;
