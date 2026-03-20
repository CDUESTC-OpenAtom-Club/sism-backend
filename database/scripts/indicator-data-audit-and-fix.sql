-- Indicator data audit and repair helpers
-- Usage:
--   psql -h <host> -p <port> -U <user> -d <db> -f database/scripts/indicator-data-audit-and-fix.sql

-- 1) Summary audit for fields that can block partial updates in StrategyApplicationService.updateIndicator
SELECT
  COUNT(*) AS total_indicators,
  COUNT(*) FILTER (WHERE weight_percent IS NULL) AS weight_null,
  COUNT(*) FILTER (WHERE weight_percent <= 0) AS weight_non_positive,
  COUNT(*) FILTER (WHERE sort_order IS NULL) AS sort_null,
  COUNT(*) FILTER (WHERE sort_order < 0) AS sort_negative,
  COUNT(*) FILTER (WHERE indicator_desc IS NULL) AS desc_null,
  COUNT(*) FILTER (WHERE btrim(indicator_desc) = '') AS desc_blank
FROM public.indicator
WHERE COALESCE(is_deleted, false) = false;

-- 2) Detailed list of flagged rows
WITH flagged AS (
  SELECT
    id,
    task_id,
    parent_indicator_id,
    indicator_desc,
    weight_percent,
    sort_order,
    remark,
    progress,
    status,
    updated_at,
    CONCAT_WS(
      '; ',
      CASE
        WHEN weight_percent IS NULL THEN 'weight_null'
        WHEN weight_percent <= 0 THEN 'weight_non_positive'
      END,
      CASE
        WHEN sort_order IS NULL THEN 'sort_null'
        WHEN sort_order < 0 THEN 'sort_negative'
      END,
      CASE
        WHEN indicator_desc IS NULL THEN 'desc_null'
        WHEN btrim(indicator_desc) = '' THEN 'desc_blank'
      END
    ) AS issues
  FROM public.indicator
  WHERE COALESCE(is_deleted, false) = false
    AND (
      weight_percent IS NULL OR weight_percent <= 0 OR
      sort_order IS NULL OR sort_order < 0 OR
      indicator_desc IS NULL OR btrim(indicator_desc) = ''
    )
)
SELECT *
FROM flagged
ORDER BY updated_at DESC NULLS LAST, id DESC;

-- 3) Optional repair SQL template
-- Review carefully before executing in production.
--
-- BEGIN;
--
-- UPDATE public.indicator
-- SET
--   weight_percent = COALESCE(NULLIF(weight_percent, 0), 100),
--   sort_order = CASE
--     WHEN sort_order IS NULL OR sort_order < 0 THEN 0
--     ELSE sort_order
--   END,
--   indicator_desc = CASE
--     WHEN indicator_desc IS NULL OR btrim(indicator_desc) = '' THEN CONCAT('指标-', id)
--     ELSE btrim(indicator_desc)
--   END,
--   updated_at = NOW()
-- WHERE COALESCE(is_deleted, false) = false
--   AND (
--     weight_percent IS NULL OR weight_percent <= 0 OR
--     sort_order IS NULL OR sort_order < 0 OR
--     indicator_desc IS NULL OR btrim(indicator_desc) = ''
--   );
--
-- COMMIT;
