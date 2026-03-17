const BaseAgent = require('./base-agent');

/**
 * 职能部门Agent
 * 测试指标拆分、二次下发等第二阶段流程
 */
class FunctionalAgent extends BaseAgent {
  constructor(config, storedData = {}) {
    super('职能部门Agent', config);
    this.setStoredData(storedData);
  }

  /**
   * 执行所有测试
   */
  async execute() {
    await this.start();

    try {
      // 登录职能部门账号
      const functionalUser = this.config.users.find(u => u.role === 'functional');
      await this.login(functionalUser.username, functionalUser.password);

      // 测试1: 接收父指标
      await this.runScenario('接收父指标', async (scenario) => {
        await this.runStep('获取已下发的指标', async () => {
          const response = await this.apiClient.indicators.list({
            status: 'DISTRIBUTED',
            page: 1,
            size: 10
          });

          this.assertions.assertStatusCode(response.code, 200);

          if (response.data.data && response.data.data.length > 0) {
            const parentIndicator = response.data.data[0];
            this.storeData('parent_indicator', parentIndicator);
            console.log(`      ✓ 父指标ID: ${parentIndicator.id}, 名称: ${parentIndicator.name}`);
          } else {
            console.log(`      ⚠️  没有找到已下发的指标`);
          }
        }, scenario);

        await this.runStep('查看父指标详情', async () => {
          const parentIndicator = this.getStoredData('parent_indicator');

          if (!parentIndicator) {
            console.log(`      ⚠️  没有父指标，跳过详情查看`);
            scenario.steps[scenario.steps.length - 1].status = 'skipped';
            return;
          }

          const response = await this.apiClient.indicators.getById(parentIndicator.id);
          this.assertions.assertStatusCode(response.code, 200);
          this.assertions.assertFieldValue(response.data, 'id', parentIndicator.id);
        }, scenario);
      });

      // 测试2: 拆分指标为子指标
      await this.runScenario('拆分指标为子指标', async (scenario) => {
        await this.runStep('创建多个子指标', async () => {
          const parentIndicator = this.getStoredData('parent_indicator');

          if (!parentIndicator) {
            console.log(`      ⚠️  没有父指标，跳过子指标创建`);
            scenario.steps[scenario.steps.length - 1].status = 'skipped';
            return;
          }

          const childIndicators = [];
          const childCount = 3;

          for (let i = 0; i < childCount; i++) {
            const childData = this.dataFactory.generateChildIndicator(parentIndicator.id, {
              name: `子指标_${i + 1}_${parentIndicator.name}`,
              code: `SUB_${parentIndicator.code}_${i + 1}`
            });

            const response = await this.apiClient.indicators.create(childData);
            this.assertions.assertStatusCode(response.code, 200);
            childIndicators.push(response.data);

            // 验证父子关系
            this.assertions.assertParentChildRelationship(parentIndicator, response.data);
          }

          this.storeData('child_indicators', childIndicators);
          console.log(`      ✓ 创建了 ${childIndicators.length} 个子指标`);
        }, scenario);
      });

      // 测试3: 验证父子指标关系
      await this.runScenario('验证父子指标关系', async (scenario) => {
        await this.runStep('查询子指标列表', async () => {
          const parentIndicator = this.getStoredData('parent_indicator');

          if (!parentIndicator) {
            console.log(`      ⚠️  没有父指标，跳过子指标查询`);
            scenario.steps[scenario.steps.length - 1].status = 'skipped';
            return;
          }

          const response = await this.apiClient.indicators.getChildren(parentIndicator.id);
          this.assertions.assertStatusCode(response.code, 200);
          this.assertions.assertions = Array.isArray(response.data);

          if (response.data.length > 0) {
            console.log(`      ✓ 找到 ${response.data.length} 个子指标`);
          }
        }, scenario);

        await this.runStep('验证没有进度聚合', async () => {
          const parentIndicator = this.getStoredData('parent_indicator');
          const childIndicators = this.getStoredData('child_indicators');

          if (!parentIndicator || !childIndicators || childIndicators.length === 0) {
            console.log(`      ⚠️  缺少数据，跳过进度聚合验证`);
            scenario.steps[scenario.steps.length - 1].status = 'skipped';
            return;
          }

          // 获取最新的父指标数据
          const parentResponse = await this.apiClient.indicators.getById(parentIndicator.id);
          this.assertions.assertNoProgressAggregation(parentResponse.data, childIndicators);
          console.log(`      ✓ 验证通过：父子指标独立，无进度聚合`);
        }, scenario);
      });

      // 测试4: 下发子指标到学院
      await this.runScenario('下发子指标到学院', async (scenario) => {
        await this.runStep('提交子指标下发审批', async () => {
          const childIndicators = this.getStoredData('child_indicators');

          if (!childIndicators || childIndicators.length === 0) {
            console.log(`      ⚠️  没有子指标，跳过下发步骤`);
            scenario.steps[scenario.steps.length - 1].status = 'skipped';
            return;
          }

          const distributedIndicators = [];

          for (const child of childIndicators) {
            // 验证状态转换
            this.assertions.assertIndicatorStatusTransition('DRAFT', 'PENDING', 'distribute');

            const response = await this.apiClient.indicators.distribute(child.id);
            this.assertions.assertStatusCode(response.code, 200);

            // 验证状态
            const updated = await this.apiClient.indicators.getById(child.id);
            this.assertions.assertStatus(updated.data, 'PENDING');

            distributedIndicators.push(updated.data);
          }

          this.storeData('distributed_child_indicators', distributedIndicators);
          console.log(`      ✓ 已提交 ${distributedIndicators.length} 个子指标的下发审批`);
        }, scenario);

        await this.runStep('审批通过子指标下发', async () => {
          const childIndicators = this.getStoredData('distributed_child_indicators');

          if (!childIndicators || childIndicators.length === 0) {
            console.log(`      ⚠️  没有待审批的子指标`);
            scenario.steps[scenario.steps.length - 1].status = 'skipped';
            return;
          }

          for (const child of childIndicators) {
            // 获取工作流实例
            const instances = await this.apiClient.workflow.getInstances({
              entityType: 'INDICATOR',
              entityId: child.id,
              status: 'PENDING'
            });

            if (instances.data && instances.data.length > 0) {
              const instance = instances.data[0];
              const comment = this.dataFactory.generateComment(true);

              await this.apiClient.workflow.approve(instance.id, comment);

              // 验证状态
              const updated = await this.apiClient.indicators.getById(child.id);
              this.assertions.assertStatus(updated.data, 'DISTRIBUTED');
            }
          }

          console.log(`      ✓ 子指标已全部审批通过并下发`);
        }, scenario);
      });

      // 测试5: 审批学院填报
      await this.runScenario('审批学院填报', async (scenario) => {
        await this.runStep('获取待审批的填报', async () => {
          // 这个测试需要先有学院提交的填报
          const response = await this.apiClient.reports.list({
            status: 'PENDING',
            page: 1,
            size: 10
          });

          this.assertions.assertStatusCode(response.code, 200);

          if (response.data.data && response.data.data.length > 0) {
            this.storeData('pending_reports', response.data.data);
            console.log(`      ✓ 找到 ${response.data.data.length} 个待审批填报`);
          } else {
            console.log(`      ⚠️  没有待审批的填报`);
          }
        }, scenario);

        await this.runStep('审批填报', async () => {
          const pendingReports = this.getStoredData('pending_reports');

          if (!pendingReports || pendingReports.length === 0) {
            console.log(`      ⚠️  没有待审批填报，跳过审批步骤`);
            scenario.steps[scenario.steps.length - 1].status = 'skipped';
            return;
          }

          const report = pendingReports[0];

          // 获取工作流实例
          const instances = await this.apiClient.workflow.getInstances({
            entityType: 'REPORT',
            entityId: report.id,
            status: 'PENDING'
          });

          if (instances.data && instances.data.length > 0) {
            const instance = instances.data[0];
            const comment = this.dataFactory.generateComment(true);

            const response = await this.apiClient.workflow.approve(instance.id, comment);
            this.assertions.assertStatusCode(response.code, 200);
            console.log(`      ✓ 填报已审批通过`);
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

module.exports = FunctionalAgent;
