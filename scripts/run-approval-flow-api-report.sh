#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"
API_BASE="${BASE_URL%/}/api/v1"
DB_NAME="${DB_NAME:-sism_db}"
PASSWORD="${PASSWORD:-admin123}"
ROOT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
REPORT_DIR="${ROOT_DIR}/logs/approval-flow-reports"
TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
REPORT_FILE="${REPORT_DIR}/approval-flow-api-report-${TIMESTAMP}.md"
TMP_DIR="$(mktemp -d)"
trap 'rm -rf "${TMP_DIR}"' EXIT

mkdir -p "${REPORT_DIR}"

log() {
  printf '[approval-flow-report] %s\n' "$*" >&2
}

db_markdown() {
  local sql="$1"
  psql "${DB_NAME}" -P border=2 -P null='NULL' -c "${sql}"
}

db_value() {
  local sql="$1"
  psql "${DB_NAME}" -At -c "${sql}"
}

json_to_block() {
  jq .
}

login() {
  local username="$1"
  local response
  response="$(curl -sS -X POST "${API_BASE}/auth/login" \
    -H 'Content-Type: application/json' \
    -d "{\"username\":\"${username}\",\"password\":\"${PASSWORD}\"}")"
  local token
  token="$(printf '%s' "${response}" | jq -r '.data.token // empty')"
  if [[ -z "${token}" ]]; then
    log "login failed for ${username}: ${response}"
    return 1
  fi
  printf '%s' "${token}"
}

api_call() {
  local method="$1"
  local url="$2"
  local token="${3:-}"
  local body="${4:-}"
  local out_file="$5"
  local code

  if [[ -n "${body}" ]]; then
    code="$(curl -sS -o "${out_file}" -w '%{http_code}' -X "${method}" "${url}" \
      -H 'Content-Type: application/json' \
      ${token:+-H "Authorization: Bearer ${token}"} \
      -d "${body}")"
  else
    code="$(curl -sS -o "${out_file}" -w '%{http_code}' -X "${method}" "${url}" \
      ${token:+-H "Authorization: Bearer ${token}"})"
  fi

  if [[ "${code}" != "200" ]]; then
    log "API call failed: ${method} ${url} -> ${code}"
    cat "${out_file}" >&2
    return 1
  fi
}

seed_supporting_data() {
  log "seeding supporting task/indicator data for plan 7055 and 7057"
  psql "${DB_NAME}" <<'SQL'
INSERT INTO public.sys_task (
    task_id, created_at, updated_at, remark, sort_order, task_type,
    created_by_org_id, cycle_id, org_id, is_deleted, plan_id, name, "desc"
)
VALUES
    (41021, NOW(), NOW(), 'API审批验证补充任务', 1, 'BASIC', 44, 7, 55, false, 7055, '马院课程思政提升任务', '用于PLAN_DISPATCH_FUNCDEPT流程API验证'),
    (41022, NOW(), NOW(), 'API审批验证补充任务', 1, 'BASIC', 44, 7, 57, false, 7057, '计算机学院实践教学提升任务', '用于PLAN_APPROVAL_COLLEGE流程API验证')
ON CONFLICT (task_id) DO NOTHING;

INSERT INTO public.indicator (
    id, task_id, parent_indicator_id, indicator_desc, weight_percent, sort_order, remark,
    created_at, updated_at, type, progress, is_deleted, owner_org_id, target_org_id,
    status, responsible_user_id, is_enabled
)
VALUES
    (2039, 41021, NULL, '完成马院课程思政案例库建设', 60.00, 1, 'API审批验证补充指标', NOW(), NOW(), '定量', 0, false, 44, 55, 'DRAFT', 267, true),
    (2040, 41021, NULL, '完成马院课程思政培训覆盖', 40.00, 2, 'API审批验证补充指标', NOW(), NOW(), '定量', 0, false, 44, 55, 'DRAFT', 267, true),
    (2041, 41022, NULL, '完成计算机学院实践平台升级', 50.00, 1, 'API审批验证补充指标', NOW(), NOW(), '定量', 0, false, 44, 57, 'DRAFT', 370, true),
    (2042, 41022, NULL, '完成计算机学院企业项目引入', 50.00, 2, 'API审批验证补充指标', NOW(), NOW(), '定性', 0, false, 44, 57, 'DRAFT', 370, true)
ON CONFLICT (id) DO NOTHING;
SQL
}

