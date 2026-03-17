/**
 * 测试数据生成器
 * 用于生成随机但有效的测试数据
 */

class DataFactory {
  constructor() {
    this.timestamp = Date.now();
  }

  /**
   * 生成随机字符串
   */
  randomString(prefix = '', length = 8) {
    const random = Math.random().toString(36).substring(2, 2 + length);
    return `${prefix}${random}_${this.timestamp}`;
  }

  /**
   * 生成随机数字
   */
  randomNumber(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
  }

  /**
   * 生成随机百分比
   */
  randomPercentage() {
    return this.randomNumber(0, 100);
  }

  /**
   * 生成随机日期
   */
  randomDate(startYear = 2024, endYear = 2026) {
    const start = new Date(startYear, 0, 1);
    const end = new Date(endYear, 11, 31);
    return new Date(start.getTime() + Math.random() * (end.getTime() - start.getTime()));
  }

  /**
   * 生成周期标识
   */
  generatePeriod(year = null, month = null) {
    const y = year || this.randomDate().getFullYear();
    const m = month !== null ? month : this.randomNumber(1, 12);
    return `${y}-${m.toString().padStart(2, '0')}`;
  }

  /**
   * 生成指标数据
   */
  generateIndicator(overrides = {}) {
    const indicatorTypes = ['QUANTITATIVE', 'QUALITATIVE'];
    const weights = [1, 2, 3, 5];

    return {
      name: this.randomString('指标'),
      code: this.randomString('IND', 6).toUpperCase(),
      description: `测试指标描述_${this.timestamp}`,
      type: indicatorTypes[this.randomNumber(0, 1)],
      weight: weights[this.randomNumber(0, 3)],
      targetValue: this.randomNumber(80, 100),
      unit: '个',
      period: this.generatePeriod(),
      status: 'DRAFT',
      ...overrides
    };
  }

  /**
   * 生成子指标数据
   */
  generateChildIndicator(parentId, overrides = {}) {
    return this.generateIndicator({
      parentIndicatorId: parentId,
      name: this.randomString('子指标'),
      code: this.randomString('SUB', 6).toUpperCase(),
      ...overrides
    });
  }

  /**
   * 生成填报数据
   */
  generateReport(indicatorId, overrides = {}) {
    return {
      indicatorId,
      period: this.generatePeriod(),
      actualValue: this.randomNumber(60, 100),
      completionRate: this.randomPercentage(),
      status: 'DRAFT',
      summary: `测试填报总结_${this.timestamp}`,
      problems: `测试问题描述_${this.timestamp}`,
      measures: `测试改进措施_${this.timestamp}`,
      nextPlan: `测试下步计划_${this.timestamp}`,
      ...overrides
    };
  }

  /**
   * 生成周期数据
   */
  generateCycle(overrides = {}) {
    const currentYear = new Date().getFullYear();

    return {
      name: `${currentYear}年度考核周期`,
      year: currentYear,
      startDate: `${currentYear}-01-01`,
      endDate: `${currentYear}-12-31`,
      status: 'DRAFT',
      description: `测试周期_${this.timestamp}`,
      ...overrides
    };
  }

  /**
   * 生成用户数据
   */
  generateUser(overrides = {}) {
    const roles = ['STRATEGIC', 'FUNCTIONAL', 'COLLEGE'];

    return {
      username: this.randomString('user', 6),
      realName: this.randomString('测试用户'),
      email: `test_${this.timestamp}@example.com`,
      phone: `138${this.randomNumber(10000000, 99999999)}`,
      role: roles[this.randomNumber(0, 2)],
      orgId: this.randomNumber(1, 10),
      status: 'ACTIVE',
      ...overrides
    };
  }

  /**
   * 生成审批意见
   */
  generateComment(positive = true) {
    const positiveComments = [
      '符合要求，同意通过',
      '内容完整，数据准确',
      '已完成目标任务，同意'
    ];

    const negativeComments = [
      '数据不准确，请重新核对',
      '内容不完整，请补充',
      '未达到要求，需要改进',
      '填报格式有误，请修改'
    ];

    const comments = positive ? positiveComments : negativeComments;
    return comments[this.randomNumber(0, comments.length - 1)];
  }

  /**
   * 生成工作流定义数据
   */
  generateWorkflowDefinition(overrides = {}) {
    return {
      name: this.randomString('工作流'),
      description: `测试工作流_${this.timestamp}`,
      entity_type: 'INDICATOR',
      status: 'ACTIVE',
      ...overrides
    };
  }

  /**
   * 生成组织数据
   */
  generateOrganization(overrides = {}) {
    const orgTypes = ['STRATEGIC', 'FUNCTIONAL', 'COLLEGE'];

    return {
      name: this.randomString('组织'),
      code: this.randomString('ORG', 4).toUpperCase(),
      type: orgTypes[this.randomNumber(0, 2)],
      parentId: null,
      status: 'ACTIVE',
      description: `测试组织_${this.timestamp}`,
      ...overrides
    };
  }

  /**
   * 生成告警数据
   */
  generateAlert(overrides = {}) {
    const alertTypes = ['DEADLINE', 'PROGRESS_LOW', 'TASK_OVERDUE'];
    const levels = ['INFO', 'WARNING', 'ERROR', 'CRITICAL'];

    return {
      type: alertTypes[this.randomNumber(0, 2)],
      level: levels[this.randomNumber(0, 3)],
      title: this.randomString('告警'),
      message: `测试告警消息_${this.timestamp}`,
      status: 'UNREAD',
      ...overrides
    };
  }

  /**
   * 生成批量指标数据
   */
  generateIndicators(count, parentIndicatorId = null) {
    const indicators = [];
    for (let i = 0; i < count; i++) {
      if (parentIndicatorId) {
        indicators.push(this.generateChildIndicator(parentIndicatorId));
      } else {
        indicators.push(this.generateIndicator());
      }
    }
    return indicators;
  }

  /**
   * 生成批量填报数据
   */
  generateReports(indicatorIds) {
    return indicatorIds.map(id => this.generateReport(id));
  }

  /**
   * 生成进度数据
   */
  generateProgress() {
    return {
      completed: this.randomNumber(0, 50),
      total: this.randomNumber(50, 100),
      percentage: this.randomPercentage(),
      trend: ['UP', 'DOWN', 'STABLE'][this.randomNumber(0, 2)]
    };
  }
}

module.exports = DataFactory;
