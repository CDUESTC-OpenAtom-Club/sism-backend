# strategic 数据库表与字段导出

- 导出时间: 2026-03-19
- 表数量: 34
- 字段数量: 341
- 备注来源: PostgreSQL `pg_description`（表备注 + 字段备注）

## public.adhoc_task

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | adhoc_task_id | bigint | NO | nextval('adhoc_task_adhoc_task_id_seq'::regclass) |  |
| 2 | created_at | timestamp(6) without time zone | NO |  |  |
| 3 | updated_at | timestamp(6) without time zone | NO |  |  |
| 4 | due_at | date | YES |  |  |
| 5 | include_in_alert | boolean | NO |  |  |
| 6 | open_at | date | YES |  |  |
| 7 | require_indicator_report | boolean | NO |  |  |
| 8 | scope_type | character varying(255) | NO |  |  |
| 9 | status | character varying(255) | NO |  |  |
| 10 | task_desc | text | YES |  |  |
| 11 | task_title | character varying(200) | NO |  |  |
| 12 | creator_org_id | bigint | NO |  |  |
| 13 | cycle_id | bigint | NO |  |  |
| 14 | indicator_id | bigint | YES |  |  |

## public.adhoc_task_indicator_map

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | adhoc_task_id | bigint | NO |  |  |
| 2 | indicator_id | bigint | NO |  |  |

## public.adhoc_task_target

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | adhoc_task_id | bigint | NO |  |  |
| 2 | target_org_id | bigint | NO |  |  |

## public.alert_event

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | event_id | bigint | NO | nextval('alert_event_event_id_seq'::regclass) |  |
| 2 | created_at | timestamp(6) without time zone | NO |  |  |
| 3 | updated_at | timestamp(6) without time zone | NO |  |  |
| 4 | actual_percent | numeric(5,2) | NO |  |  |
| 5 | detail_json | jsonb | YES |  |  |
| 6 | expected_percent | numeric(5,2) | NO |  |  |
| 7 | gap_percent | numeric(5,2) | NO |  |  |
| 8 | handled_note | text | YES |  |  |
| 9 | severity | character varying(255) | NO |  |  |
| 10 | status | character varying(255) | NO |  |  |
| 11 | handled_by | bigint | YES |  |  |
| 12 | indicator_id | bigint | NO |  |  |
| 13 | rule_id | bigint | NO |  |  |
| 14 | window_id | bigint | NO |  |  |

## public.alert_rule

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | rule_id | bigint | NO | nextval('alert_rule_rule_id_seq'::regclass) |  |
| 2 | created_at | timestamp(6) without time zone | NO |  |  |
| 3 | updated_at | timestamp(6) without time zone | NO |  |  |
| 4 | gap_threshold | numeric(5,2) | NO |  |  |
| 5 | is_enabled | boolean | NO |  |  |
| 6 | name | character varying(100) | NO |  |  |
| 7 | severity | character varying(255) | NO |  |  |
| 8 | cycle_id | bigint | NO |  |  |

## public.alert_window

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | window_id | bigint | NO | nextval('alert_window_window_id_seq'::regclass) |  |
| 2 | created_at | timestamp(6) without time zone | NO |  |  |
| 3 | updated_at | timestamp(6) without time zone | NO |  |  |
| 4 | cutoff_date | date | NO |  |  |
| 5 | is_default | boolean | NO |  |  |
| 6 | name | character varying(100) | NO |  |  |
| 7 | cycle_id | bigint | NO |  |  |

## public.attachment

