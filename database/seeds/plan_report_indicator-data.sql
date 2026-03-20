-- plan_report_indicator clean seed

BEGIN;

DELETE FROM public.plan_report_indicator
WHERE id IN (3505, 3506);

INSERT INTO public.plan_report_indicator (
    id,
    report_id,
    indicator_id,
    progress,
    milestone_note,
    comment,
    created_at
)
VALUES
    (3501, 3001, 2001, 100, '重点工作已全部完成并形成台账', '党委办公室已按要求完成该项指标', NOW()),
    (3502, 3001, 2002, 85, '专项台账已建立，待补充一项总结材料', '整体推进良好', NOW()),
    (3503, 3002, 2003, 20, '已完成初版巡检计划草稿', '等待正式提交前再补充附件', NOW()),
    (3504, 3002, 2004, 10, '已开始梳理重点区域整改清单', '当前仍为草稿', NOW())
ON CONFLICT (id) DO UPDATE
SET
    report_id = EXCLUDED.report_id,
    indicator_id = EXCLUDED.indicator_id,
    progress = EXCLUDED.progress,
    milestone_note = EXCLUDED.milestone_note,
    comment = EXCLUDED.comment,
    created_at = EXCLUDED.created_at;

COMMIT;
