-- progress_report clean seed
-- Scope:
-- - progress_report is treated as the analytics Report entity.
-- - This is the output-side report table used by the sism-analytics module.
-- - It is intentionally distinct from plan_report, which belongs to the execution/input workflow.

BEGIN;

INSERT INTO public.progress_report (
    id,
    report_name,
    report_type,
    report_format,
    status,
    file_path,
    file_size,
    creator_id,
    generation_time,
    report_params,
    description,
    is_deleted,
    created_at,
    updated_at
)
VALUES
    (
        9501,
        '2026年度战略执行总览分析报告',
        'STRATEGIC',
        'PDF',
        'GENERATED',
        '/analytics/reports/2026/strategic-overview-9501.pdf',
        5242880,
        188,
        TIMESTAMP '2026-03-20 09:30:00',
        '{"cycleId":4,"scope":"ALL","source":"plan_report","reportMonth":"202603"}',
        '基于 2026 年 3 月各部门 plan_report 汇总生成的战略执行总览分析报告。',
        false,
        TIMESTAMP '2026-03-20 09:00:00',
        TIMESTAMP '2026-03-20 09:30:00'
    ),
    (
        9502,
        '2026年度执行偏差专题分析',
        'EXECUTION',
        'EXCEL',
        'GENERATED',
        '/analytics/reports/2026/execution-gap-9502.xlsx',
        3145728,
        189,
        TIMESTAMP '2026-03-20 10:15:00',
        '{"cycleId":4,"scope":"DEVIATION","source":"alert_event","windowId":7101}',
        '基于预警事件与填报进度数据生成的执行偏差专题分析导出。',
        false,
        TIMESTAMP '2026-03-20 09:50:00',
        TIMESTAMP '2026-03-20 10:15:00'
    ),
    (
        9503,
        '2026年学院填报质量分析草稿',
        'COMPREHENSIVE',
        'HTML',
        'DRAFT',
        NULL,
        NULL,
        124,
        NULL,
        '{"cycleId":4,"scope":"COLLEGE","source":"plan_report_indicator_attachment"}',
        '面向二级学院填报完整性与附件提交质量的综合分析草稿。',
        false,
        TIMESTAMP '2026-03-20 11:00:00',
        TIMESTAMP '2026-03-20 11:00:00'
    ),
    (
        9504,
        '2026年度财务协同分析报告',
        'FINANCIAL',
        'CSV',
        'FAILED',
        NULL,
        NULL,
        190,
        NULL,
        '{"cycleId":4,"scope":"FINANCIAL","source":"plan_report"}',
        '财务协同分析报告生成失败示例，用于验证 analytics 状态流。',
        false,
        TIMESTAMP '2026-03-20 11:10:00',
        TIMESTAMP '2026-03-20 11:25:00'
    )
ON CONFLICT (id) DO UPDATE
SET
    report_name = EXCLUDED.report_name,
    report_type = EXCLUDED.report_type,
    report_format = EXCLUDED.report_format,
    status = EXCLUDED.status,
    file_path = EXCLUDED.file_path,
    file_size = EXCLUDED.file_size,
    creator_id = EXCLUDED.creator_id,
    generation_time = EXCLUDED.generation_time,
    report_params = EXCLUDED.report_params,
    description = EXCLUDED.description,
    is_deleted = EXCLUDED.is_deleted,
    created_at = EXCLUDED.created_at,
    updated_at = EXCLUDED.updated_at;

COMMIT;