表备注: INSERT INTO attachment(storage_driver, bucket, object_key, original_name, content_type, file_ext, size_bytes, sha256, uploaded_by) VALUES ('FILE', NULL, '2026/02/03/xxx.pdf', '验收材料.pdf', 'application/pdf', 'pdf', 123456, '...sha256...', 1001)

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | id | bigint | NO | nextval('attachment_id_seq'::regclass) |  |
| 2 | storage_driver | character varying(16) | NO | 'FILE'::character varying | FILE/S3/OSS/COS/MINIO |
| 3 | bucket | character varying(128) | YES |  | 对象存储桶；本地可为空 |
| 4 | object_key | text | NO |  | 核心：在存储层的唯一Key/相对路径 |
| 5 | public_url | text | YES |  | 可选：公开访问URL（如果是公有桶/静态服务器） |
| 6 | original_name | text | NO |  | 文件名 |
| 7 | content_type | character varying(128) | YES |  | 文件类型 |
| 8 | file_ext | character varying(16) | YES |  | 后缀 |
| 9 | size_bytes | bigint | NO |  |  |
| 10 | sha256 | character(64) | YES |  | 校验用 |
| 11 | etag | text | YES |  | 验证用，暂时为空 |
| 12 | uploaded_by | bigint | NO |  |  |
| 13 | uploaded_at | timestamp with time zone | NO | now() |  |
| 14 | remark | text | YES |  |  |
| 15 | is_deleted | boolean | NO | false |  |
| 16 | deleted_at | timestamp with time zone | YES |  |  |

## public.audit_flow_def

表备注: 审批流程定义表。当前导出结果中 `entity_type` 仍存在，`biz_type` 已删除。

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | id | bigint | NO | nextval('audit_flow_def_id_seq'::regclass) |  |
| 2 | flow_code | character varying(64) | NO |  |  |
| 3 | flow_name | character varying(128) | NO |  |  |
| 6 | is_enabled | boolean | NO | true |  |
| 8 | created_at | timestamp with time zone | NO | now() |  |
| 9 | updated_at | timestamp with time zone | NO | now() |  |
| 13 | description | character varying(255) | YES |  |  |
| 14 | version | integer | YES |  |  |
| 17 | flow_def_id | bigint | YES |  |  |
| 18 | entity_type | character varying(64) | NO | 'PLAN_REPORT'::character varying | 实体类型: INDICATOR, PLAN_REPORT 等 |

## public.audit_instance

表备注: Audit workflow instance. Approval context is resolved dynamically via ApprovalResolverService, not stored as snapshot columns.

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | id | bigint | NO | nextval('audit_instance_id_seq'::regclass) |  |
| 6 | status | character varying(32) | NO |  | 填写节点：DRAFT /APPROVED  审核节点：IN_REVIEW / APPROVED / REJECTED |
| 7 | started_at | timestamp with time zone | YES |  | 启动时间 |
| 10 | created_at | timestamp with time zone | NO | now() |  |
| 11 | updated_at | timestamp with time zone | NO | now() |  |
| 24 | completed_at | timestamp(6) without time zone | YES |  |  |
| 26 | entity_id | bigint | NO |  |  |
| 27 | entity_type | character varying(255) | YES |  |  |
| 28 | flow_def_id | bigint | YES |  |  |
| 29 | is_deleted | boolean | NO |  |  |
| 30 | requester_id | bigint | YES |  |  |
| 31 | requester_org_id | bigint | YES |  |  |

## public.audit_log

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | log_id | bigint | NO | nextval('audit_log_log_id_seq'::regclass) |  |
| 2 | action | character varying(255) | NO |  |  |
| 3 | after_json | jsonb | YES |  |  |
| 4 | before_json | jsonb | YES |  |  |
| 5 | changed_fields | jsonb | YES |  |  |
| 6 | created_at | timestamp(6) without time zone | NO |  |  |
| 7 | entity_id | bigint | NO |  |  |
| 8 | entity_type | character varying(255) | NO |  |  |
| 9 | reason | text | YES |  |  |
| 10 | actor_org_id | bigint | YES |  |  |
| 11 | actor_user_id | bigint | YES |  |  |

## public.audit_step_def

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | id | bigint | NO | nextval('audit_step_def_id_seq'::regclass) |  |
| 2 | flow_id | bigint | NO |  |  |
| 5 | step_name | character varying(128) | NO |  | 审核节点名称 |
| 16 | step_type | character varying(255) | YES |  |  |
| 20 | role_id | bigint | YES |  |  |
| 21 | is_terminal | boolean | YES | false |  |
| 22 | created_at | timestamp without time zone | YES |  |  |
| 23 | updated_at | timestamp without time zone | YES |  |  |
| 24 | step_no | integer | YES |  |  |

