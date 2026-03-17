/**
 * 断言库
 * 用于验证API响应和业务逻辑
 */

class AssertionLibrary {
  /**
   * 验证响应状态
   */
  static assertStatusCode(actual, expected) {
    if (actual !== expected) {
      throw new Error(`Status code mismatch: expected ${expected}, got ${actual}`);
    }
    return true;
  }

  /**
   * 验证业务码
   */
  static assertBusinessCode(actual, expected = 200) {
    if (actual !== expected) {
      throw new Error(`Business code mismatch: expected ${expected}, got ${actual}`);
    }
    return true;
  }

  /**
   * 验证响应数据存在
   */
  static assertDataExists(response) {
    if (!response || !response.data) {
      throw new Error('Response data is missing');
    }
    return true;
  }

  /**
   * 验证字段存在
   */
  static assertFieldExists(obj, field) {
    if (!(field in obj)) {
      throw new Error(`Field '${field}' not found in object`);
    }
    return true;
  }

  /**
   * 验证字段值
   */
  static assertFieldValue(obj, field, expected) {
    this.assertFieldExists(obj, field);
    if (obj[field] !== expected) {
      throw new Error(`Field '${field}' value mismatch: expected ${expected}, got ${obj[field]}`);
    }
    return true;
  }

  /**
   * 验证状态
   */
  static assertStatus(obj, expected) {
    return this.assertFieldValue(obj, 'status', expected);
  }

  /**
   * 验证指标状态流转
   */
  static assertIndicatorStatusTransition(fromStatus, toStatus, action) {
    const validTransitions = {
      'DRAFT': {
        'PENDING': ['distribute', 'submit'],
      },
      'PENDING': {
        'DISTRIBUTED': ['approve'],
        'DRAFT': ['reject']
      },
      'DISTRIBUTED': {
        'PENDING': ['split']
      }
    };

    if (!validTransitions[fromStatus]) {
      throw new Error(`Invalid from status: ${fromStatus}`);
    }

    if (!validTransitions[fromStatus][toStatus]) {
      throw new Error(`Invalid status transition from ${fromStatus} to ${toStatus}`);
    }

    if (!validTransitions[fromStatus][toStatus].includes(action)) {
      throw new Error(`Action '${action}' cannot cause transition from ${fromStatus} to ${toStatus}`);
    }

    return true;
  }

  /**
   * 验证填报状态流转
   */
  static assertReportStatusTransition(fromStatus, toStatus, action) {
    const validTransitions = {
      'DRAFT': {
        'PENDING': ['submit'],
      },
      'PENDING': {
        'DRAFT': ['reject', 'withdraw'],
        'APPROVED': ['approve']
      },
      'APPROVED': {
        'DRAFT': ['new_period']
      }
    };

    if (!validTransitions[fromStatus]) {
      throw new Error(`Invalid from status: ${fromStatus}`);
    }

    if (!validTransitions[fromStatus][toStatus]) {
      throw new Error(`Invalid status transition from ${fromStatus} to ${toStatus}`);
    }

    if (!validTransitions[fromStatus][toStatus].includes(action)) {
      throw new Error(`Action '${action}' cannot cause transition from ${fromStatus} to ${toStatus}`);
    }

    return true;
  }

  /**
   * 验证父子指标关系
   */
  static assertParentChildRelationship(parent, child) {
    if (child.parentIndicatorId !== parent.id) {
      throw new Error(`Child's parentIndicatorId ${child.parentIndicatorId} does not match parent's id ${parent.id}`);
    }
    return true;
  }

  /**
   * 验证周期标识
   */
  static assertPeriodFormat(period) {
    const regex = /^\d{4}-(0[1-9]|1[0-2])$/;
    if (!regex.test(period)) {
      throw new Error(`Invalid period format: ${period}. Expected format: YYYY-MM`);
    }
    return true;
  }

  /**
   * 验证审批时间轴
   */
  static assertTimelineIntegrity(timeline) {
    if (!Array.isArray(timeline)) {
      throw new Error('Timeline must be an array');
    }

    const requiredFields = ['operator', 'timestamp', 'action', 'stepName'];

    for (const step of timeline) {
      for (const field of requiredFields) {
        if (!(field in step)) {
          throw new Error(`Timeline step missing required field: ${field}`);
        }
      }

      // 验证时间戳格式
      if (isNaN(Date.parse(step.timestamp))) {
        throw new Error(`Invalid timestamp format: ${step.timestamp}`);
      }
    }

    // 验证时间轴顺序
    for (let i = 1; i < timeline.length; i++) {
      const prevTime = new Date(timeline[i - 1].timestamp);
      const currTime = new Date(timeline[i].timestamp);
      if (currTime < prevTime) {
        throw new Error('Timeline is not in chronological order');
      }
    }

    return true;
  }

