## 🔌 API接口缺口分析

### 已实现的核心接口 (43个)

#### IAM模块 (6个)
- ✅ POST `/auth/login` - 用户登录
- ✅ POST `/auth/refresh` - Token刷新
- ✅ POST `/auth/logout` - 用户登出
- ✅ GET `/users/profile` - 获取用户信息
- ✅ GET `/users` - 获取用户列表
- ✅ GET `/roles` - 获取角色列表

#### 战略管理模块 (6个)
- ✅ POST `/indicators` - 创建指标
- ✅ PUT `/indicators/{id}` - 更新指标
- ✅ GET `/indicators/{id}` - 查询指标详情
- ✅ GET `/indicators` - 查询指标列表
- ✅ POST `/indicators/{id}/distribute` - 下发指标
- ✅ POST `/cycles` - 创建考核周期

#### 执行管理模块 (7个)
- ✅ POST `/reports` - 创建填报
- ✅ PUT `/reports/{id}` - 更新填报
- ✅ POST `/reports/{id}/submit` - 提交审批
- ✅ POST `/reports/{id}/withdraw` - 撤回填报
- ✅ GET `/reports` - 查询填报列表
- ✅ GET `/indicators/{id}/reports` - 查询指标历史填报
- ✅ GET `/reports?cycle={id}` - 按周期查询填报

#### 工作流模块 (7个)
- ✅ GET `/workflow/definitions` - 查询工作流定义
- ✅ GET `/workflow/instances` - 查询工作流实例
- ✅ GET `/workflow/instances/{id}` - 获取实例详情
- ✅ GET `/workflow/instances/{id}/timeline` - 查询审批时间轴
- ✅ GET `/workflow/instances/{id}/history` - 查询审批历史
- ✅ POST `/workflow/instances/{id}/approve` - 审批通过
- ✅ POST `/workflow/instances/{id}/reject` - 审批驳回

---

### ❌ 缺失的关键接口 (12个)

#### 1. 批量操作接口 (优先级: P1)

**业务场景**: 职能部门需要将1个父指标拆分为10个子指标并批量下发

```
❌ POST /indicators/batch
   请求: { indicators: [{ name, target, weight, assignedOrgId }] }
   响应: { createdIds: [...] }

❌ POST /indicators/batch-distribute
   请求: { indicatorIds: [...], targetOrgIds: [...] }
   响应: { workflowInstanceIds: [...] }

❌ POST /workflow/instances/batch-approve
   请求: { instanceIds: [...], comment: "..." }
   响应: { successCount, failedIds: [...] }
```

**影响**: 用户需要重复操作10次,效率低下

---

#### 2. 数据导入导出接口 (优先级: P1)

**业务场景**: 学院需要批量导入历史数据,战略部门需要导出汇总报表

```
❌ POST /indicators/import
   请求: multipart/form-data (Excel文件)
   响应: { successCount, failedRows: [...] }

❌ POST /reports/import
   请求: multipart/form-data (Excel文件)
   响应: { successCount, failedRows: [...] }

❌ GET /analytics/export?format=excel&type=indicators&cycleId={id}
   响应: application/vnd.openxmlformats-officedocument.spreadsheetml.sheet

❌ GET /analytics/export?format=pdf&type=summary&cycleId={id}
   响应: application/pdf
```

**影响**: 无法处理大批量数据,影响系统推广

---

#### 3. 消息通知接口 (优先级: P1)

**业务场景**: 指标下发后,接收方需要及时收到通知

```
❌ GET /notifications
   响应: { items: [{ id, type, title, content, read, createdAt }] }

❌ POST /notifications/{id}/read
   响应: { success: true }

❌ GET /notifications/unread-count
   响应: { count: 5 }

❌ GET /todos
   响应: { items: [{ id, type, title, deadline, priority }] }
```

**影响**: 用户无法及时处理任务,流程效率低

---

#### 4. 数据变更对比接口 (优先级: P2)

**业务场景**: 审批流中,若数据被上级修改,下级需要看到变更对比

```
❌ GET /workflow/instances/{id}/changes
   响应: { 
     changes: [
       { field, oldValue, newValue, changedBy, changedAt }
     ]
   }

❌ GET /reports/{id}/versions
   响应: {
     versions: [
       { version, content, submittedAt, approvedAt }
     ]
   }
```

**影响**: 用户不知道数据被修改,缺乏知情权

---

## 📋 业务需求缺口分析

### 核心UX功能实现状态

#### ✅ 已实现 (2/6)

1. **审批时间轴** - 部分实现
   - ✅ API: GET `/workflow/instances/{id}/timeline`
   - ✅ 数据支持: `audit_step_instance` 表
   - ⚠️ 需验证: 前端是否已实现可视化时间轴

2. **审批历史记录** - 已实现
   - ✅ API: GET `/workflow/instances/{id}/history`
   - ✅ 数据支持: `audit_step_instance` 表

---

#### ❌ 缺失或待验证 (4/6)

3. **强制驳回意见** (优先级: P0)
   - ⚠️ 数据字段: `audit_step_instance.comment` 存在
   - ❌ 后端校验: 需验证驳回时是否强制要求填写comment
   - ❌ 前端校验: 需验证前端是否禁止空comment提交
   
   **验证方法**:
   ```bash
   # 测试驳回时不填写comment
   curl -X POST http://localhost:8080/api/v1/workflow/instances/{id}/reject \
     -H "Authorization: Bearer {token}" \
     -d '{"comment":""}'
   
   # 预期: 应返回400错误,提示"驳回意见不能为空"
   ```

4. **填报版本控制** (优先级: P1)
   - ❌ 数据字段: `plan_report.version` 不存在
   - ❌ API接口: GET `/reports/{id}/versions` 不存在
   - ⚠️ 当前方案: 通过 `audit_step_instance` 日志追溯
   
   **业务影响**: 用户无法直观查看历史版本对比

5. **周期标识展示** (优先级: P0)
   - ⚠️ 数据字段: 需验证 `cycle_id` 是否存在于所有业务表
   - ❌ API响应: 需验证VO中是否包含"当前周期"、"上次归档时间"
   
   **验证方法**:
   ```bash
   # 查询指标详情
   curl http://localhost:8080/api/v1/indicators/{id}
   
   # 预期响应应包含:
   # {
   #   "cycleId": "2026-Q1",
   #   "cycleName": "2026年第一季度",
   #   "lastArchivedAt": "2026-03-01T10:00:00",
   #   "lastArchivedValue": 85.5
   # }
   ```

6. **数据变更提醒** (优先级: P1)
   - ❌ 数据字段: `audit_step_instance.change_log` 不存在
   - ❌ API接口: GET `/workflow/instances/{id}/changes` 不存在
   - ❌ 消息推送: 无通知机制
   
   **业务影响**: 用户不知道数据被修改,缺乏知情权

---