## public.audit_step_instance

表备注: 流程节点实例表：记录某个流程实例在某个节点上的实际处理情况

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | id | bigint | NO | nextval('audit_step_instance_id_seq'::regclass) | 主键 |
| 2 | instance_id | bigint | YES |  | 所属流程实例ID，关联 audit_instance.id |
| 6 | step_name | character varying(128) | NO |  | 节点名称（冗余保存，便于展示） |
| 12 | approved_at | timestamp(6) without time zone | YES |  |  |
| 13 | approver_id | bigint | YES |  |  |
| 15 | comment | character varying(255) | YES |  |  |
| 17 | status | character varying(255) | YES |  |  |
| 18 | step_index | integer | YES |  |  |
| 19 | step_def_id | bigint | YES |  |  |
| 20 | approver_org_id | bigint | YES |  |  |
| 31 | created_at | timestamp without time zone | YES |  |  |

### 审核流程最小关系

为了先把流程接口跑通，可以把这 5 张表理解成一条固定模板驱动的线性链路：

1. `audit_flow_def`
   - 流程模板头
   - 一条记录代表一类审批流程，例如 `PLAN_DISPATCH_STRATEGY`
2. `audit_step_def`
   - 流程模板步骤
   - 通过 `flow_id -> audit_flow_def.id` 归属于某个模板
   - 用 `step_no` 表示模板内顺序
3. `audit_instance`
   - 某个业务对象的一次实际审批实例
   - 通过 `flow_def_id -> audit_flow_def.id` 说明“这次实例使用了哪条模板”
   - 通过 `entity_type + entity_id` 说明“这次审批针对哪个业务对象”
4. `audit_step_instance`
   - 某个实例下的运行时节点
   - 通过 `instance_id -> audit_instance.id` 归属于一次实例
   - 通过 `step_def_id -> audit_step_def.id` 对应模板中的原始节点
5. `audit_log`
   - 审计留痕表
   - 通过 `entity_type + entity_id` 记录业务动作，不承担主链路推进职责

### 表关系图

```text
audit_flow_def (1)
  ├─< audit_step_def.flow_id
  └─< audit_instance.flow_def_id
          └─< audit_step_instance.instance_id
                    └─ audit_step_instance.step_def_id >─ audit_step_def.id

audit_log
  └─ 按 entity_type + entity_id 记录动作日志
```

### 最小跑通建议

如果这轮目标只是“流程接口成功跑通”，建议先固定为下面这套最小规则：

- `audit_flow_def` 只维护一个启用中的模板
- `audit_step_def` 固定 3 步：
  - `step_no = 1`，`step_type = SUBMIT`
  - `step_no = 2`，`step_type = APPROVAL`
  - `step_no = 3`，`step_type = APPROVAL`
- 启动流程时创建一条 `audit_instance`
- 同时按模板生成多条 `audit_step_instance`
- 第 1 步 `SUBMIT` 自动完成
- 当前待审批节点只保留一条 `audit_step_instance.status = PENDING`
- 审批通过时推进到下一条节点实例
- 最后一个节点通过后把 `audit_instance.status` 更新为 `APPROVED`

## public.cycle

表备注: 考核周期表

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | id | bigint | NO | nextval('cycle_id_seq'::regclass) |  |
| 2 | cycle_name | character varying(100) | NO |  |  |
| 3 | year | integer | NO |  |  |
| 4 | start_date | date | NO |  |  |
| 5 | end_date | date | NO |  |  |
| 6 | description | text | YES |  |  |
| 7 | created_at | timestamp(6) without time zone | NO | CURRENT_TIMESTAMP |  |
| 8 | updated_at | timestamp(6) without time zone | NO | CURRENT_TIMESTAMP |  |