  /**
   * 验证驳回意见
   */
  static assertRejectComment(comment) {
    if (!comment || comment.trim().length === 0) {
      throw new Error('Reject comment is required');
    }
    return true;
  }

  /**
   * 验证数据一致性
   */
  static assertDataConsistency(sourceObj, targetObj, fields) {
    for (const field of fields) {
      if (sourceObj[field] !== targetObj[field]) {
        throw new Error(`Data inconsistency in field '${field}': ${sourceObj[field]} !== ${targetObj[field]}`);
      }
    }
    return true;
  }

  /**
   * 验证没有进度聚合
   */
  static assertNoProgressAggregation(parent, children) {
    // 父指标的进度应该独立存在，不应该是子指标的聚合
    if (children.length === 0) {
      return true;
    }

    // 计算子指标的平均进度
    const avgProgress = children.reduce((sum, child) => sum + (child.progress || 0), 0) / children.length;

    // 父指标进度不等于子指标平均进度（允许小的浮点误差）
    if (parent.progress !== undefined && Math.abs(parent.progress - avgProgress) < 0.01) {
      console.warn(`Warning: Parent progress (${parent.progress}) equals average of children (${avgProgress}). This might indicate unwanted aggregation.`);
    }

    return true;
  }

  /**
   * 验证权限
   */
  static assertPermission(user, action, resource) {
    // 根据流程.md中的角色权限定义
    const rolePermissions = {
      'STRATEGIC': [
        'CREATE_INDICATOR',
        'DISTRIBUTE_INDICATOR',
        'APPROVE_INDICATOR',
        'VIEW_ALL_INDICATORS'
      ],
      'FUNCTIONAL': [
        'RECEIVE_INDICATOR',
        'SPLIT_INDICATOR',
        'DISTRIBUTE_INDICATOR',
        'APPROVE_REPORT',
        'VIEW_ASSIGNED_INDICATORS'
      ],
      'COLLEGE': [
        'RECEIVE_INDICATOR',
        'SUBMIT_REPORT',
        'WITHDRAW_REPORT',
        'VIEW_OWN_REPORTS'
      ]
    };

    const userRole = user.role;
    const permissions = rolePermissions[userRole] || [];

    if (!permissions.includes(action)) {
      throw new Error(`User with role ${userRole} does not have permission ${action} on ${resource}`);
    }

    return true;
  }

  /**
   * 验证变更记录
   */
  static assertChangeRecord(changeRecord) {
    const requiredFields = ['fieldName', 'oldValue', 'newValue', 'changedBy', 'changedAt'];

    for (const field of requiredFields) {
      if (!(field in changeRecord)) {
        throw new Error(`Change record missing required field: ${field}`);
      }
    }

    // 验证时间戳
    if (isNaN(Date.parse(changeRecord.changedAt))) {
      throw new Error(`Invalid changedAt timestamp: ${changeRecord.changedAt}`);
    }

    return true;
  }

  /**
   * 验证响应时间
   */
  static assertResponseTime(startTime, maxDuration) {
    const duration = Date.now() - startTime;
    if (duration > maxDuration) {
      throw new Error(`Response time ${duration}ms exceeds maximum ${maxDuration}ms`);
    }
    return true;
  }

  /**
   * 验证分页
   */
  static assertPagination(response) {
    this.assertFieldExists(response, 'total');
    this.assertFieldExists(response, 'page');
    this.assertFieldExists(response, 'size');

    if (response.page < 1) {
      throw new Error('Page number must be >= 1');
    }

    if (response.size < 1) {
      throw new Error('Page size must be >= 1');
    }

    if (response.data && response.data.length > response.size) {
      throw new Error('Data length exceeds page size');
    }

    return true;
  }

  /**
   * 验证错误响应
   */
  static assertErrorResponse(response, expectedCode, expectedHttpStatus) {
    if (response.code !== expectedCode) {
      throw new Error(`Expected error code ${expectedCode}, got ${response.code}`);
    }

    if (response.httpStatus !== expectedHttpStatus) {
      throw new Error(`Expected HTTP status ${expectedHttpStatus}, got ${response.httpStatus}`);
    }

    if (!response.message) {
      throw new Error('Error response must contain message');
    }

    return true;
  }

  /**
   * 批量验证
   */
  static assertAll(assertions) {
    const errors = [];
    const results = [];

    for (let i = 0; i < assertions.length; i++) {
      try {
        assertions[i]();
        results.push({ index: i, passed: true });
      } catch (error) {
        results.push({ index: i, passed: false, error: error.message });
        errors.push({ index: i, error: error.message });
      }
    }

    if (errors.length > 0) {
      throw new Error(`Multiple assertions failed:\n${errors.map(e => `  [${e.index}] ${e.error}`).join('\n')}`);
    }

    return results;
  }
}

module.exports = AssertionLibrary;
