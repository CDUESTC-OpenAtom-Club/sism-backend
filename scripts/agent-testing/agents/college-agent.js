const BaseAgent = require('./base-agent');

/**
 * 学院Agent
 * 测试进度填报、提交审批等核心循环流程
 */
class CollegeAgent extends BaseAgent {
  constructor(config, storedData = {}) {
    super('学院Agent', config);
    this.setStoredData(storedData);
  }

  /**
   * 执行所有测试
   */
  async execute() {
    await this.start();

    try {
      // 登录学院账号
      const collegeUser = this.config.users.find(u => u.role === 'college');
      await this.login(collegeUser.username, collegeUser.password);

      // 测试1: 查看已分配的指标
      await this.runScenario('查看已分配指标', async (scenario) => {
        await this.runStep('获取已下发的指标', async () => {
          const response = await this.apiClient.indicators.list({
            status: 'DISTRIBUTED',
            page: 1,
            size: 20
          });

          this.assertions.assertStatusCode(response.code, 200);

          if (response.data.data && response.data.data.length > 0) {
            const assignedIndicators = response.data.data;
            this.storeData('assigned_indicators', assignedIndicators);
            console.log(`      ✓ 分配了 ${assignedIndicators.length} 个指标`);

            // 验证周期标识
            for (const indicator of assignedIndicators) {
              this.assertions.assertPeriodFormat(indicator.period);
            }
            console.log(`      ✓ 所有指标周期格式正确`);
          } else {
            console.log(`      ⚠️  没有已分配的指标`);
          }
        }, scenario);
      });

      // 测试2: 创建填报（草稿）
      await this.runScenario('创建填报草稿', async (scenario) => {
        await this.runStep('为指标创建填报', async () => {
          const assignedIndicators = this.getStoredData('assigned_indicators');

          if (!assignedIndicators || assignedIndicators.length === 0) {
            console.log(`      ⚠️  没有已分配的指标，跳过填报创建`);
            scenario.steps[scenario.steps.length - 1].status = 'skipped';
            return;
          }

          const indicator = assignedIndicators[0];
          const reportData = this.dataFactory.generateReport(indicator.id, {
            period: indicator.period
          });

          const response = await this.apiClient.reports.create(reportData);
          this.assertions.assertStatusCode(response.code, 200);
          this.assertions.assertStatus(response.data, 'DRAFT');

          this.storeData('draft_report', response.data);
          console.log(`      ✓ 创建填报，ID: ${response.data.id}`);
        }, scenario);

        await this.runStep('保存多次草稿', async () => {
          const assignedIndicators = this.getStoredData('assigned_indicators');
          const draftReport = this.getStoredData('draft_report');

          if (!assignedIndicators || assignedIndicators.length === 0 || !draftReport) {
            console.log(`      ⚠️  缺少数据，跳过多草稿测试`);
            scenario.steps[scenario.steps.length - 1].status = 'skipped';
            return;
          }

          // 修改草稿内容
          const updates = {
            actualValue: this.dataFactory.randomNumber(70, 95),
            summary: '更新后的填报总结'
          };

          const response = await this.apiClient.reports.update(draftReport.id, updates);
          this.assertions.assertStatusCode(response.code, 200);
          this.assertions.assertFieldValue(response.data, 'status', 'DRAFT');

          console.log(`      ✓ 草稿更新成功，状态仍为 DRAFT`);
        }, scenario);
      });

      // 测试3: 提交填报审批
      await this.runScenario('提交填报审批', async (scenario) => {
        await this.runStep('提交填报', async () => {
          const draftReport = this.getStoredData('draft_report');

          if (!draftReport) {
            console.log(`      ⚠️  没有草稿填报，跳过提交步骤`);
            scenario.steps[scenario.steps.length - 1].status = 'skipped';
            return;
          }

          // 验证状态转换
          this.assertions.assertReportStatusTransition('DRAFT', 'PENDING', 'submit');

          const response = await this.apiClient.reports.submit(draftReport.id);
          this.assertions.assertStatusCode(response.code, 200);

          // 验证状态
          const updated = await this.apiClient.reports.getById(draftReport.id);
          this.assertions.assertStatus(updated.data, 'PENDING');

          this.storeData('submitted_report', updated.data);
          console.log(`      ✓ 填报已提交，状态更新为 PENDING`);
        }, scenario);

        await this.runStep('查看审批时间轴', async () => {
          const submittedReport = this.getStoredData('submitted_report');

          if (!submittedReport) {
            console.log(`      ⚠️  没有已提交的填报`);
            scenario.steps[scenario.steps.length - 1].status = 'skipped';
            return;
          }

          // 获取工作流实例
          const instances = await this.apiClient.workflow.getInstances({
            entityType: 'REPORT',
            entityId: submittedReport.id
          });

          if (instances.data && instances.data.length > 0) {
            const instance = instances.data[0];
            const timeline = await this.apiClient.workflow.getTimeline(instance.id);
            this.assertions.assertTimelineIntegrity(timeline.data);
            console.log(`      ✓ 审批时间轴完整，共 ${timeline.data.length} 个节点`);
          }
        }, scenario);
      });

      // 测试4: 填报撤回功能
      await this.runScenario('填报撤回功能', async (scenario) => {
        await this.runStep('创建并提交新填报', async () => {
          const assignedIndicators = this.getStoredData('assigned_indicators');

          if (!assignedIndicators || assignedIndicators.length === 0) {
            console.log(`      ⚠️  没有指标，跳过测试`);
            scenario.steps[scenario.steps.length - 1].status = 'skipped';
            return;
          }

          const indicator = assignedIndicators[0];
          const reportData = this.dataFactory.generateReport(indicator.id, {
            period: indicator.period
          });

          const report = await this.apiClient.reports.create(reportData);
          await this.apiClient.reports.submit(report.data.id);

          this.storeData('report_to_withdraw', report.data);
          console.log(`      ✓ 新填报已创建并提交`);
        }, scenario);

        await this.runStep('撤回填报', async () => {
          const report = this.getStoredData('report_to_withdraw');

          if (!report) {
            console.log(`      ⚠️  没有可撤回的填报`);
            scenario.steps[scenario.steps.length - 1].status = 'skipped';
            return;
          }

          // 撤回填报
          const response = await this.apiClient.reports.withdraw(report.id);
          this.assertions.assertStatusCode(response.code, 200);

          // 验证状态回退到草稿
          const updated = await this.apiClient.reports.getById(report.id);
          this.assertions.assertStatus(updated.data, 'DRAFT');

          console.log(`      ✓ 填报已撤回，状态回退到 DRAFT`);
        }, scenario);
      });

      // 测试5: 驳回后重新提交
      await this.runScenario('驳回后重新提交', async (scenario) => {
        await this.runStep('模拟填报被驳回', async () => {
          // 这个测试需要先有审批人驳回填报
          // 这里只是检查如何处理被驳回的填报
          const response = await this.apiClient.reports.list({
            status: 'DRAFT',
            page: 1,
            size: 10
          });

          this.assertions.assertStatusCode(response.code, 200);
          console.log(`      ✓ 查询到草稿状态的填报`);
        }, scenario);

        await this.runStep('修改被驳回的填报', async () => {
          const draftReport = this.getStoredData('draft_report');

          if (!draftReport) {
            console.log(`      ⚠️  没有草稿填报`);
            scenario.steps[scenario.steps.length - 1].status = 'skipped';
            return;
          }

          // 修改填报内容
          const updates = {
            actualValue: this.dataFactory.randomNumber(80, 100),
            measures: '根据驳回意见改进的措施'
          };

          const response = await this.apiClient.reports.update(draftReport.id, updates);
          this.assertions.assertStatusCode(response.code, 200);
          console.log(`      ✓ 填报内容已更新`);
        }, scenario);
      });

      // 测试6: 批量填报
      await this.runScenario('批量填报测试', async (scenario) => {
        await this.runStep('为多个指标创建填报', async () => {
          const assignedIndicators = this.getStoredData('assigned_indicators');

          if (!assignedIndicators || assignedIndicators.length < 2) {
            console.log(`      ⚠️  指标数量不足，跳过批量测试`);
            scenario.steps[scenario.steps.length - 1].status = 'skipped';
            return;
          }

          const reports = [];
          const batchCount = Math.min(3, assignedIndicators.length);

          for (let i = 0; i < batchCount; i++) {
            const indicator = assignedIndicators[i];
            const reportData = this.dataFactory.generateReport(indicator.id, {
              period: indicator.period
            });

            const response = await this.apiClient.reports.create(reportData);
            this.assertions.assertStatusCode(response.code, 200);
            reports.push(response.data);
          }

          this.storeData('batch_reports', reports);
          console.log(`      ✓ 创建了 ${reports.length} 个填报`);
        }, scenario);

        await this.runStep('批量提交填报', async () => {
          const batchReports = this.getStoredData('batch_reports');

          if (!batchReports || batchReports.length === 0) {
            console.log(`      ⚠️  没有批量填报`);
            scenario.steps[scenario.steps.length - 1].status = 'skipped';
            return;
          }

          for (const report of batchReports) {
            await this.apiClient.reports.submit(report.id);
            // 验证状态
            const updated = await this.apiClient.reports.getById(report.id);
            this.assertions.assertStatus(updated.data, 'PENDING');
          }

          console.log(`      ✓ ${batchReports.length} 个填报已提交`);
        }, scenario);
      });

      // 测试7: 查看历史填报
      await this.runScenario('查看历史填报', async (scenario) => {
        await this.runStep('按周期查询填报', async () => {
          const assignedIndicators = this.getStoredData('assigned_indicators');

          if (!assignedIndicators || assignedIndicators.length === 0) {
            console.log(`      ⚠️  没有指标，跳过历史查询`);
            scenario.steps[scenario.steps.length - 1].status = 'skipped';
            return;
          }

          const indicator = assignedIndicators[0];
          const response = await this.apiClient.reports.getByIndicator(indicator.id);
          this.assertions.assertStatusCode(response.code, 200);

          if (response.data && response.data.length > 0) {
            console.log(`      ✓ 找到 ${response.data.length} 个历史填报`);

            // 验证周期标识
            for (const report of response.data) {
              this.assertions.assertPeriodFormat(report.period);
            }
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

module.exports = CollegeAgent;