## public.flyway_schema_history

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | installed_rank | integer | NO |  |  |
| 2 | version | character varying(50) | YES |  |  |
| 3 | description | character varying(200) | NO |  |  |
| 4 | type | character varying(20) | NO |  |  |
| 5 | script | character varying(1000) | NO |  |  |
| 6 | checksum | integer | YES |  |  |
| 7 | installed_by | character varying(100) | NO |  |  |
| 8 | installed_on | timestamp without time zone | NO | now() |  |
| 9 | execution_time | integer | NO |  |  |
| 10 | success | boolean | NO |  |  |

## public.idempotency_records

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | id | bigint | NO | nextval('idempotency_records_id_seq'::regclass) |  |
| 2 | created_at | timestamp(6) without time zone | NO |  |  |
| 3 | expires_at | timestamp(6) without time zone | NO |  |  |
| 4 | http_method | character varying(10) | YES |  |  |
| 5 | idempotency_key | character varying(64) | NO |  |  |
| 6 | request_path | character varying(255) | YES |  |  |
| 7 | response_body | text | YES |  |  |
| 8 | status | character varying(20) | YES |  |  |
| 9 | status_code | integer | YES |  |  |

## public.indicator

表备注: 指标表 - 支持自引用分层

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | id | bigint | NO | nextval('indicator_id_seq'::regclass) |  |
| 2 | task_id | bigint | YES |  | 为空的话，即是level2的指标 |
| 3 | parent_indicator_id | bigint | YES |  | 只有level2有这个数据 |
| 7 | indicator_desc | text | NO |  | 指标描述，具体的指标值也在描述里 |
| 8 | weight_percent | numeric(38,2) | NO | 0 | 权重 |
| 9 | sort_order | integer | NO | 0 |  |
| 12 | remark | text | YES |  |  |
| 13 | created_at | timestamp(6) without time zone | NO | CURRENT_TIMESTAMP |  |
| 14 | updated_at | timestamp(6) without time zone | NO | CURRENT_TIMESTAMP |  |
| 16 | type | character varying(20) | NO | '定量'::character varying | 指标类型: 定量/定性 |
| 23 | progress | integer | YES | 0 | 最新进度 |
| 30 | is_deleted | boolean | NO | false |  |
| 48 | owner_org_id | bigint | YES |  | Owner organization (functional department) |
| 49 | target_org_id | bigint | YES |  | Target organization (functional department or college) |
| 51 | status | character varying(20) | YES |  | 指标状态: DRAFT=草稿, PENDING=待审批, DISTRIBUTED=已下发（其他状态在审批流程中管理） |
| 56 | responsible_user_id | bigint | YES |  |  |
| 57 | is_enabled | boolean | YES | true |  |

## public.indicator_milestone

表备注: 指标里程碑关联表 - V26: 删除了 inherited_from 字段，通过 indicator_id 关联实现相同功能

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | id | bigint | NO | nextval('indicator_milestone_id_seq'::regclass) |  |
| 2 | indicator_id | bigint | NO |  |  |
| 3 | milestone_name | character varying(200) | NO |  |  |
| 4 | milestone_desc | text | YES |  |  |
| 5 | due_date | date | NO |  |  |
| 7 | status | character varying | NO | 'NOT_STARTED'::milestone_status |  |
| 8 | sort_order | integer | NO | 0 |  |
| 10 | created_at | timestamp(6) without time zone | NO | CURRENT_TIMESTAMP |  |
| 11 | updated_at | timestamp(6) without time zone | NO | CURRENT_TIMESTAMP |  |
| 12 | target_progress | integer | YES | 0 |  |
| 13 | is_paired | boolean | YES | false |  |

## public.plan

表备注: 用于统一管理一批同时下发的指标。 战略发展部给职能部门发布的任务集合对应一个plan。 职能部门给二级学院发布的指标集合对应一个plan。

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | id | bigint | NO | nextval('plan_id_seq'::regclass) |  |
| 2 | cycle_id | bigint | NO |  | 周期 |
| 5 | created_at | timestamp without time zone | NO |  |  |
| 6 | updated_at | timestamp without time zone | NO |  |  |
| 7 | is_deleted | boolean | NO | false |  |
| 8 | target_org_id | bigint | NO |  | 接受计划组织 |
| 9 | created_by_org_id | bigint | NO |  | 创建组织 |
| 10 | plan_level | plan_level | NO |  | 指标层级: STRAT_TO_FUNC-战略到职能, FUNC_TO_COLLEGE-职能到学院 |
| 11 | status | character varying | NO | 'DRAFT'::character varying | 与下发流程状态一致 |

