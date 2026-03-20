-- cycle clean seed

BEGIN;

INSERT INTO public.cycle (
    id,
    cycle_name,
    year,
    start_date,
    end_date,
    description,
    created_at,
    updated_at
)
VALUES
    (5, '2023年度战略目标考核', 2023, DATE '2023-01-01', DATE '2023-12-31', '2023年度学校战略发展目标考核周期', NOW(), NOW()),
    (6, '2024年度战略目标考核', 2024, DATE '2024-01-01', DATE '2024-12-31', '2024年度学校战略发展目标考核周期', NOW(), NOW()),
    (7, '2025年度战略目标考核', 2025, DATE '2025-01-01', DATE '2025-12-31', '2025年度学校战略发展目标考核周期', NOW(), NOW()),
    (4, '2026年度战略目标考核', 2026, DATE '2026-01-01', DATE '2026-12-31', '2026年度学校战略发展目标考核周期', NOW(), NOW())
ON CONFLICT (id) DO UPDATE
SET
    cycle_name = EXCLUDED.cycle_name,
    year = EXCLUDED.year,
    start_date = EXCLUDED.start_date,
    end_date = EXCLUDED.end_date,
    description = EXCLUDED.description,
    updated_at = EXCLUDED.updated_at;

COMMIT;