wait_for_instance() {
  local token="$1"
  local entity_type="$2"
  local entity_id="$3"
  local out_file="$4"
  local attempt

  for attempt in $(seq 1 20); do
    local code
    code="$(curl -sS -o "${out_file}" -w '%{http_code}' -X GET \
      "${API_BASE}/workflows/instances/entity/${entity_type}/${entity_id}" \
      -H "Authorization: Bearer ${token}")"
    if [[ "${code}" == "200" && "$(jq -r '.data.instanceId // empty' "${out_file}")" != "" ]]; then
      return 0
    fi
    sleep 1
  done

  log "timed out waiting for instance entityType=${entity_type} entityId=${entity_id}"
  return 1
}

append_markdown() {
  cat >> "${REPORT_FILE}"
}

capture_snapshot() {
  local title="$1"
  local plan_id="$2"
  local instance_id="$3"
  append_markdown <<EOF
### ${title}

\`\`\`sql
$(db_markdown "SELECT id, status, audit_instance_id, target_org_id, created_by_org_id, plan_level, updated_at FROM plan WHERE id = ${plan_id};")
\`\`\`

EOF

  if [[ -n "${instance_id}" ]]; then
    append_markdown <<EOF
\`\`\`sql
$(db_markdown "SELECT id, flow_def_id, entity_type, entity_id, status, requester_id, requester_org_id, started_at, completed_at FROM audit_instance WHERE id = ${instance_id};")
\`\`\`

\`\`\`sql
$(db_markdown "SELECT id, step_no, step_name, status, approver_id, approver_org_id, comment, approved_at, created_at FROM audit_step_instance WHERE instance_id = ${instance_id} ORDER BY step_no, id;")
\`\`\`

EOF
  fi
}

run_plan_flow() {
  local flow_code="$1"
  local label="$2"
  local plan_id="$3"
  local submitter="$4"
  local submit_path="$5"

  local submitter_token detail_file submit_file history_file mytasks_file
  local instance_id status pending_task_id pending_approver_id pending_approver_username
  local step_index=1

  submitter_token="$(login "${submitter}")"
  submit_file="${TMP_DIR}/${plan_id}-submit.json"
  detail_file="${TMP_DIR}/${plan_id}-detail.json"
  history_file="${TMP_DIR}/${plan_id}-history.json"
  mytasks_file="${TMP_DIR}/${plan_id}-mytasks.json"

  append_markdown <<EOF
## ${label}

- 流程编码: \`${flow_code}\`
- 业务对象: \`PLAN#${plan_id}\`
- 发起账号: \`${submitter}\`
- 提交接口: \`POST ${submit_path}\`

### 提交前数据库状态

\`\`\`sql
$(db_markdown "SELECT id, status, audit_instance_id, target_org_id, created_by_org_id, plan_level, updated_at FROM plan WHERE id = ${plan_id};")
\`\`\`

EOF

  api_call POST "${API_BASE}${submit_path}" "${submitter_token}" "{\"workflowCode\":\"${flow_code}\"}" "${submit_file}"
  wait_for_instance "${submitter_token}" "PLAN" "${plan_id}" "${detail_file}"
  instance_id="$(jq -r '.data.instanceId // empty' "${detail_file}")"

  append_markdown <<EOF
### 提交接口响应

\`\`\`json
$(json_to_block < "${submit_file}")
\`\`\`

### 提交后当前流程详情

\`\`\`json
$(json_to_block < "${detail_file}")
\`\`\`

EOF

  capture_snapshot "提交后数据库状态" "${plan_id}" "${instance_id}"

  status="$(jq -r '.data.status // empty' "${detail_file}")"
  while [[ "${status}" == "PENDING" ]]; do
    pending_task_id="$(jq -r '.data.tasks[] | select(.status == "PENDING") | .taskId' "${detail_file}" | head -n 1)"
    pending_approver_id="$(jq -r '.data.tasks[] | select(.status == "PENDING") | .approverId' "${detail_file}" | head -n 1)"
    pending_approver_username="$(db_value "SELECT username FROM sys_user WHERE id = ${pending_approver_id};")"

    local approver_token approve_file
    approver_token="$(login "${pending_approver_username}")"
    approve_file="${TMP_DIR}/${plan_id}-approve-${step_index}.json"

    api_call GET "${API_BASE}/workflows/my-tasks" "${approver_token}" "" "${mytasks_file}"
    api_call POST "${API_BASE}/workflows/tasks/${pending_task_id}/approve" "${approver_token}" \
      "{\"comment\":\"${label} API验证通过，第${step_index}个审批节点\"}" "${approve_file}"
    api_call GET "${API_BASE}/workflows/instances/entity/PLAN/${plan_id}" "${submitter_token}" "" "${detail_file}"

    append_markdown <<EOF
### 第 ${step_index} 次审批

- 待办步骤ID: \`${pending_task_id}\`
- 审批人ID: \`${pending_approver_id}\`
- 审批账号: \`${pending_approver_username}\`

\`\`\`json
$(json_to_block < "${mytasks_file}")
\`\`\`

\`\`\`json
$(json_to_block < "${approve_file}")
\`\`\`

\`\`\`json
$(json_to_block < "${detail_file}")
\`\`\`

EOF

    capture_snapshot "第 ${step_index} 次审批后的数据库状态" "${plan_id}" "${instance_id}"

    status="$(jq -r '.data.status // empty' "${detail_file}")"
    step_index=$((step_index + 1))
    if [[ ${step_index} -gt 8 ]]; then
      log "too many approval loops for plan ${plan_id}"
      break
    fi
  done

  api_call GET "${API_BASE}/workflows/instances/entity/PLAN/${plan_id}/list" "${submitter_token}" "" "${history_file}"
  append_markdown <<EOF
### 审批历史接口结果

\`\`\`json
$(json_to_block < "${history_file}")
\`\`\`

### 最终数据库状态

\`\`\`sql
$(db_markdown "SELECT id, status, audit_instance_id, target_org_id, created_by_org_id, plan_level, updated_at FROM plan WHERE id = ${plan_id};")
\`\`\`

\`\`\`sql
$(db_markdown "SELECT id, flow_def_id, entity_type, entity_id, status, requester_id, requester_org_id, started_at, completed_at FROM audit_instance WHERE id = ${instance_id};")
\`\`\`

\`\`\`sql
$(db_markdown "SELECT id, step_no, step_name, status, approver_id, approver_org_id, comment, approved_at, created_at FROM audit_step_instance WHERE instance_id = ${instance_id} ORDER BY step_no, id;")
\`\`\`

---

EOF
}

main() {
  seed_supporting_data

  cat > "${REPORT_FILE}" <<EOF
# 审批四流程 API 测试与数据库流转报告

- 执行时间: $(date '+%Y-%m-%d %H:%M:%S %Z')
- API 基地址: \`${API_BASE}\`
- 数据库: \`${DB_NAME}\`
- 测试方式: 不经过前端，直接使用真实登录账号调用后端 API，并结合 PostgreSQL 查询 \`plan\` / \`audit_instance\` / \`audit_step_instance\` 表记录数据库状态流转。

EOF

  run_plan_flow "PLAN_DISPATCH_STRATEGY" "流程一：战略发展部 Plan 下发审批" 4037 "zlb_admin" "/plans/4037/submit-dispatch"
  run_plan_flow "PLAN_DISPATCH_FUNCDEPT" "流程二：职能部门 Plan 下发审批" 7055 "jiaowu_report" "/plans/7055/submit-dispatch"
  run_plan_flow "PLAN_APPROVAL_FUNCDEPT" "流程三：职能部门 Plan 审批流程" 4044 "jiaowu_report" "/plans/4044/submit"
  run_plan_flow "PLAN_APPROVAL_COLLEGE" "流程四：二级学院 Plan 审批流程" 7057 "jisuanji_report" "/plans/7057/submit"

  log "report generated: ${REPORT_FILE}"
  printf '%s\n' "${REPORT_FILE}"
}

main "$@"