## public.plan_report

表备注: plan提交记录。提交时如果目标月份已有记录，则在其基础上修改，否则新增目标月份的记录。

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | id | bigint | NO | nextval('plan_report_id_seq'::regclass) |  |
| 2 | plan_id | bigint | NO |  |  |
| 3 | report_month | character varying(255) | NO |  |  |
| 5 | report_org_type | character varying(16) | NO |  |  |
| 6 | report_org_id | bigint | NO |  |  |
| 7 | status | character varying(16) | NO | 'DRAFT'::character varying | 应该是流程中的最新状态 |
| 8 | submitted_at | timestamp with time zone | YES |  |  |
| 9 | remark | text | YES |  |  |
| 10 | created_at | timestamp with time zone | NO | CURRENT_TIMESTAMP |  |
| 11 | updated_at | timestamp with time zone | NO | CURRENT_TIMESTAMP |  |
| 12 | is_deleted | boolean | NO | false |  |
| 24 | created_by | bigint | YES |  |  |

## public.plan_report_indicator

表备注: 填报记录表（包括历史）

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | id | bigint | NO | nextval('plan_report_indicator_id_seq'::regclass) |  |
| 2 | report_id | bigint | NO |  |  |
| 3 | indicator_id | bigint | NO |  |  |
| 6 | progress | integer | NO | 0 |  |
| 7 | milestone_note | text | YES |  | 用于记录对应里程碑的情况，可以为空 |
| 8 | comment | text | YES |  |  |
| 9 | created_at | timestamp with time zone | NO | CURRENT_TIMESTAMP |  |

## public.plan_report_indicator_attachment

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | id | bigint | NO | nextval('plan_report_indicator_attachment_id_seq'::regclass) |  |
| 2 | plan_report_indicator_id | bigint | NO |  |  |
| 3 | attachment_id | bigint | NO |  |  |
| 5 | sort_order | integer | NO | 0 |  |
| 6 | created_by | bigint | NO |  |  |
| 7 | created_at | timestamp with time zone | NO | now() |  |

## public.progress_report

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | report_id | bigint | NO | nextval('progress_report_report_id_seq'::regclass) |  |
| 2 | created_at | timestamp(6) without time zone | NO |  |  |
| 3 | updated_at | timestamp(6) without time zone | NO |  |  |
| 4 | achieved_milestone | boolean | NO |  |  |
| 5 | is_final | boolean | NO |  |  |
| 6 | narrative | text | YES |  |  |
| 7 | percent_complete | numeric(5,2) | NO |  |  |
| 8 | reported_at | timestamp(6) without time zone | YES |  |  |
| 9 | status | character varying(255) | NO |  |  |
| 10 | version_no | integer | NO |  |  |
| 11 | adhoc_task_id | bigint | YES |  |  |
| 12 | indicator_id | bigint | NO |  |  |
| 13 | milestone_id | bigint | YES |  |  |
| 14 | reporter_id | bigint | NO |  |  |

## public.refresh_tokens

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | id | bigint | NO | nextval('refresh_tokens_id_seq'::regclass) |  |
| 2 | created_at | timestamp(6) without time zone | NO |  |  |
| 3 | device_info | character varying(255) | YES |  |  |
| 4 | expires_at | timestamp(6) without time zone | NO |  |  |
| 5 | ip_address | character varying(45) | YES |  |  |
| 6 | revoked_at | timestamp(6) without time zone | YES |  |  |
| 7 | token_hash | character varying(64) | NO |  |  |
| 8 | user_id | bigint | NO |  |  |

## public.sys_org

