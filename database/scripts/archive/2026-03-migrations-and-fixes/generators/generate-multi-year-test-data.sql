-- 生成多年份测试数据
-- 用于测试年份切换功能
-- 基于现有 2026 年数据生成 2024 和 2025 年的测试数据

-- =====================================================
-- 1. 为 2024 年生成测试指标
-- =====================================================
DO $$
DECLARE
    v_count INTEGER;
BEGIN
    -- 检查是否已有 2024 年数据
    SELECT COUNT(*) INTO v_count FROM indicator WHERE year = 2024;
    
    IF v_count = 0 THEN
        RAISE NOTICE '开始生成 2024 年测试数据...';
        
        -- 从 2026 年数据复制 10 条作为 2024 年测试数据
        INSERT INTO indicator (
            task_id, parent_indicator_id, level, owner_org_id, target_org_id,
            indicator_desc, weight_percent, sort_order, year, status,
            type, progress, owner_dept, responsible_dept, can_withdraw,
            is_deleted, created_at, updated_at, remark,
            type1, type2, is_qualitative, unit, actual_value, target_value,
            responsible_person, status_audit, progress_approval_status
        )
        SELECT 
            task_id,
            parent_indicator_id,
            level,
            owner_org_id,
            target_org_id,
            indicator_desc || ' (2024年度)',
            weight_percent,
            sort_order,
            2024 as year,
            status,
            type,
            FLOOR(RANDOM() * 80 + 10)::INTEGER as progress, -- 随机进度 10-90
            owner_dept,
            responsible_dept,
            can_withdraw,
            false as is_deleted,
            NOW() - INTERVAL '2 years' as created_at,
            NOW() - INTERVAL '1 year' as updated_at,
            '2024年度测试数据' as remark,
            type1,
            type2,
            is_qualitative,
            unit,
            CASE WHEN is_qualitative = false THEN FLOOR(RANDOM() * 80 + 10)::NUMERIC END as actual_value,
            CASE WHEN is_qualitative = false THEN 100::NUMERIC END as target_value,
            responsible_person,
            status_audit,
            'NONE' as progress_approval_status
        FROM indicator
        WHERE year = 2026 
          AND (is_deleted = false OR is_deleted IS NULL)
          AND parent_indicator_id IS NULL -- 只复制顶层指标
        ORDER BY indicator_id
        LIMIT 10;
        
        GET DIAGNOSTICS v_count = ROW_COUNT;
        RAISE NOTICE '✅ 已生成 % 条 2024 年测试数据', v_count;
    ELSE
        RAISE NOTICE '⚠️  2024 年已有 % 条数据，跳过生成', v_count;
    END IF;
END $$;

-- =====================================================
-- 2. 为 2025 年生成测试指标
-- =====================================================
DO $$
DECLARE
    v_count INTEGER;
BEGIN
    -- 检查是否已有 2025 年数据
    SELECT COUNT(*) INTO v_count FROM indicator WHERE year = 2025;
    
    IF v_count = 0 THEN
        RAISE NOTICE '开始生成 2025 年测试数据...';
        
        -- 从 2026 年数据复制 15 条作为 2025 年测试数据
        INSERT INTO indicator (
            task_id, parent_indicator_id, level, owner_org_id, target_org_id,
            indicator_desc, weight_percent, sort_order, year, status,
            type, progress, owner_dept, responsible_dept, can_withdraw,
            is_deleted, created_at, updated_at, remark,
            type1, type2, is_qualitative, unit, actual_value, target_value,
            responsible_person, status_audit, progress_approval_status
        )
        SELECT 
            task_id,
            parent_indicator_id,
            level,
            owner_org_id,
            target_org_id,
            indicator_desc || ' (2025年度)',
            weight_percent,
            sort_order,
            2025 as year,
            status,
            type,
            FLOOR(RANDOM() * 70 + 20)::INTEGER as progress, -- 随机进度 20-90
            owner_dept,
            responsible_dept,
            can_withdraw,
            false as is_deleted,
            NOW() - INTERVAL '1 year' as created_at,
            NOW() - INTERVAL '6 months' as updated_at,
            '2025年度测试数据' as remark,
            type1,
            type2,
            is_qualitative,
            unit,
            CASE WHEN is_qualitative = false THEN FLOOR(RANDOM() * 70 + 20)::NUMERIC END as actual_value,
            CASE WHEN is_qualitative = false THEN 100::NUMERIC END as target_value,
            responsible_person,
            status_audit,
            'NONE' as progress_approval_status
        FROM indicator
        WHERE year = 2026 
          AND (is_deleted = false OR is_deleted IS NULL)
          AND parent_indicator_id IS NULL -- 只复制顶层指标
        ORDER BY indicator_id
        LIMIT 15;
        
        GET DIAGNOSTICS v_count = ROW_COUNT;
        RAISE NOTICE '✅ 已生成 % 条 2025 年测试数据', v_count;
    ELSE
        RAISE NOTICE '⚠️  2025 年已有 % 条数据，跳过生成', v_count;
    END IF;
END $$;

-- =====================================================
-- 3. 验证生成结果
-- =====================================================
DO $$
DECLARE
    v_2024_count INTEGER;
    v_2025_count INTEGER;
    v_2026_count INTEGER;
BEGIN
    SELECT COUNT(*) INTO v_2024_count FROM indicator WHERE year = 2024 AND (is_deleted = false OR is_deleted IS NULL);
    SELECT COUNT(*) INTO v_2025_count FROM indicator WHERE year = 2025 AND (is_deleted = false OR is_deleted IS NULL);
    SELECT COUNT(*) INTO v_2026_count FROM indicator WHERE year = 2026 AND (is_deleted = false OR is_deleted IS NULL);
    
    RAISE NOTICE '';
    RAISE NOTICE '========================================';
    RAISE NOTICE '📊 年份数据统计:';
    RAISE NOTICE '  2024 年: % 条', v_2024_count;
    RAISE NOTICE '  2025 年: % 条', v_2025_count;
    RAISE NOTICE '  2026 年: % 条', v_2026_count;
    RAISE NOTICE '========================================';
END $$;