表备注: 组织表 - 战略发展部/职能部门/二级学院/系部

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | id | bigint | NO | nextval('org_id_seq'::regclass) |  |
| 2 | name | character varying(100) | NO |  |  |
| 3 | type | org_type | NO |  | 组织类型: admin(系统管理层), functional(职能部门), academic(二级学院) |
| 5 | is_active | boolean | NO | true |  |
| 6 | sort_order | integer | NO | 0 |  |
| 7 | created_at | timestamp(6) without time zone | NO | CURRENT_TIMESTAMP |  |
| 8 | updated_at | timestamp(6) without time zone | NO | CURRENT_TIMESTAMP |  |
| 9 | parent_org_id | bigint | YES |  | 父组织ID，用于构建组织层级关系 |
| 10 | level | integer | YES |  | 组织层级，1=顶级组织，2=二级组织，以此类推 |
| 12 | is_deleted | boolean | NO | false |  |

## public.sys_permission

表备注: 权限资源表：统一定义页面（PAGE）与按钮（BUTTON），按钮通过parent_id归属到页面

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | id | bigint | NO | nextval('sys_permission_id_seq'::regclass) | 主键 |
| 2 | perm_code | character varying(128) | NO |  | 权限编码（唯一，建议前端/后端统一使用） |
| 3 | perm_name | character varying(128) | NO |  | 权限名称（中文展示） |
| 4 | perm_type | character varying(16) | NO |  | 权限类型：PAGE=页面，BUTTON=按钮 |
| 5 | parent_id | bigint | YES |  | 父权限ID：按钮指向其所属页面；页面为空 |
| 6 | route_path | character varying(256) | YES |  | 页面路由路径（仅PAGE使用） |
| 7 | page_key | character varying(128) | YES |  | 页面标识（可用于前端定位、埋点、菜单渲染） |
| 8 | action_key | character varying(128) | YES |  | 按钮动作标识（仅BUTTON使用，如 submit/approve/export） |
| 9 | sort_order | integer | NO | 0 | 排序号 |
| 10 | is_enabled | boolean | NO | true | 是否启用 |
| 11 | remark | text | YES |  | 备注说明 |
| 12 | created_at | timestamp with time zone | NO | CURRENT_TIMESTAMP | 创建时间 |
| 13 | updated_at | timestamp with time zone | NO | CURRENT_TIMESTAMP | 更新时间 |

## public.sys_role

表备注: 角色表：RBAC角色定义（决定页面/按钮权限）；data_access_mode用于区分战略部全量访问与普通组织仅本组织

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | id | bigint | NO | nextval('sys_role_id_seq'::regclass) | 主键 |
| 2 | role_code | character varying(64) | NO |  | 角色编码（唯一） |
| 3 | role_name | character varying(128) | NO |  | 角色名称 |
| 4 | data_access_mode | character varying(16) | NO | 'OWN_ORG'::character varying | 数据访问模式：ALL=全量可见（如战略发展部），OWN_ORG=仅本组织数据（一期可不启用） |
| 5 | is_enabled | boolean | NO | true | 是否启用 |
| 6 | remark | text | YES |  | 备注说明 |
| 7 | created_at | timestamp with time zone | NO | CURRENT_TIMESTAMP | 创建时间 |
| 8 | updated_at | timestamp with time zone | NO | CURRENT_TIMESTAMP | 更新时间 |

## public.sys_role_permission

表备注: 角色权限关联表：给角色授予页面/按钮权限

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | id | bigint | NO | nextval('sys_role_permission_id_seq'::regclass) | 主键 |
| 2 | role_id | bigint | NO |  | 角色ID |
| 3 | perm_id | bigint | NO |  | 权限资源ID（页面或按钮） |
| 4 | created_at | timestamp with time zone | NO | CURRENT_TIMESTAMP | 创建时间 |

## public.sys_task

表备注: Strategic task table (migrated from task on 2026-02-10)

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | task_id | bigint | NO | nextval('strategic_task_task_id_seq'::regclass) |  |
| 2 | created_at | timestamp(6) without time zone | NO |  |  |
| 3 | updated_at | timestamp(6) without time zone | NO |  |  |
| 4 | remark | text | YES |  |  |
| 5 | sort_order | integer | NO |  |  |
| 6 | task_desc | text | YES |  |  |
| 7 | task_name | character varying(200) | NO |  |  |
| 8 | task_type | character varying(255) | NO |  |  |
| 9 | created_by_org_id | bigint | NO |  |  |
| 10 | cycle_id | bigint | NO |  | 考核周期 |
| 11 | org_id | bigint | NO |  |  |
| 12 | is_deleted | boolean | NO |  |  |
| 13 | plan_id | bigint | NO |  |  |
| 14 | name | character varying(255) | NO |  | 冗余？ |
| 16 | desc | character varying(255) | YES |  |  |

## public.sys_user

表备注: System user table (renamed from app_user on 2026-02-10)

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | id | bigint | NO | nextval('sys_user_user_id_seq'::regclass) |  |
| 2 | created_at | timestamp(6) without time zone | NO |  |  |
| 3 | updated_at | timestamp(6) without time zone | NO |  |  |
| 4 | is_active | boolean | NO |  |  |
| 5 | password_hash | character varying(255) | NO |  |  |
| 6 | real_name | character varying(50) | NO |  |  |
| 7 | sso_id | character varying(100) | YES |  |  |
| 8 | username | character varying(50) | NO |  |  |
| 9 | org_id | bigint | NO |  |  |

## public.sys_user_role

表备注: 用户角色关联表：用户可拥有多个角色，角色也可分配给多个用户

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | id | bigint | NO | nextval('sys_user_role_id_seq'::regclass) | 主键 |
| 2 | user_id | bigint | NO |  | 用户ID |
| 3 | role_id | bigint | NO |  | 角色ID |
| 4 | created_at | timestamp with time zone | NO | CURRENT_TIMESTAMP | 创建时间 |

## public.sys_user_supervisor

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | user_id | bigint | NO |  |  |
| 2 | supervisor_id | bigint | NO |  |  |
| 3 | level | integer | NO |  |  |
| 4 | created_at | timestamp without time zone | YES | CURRENT_TIMESTAMP |  |

## public.warn_level

表备注: 预警等级字典表：定义系统统一使用的预警等级（如正常/预警/严重/危急），用于实时预警与历史预警事件

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | id | bigint | NO | nextval('warn_level_id_seq'::regclass) | 主键 |
| 2 | level_code | character varying(32) | NO |  | 预警等级编码（OK / INFO / WARN / MAJOR / CRITICAL） |
| 3 | level_name | character varying(64) | NO |  | 预警等级名称（中文展示用） |
| 4 | severity | integer | NO |  | 严重程度数值，数值越大表示越严重，用于排序和统计 |
| 5 | remark | text | YES |  | 备注说明 |

## public.workflow_task

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | task_id | bigint | NO | nextval('workflow_task_task_id_seq'::regclass) |  |
| 2 | assignee_id | bigint | YES |  |  |
| 3 | assignee_org_id | bigint | YES |  |  |
| 4 | completed_at | timestamp(6) without time zone | YES |  |  |
| 5 | current_step | character varying(255) | YES |  |  |
| 6 | due_date | timestamp(6) without time zone | YES |  |  |
| 7 | error_message | text | YES |  |  |
| 8 | initiator_id | bigint | YES |  |  |
| 9 | initiator_org_id | bigint | YES |  |  |
| 10 | next_step | character varying(255) | YES |  |  |
| 11 | result | text | YES |  |  |
| 12 | started_at | timestamp(6) without time zone | YES |  |  |
| 13 | status | character varying(255) | NO |  |  |
| 14 | task_name | character varying(255) | NO |  |  |
| 15 | task_type | character varying(255) | YES |  |  |
| 16 | workflow_id | character varying(255) | NO |  |  |
| 17 | workflow_type | character varying(255) | NO |  |  |

## public.workflow_task_history

| 序号 | 字段名 | 数据类型 | 可空 | 默认值 | 字段备注 |
| --- | --- | --- | --- | --- | --- |
| 1 | task_id | bigint | NO |  |  |
| 2 | history | character varying(255) | YES |  |  |
