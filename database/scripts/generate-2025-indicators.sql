-- =====================================================
-- 生成 2025 年指标数据
-- 基于 2026 年现有指标数据复制
-- =====================================================

SET client_encoding = 'UTF8';

BEGIN;

-- =====================================================
-- 1. 复制指标 (indicator)
-- =====================================================
DO $$
DECLARE
    v_count INTEGER;
    v_2026_count INTEGER;
BEGIN
    -- 检查是否已有 2025 年指标数据
    SELECT COUNT(*) INTO v_count FROM indicator WHERE year = 2025;
    
    IF v_count = 0 THEN
        RAISE NOTICE '开始复制指标数据...';
        
        -- 获取 2026 年指标数量
        SELECT COUNT(*) INTO v_2026_count 
        FROM indicator 
        WHERE year = 2026 AND (is_deleted = false OR is_deleted IS NULL);
        RAISE NOTICE '2026 年共有 % 个活跃指标', v_2026_count;
        
        -- 第一步：复制顶层指标（parent_indicator_id IS NULL）
        INSERT INTO indicator (
            task_id, parent_indicator_id, level, owner_org_id, target_org_id,
            indicator_desc, weight_percent, sort_order, year, status,
            type, progress, owner_dept, responsible_dept, can_withdraw,
            is_deleted, created_at, updated_at, remark,
            type1, type2, is_qualitative, unit, actual_value, target_value,
            responsible_person, status_audit, progress_approval_status,
            pending_progress, pending_remark, pending_attachments
        )
        SELECT 
            task_id,
            NULL as parent_indicator_id,  -- 顶层指标
            level,
            owner_org_id,
            target_org_id,
            indicator_desc || ' (2025年度)',
            weight_percent,
            sort_order,
            2025 as year,
            status,
            type,
            FLOOR(RANDOM() * 60 + 20)::INTEGER as progress,  -- 随机进度 20-80
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
            CASE 
                WHEN is_qualitative = false AND target_value IS NOT NULL 
                THEN FLOOR(RANDOM() * target_value * 0.8)::NUMERIC 
                ELSE actual_value 
            END as actual_value,
            target_value,
            responsible_person,
            '[]'::JSONB as status_audit,  -- 清空审计记录
            'NONE' as progress_approval_status,
            NULL as pending_progress,
            NULL as pending_remark,
            NULL as pending_attachments
        FROM indicator
        WHERE year = 2026 
          AND (is_deleted = false OR is_deleted IS NULL)
          AND parent_indicator_id IS NULL  -- 只复制顶层指标
        ORDER BY indicator_id;
        
        GET DIAGNOSTICS v_count = ROW_COUNT;
        RAISE NOTICE '✅ 已复制 % 个顶层指标', v_count;
        
        -- 第二步：复制子指标（如果有的话）
        -- 通过 indicator_desc 匹配父指标
        INSERT INTO indicator (
            task_id, parent_indicator_id, level, owner_org_id, target_org_id,
            indicator_desc, weight_percent, sort_order, year, status,
            type, progress, owner_dept, responsible_dept, can_withdraw,
            is_deleted, created_at, updated_at, remark,
            type1, type2, is_qualitative, unit, actual_value, target_value,
            responsible_person, status_audit, progress_approval_status
        )
        SELECT 
            i2026.task_id,
            i2025.indicator_id as parent_indicator_id,  -- 映射到 2025 年的父指标
            i2026.level,
            i2026.owner_org_id,
            i2026.target_org_id,
            i2026.indicator_desc || ' (2025年度)',
            i2026.weight_percent,
            i2026.sort_order,
            2025 as year,
            i2026.status,
            i2026.type,
            FLOOR(RANDOM() * 60 + 20)::INTEGER as progress,
            i2026.owner_dept,
            i2026.responsible_dept,
            i2026.can_withdraw,
            false as is_deleted,
            NOW() - INTERVAL '1 year' as created_at,
            NOW() - INTERVAL '6 months' as updated_at,
            '2025年度测试数据' as remark,
            i2026.type1,
            i2026.type2,
            i2026.is_qualitative,
            i2026.unit,
            CASE 
                WHEN i2026.is_qualitative = false AND i2026.target_value IS NOT NULL 
                THEN FLOOR(RANDOM() * i2026.target_value * 0.8)::NUMERIC 
                ELSE i2026.actual_value 
            END as actual_value,
            i2026.target_value,
            i2026.responsible_person,
            '[]'::JSONB as status_audit,
            'NONE' as progress_approval_status
        FROM indicator i2026
        JOIN indicator p2026 ON p2026.indicator_id = i2026.parent_indicator_id
        JOIN indicator i2025 ON 
            REPLACE(i2025.indicator_desc, ' (2025年度)', '') = p2026.indicator_desc
            AND i2025.year = 2025
        WHERE i2026.year = 2026 
          AND (i2026.is_deleted = false OR i2026.is_deleted IS NULL)
          AND i2026.parent_indicator_id IS NOT NULL;
        
        GET DIAGNOSTICS v_count = ROW_COUNT;
        IF v_count > 0 THEN
            RAISE NOTICE '✅ 已复制 % 个子指标', v_count;
        END IF;
        
    ELSE
        RAISE NOTICE '⚠️  2025 年已有 % 个指标，跳过复制', v_count;
    END IF;
END $$;

-- =====================================================
-- 2. 复制里程碑 (indicator_milestone)
-- =====================================================
DO $$
DECLARE
    v_count INTEGER;
BEGIN
    -- 检查是否已有 2025 年里程碑数据
    SELECT COUNT(*) INTO v_count 
    FROM indicator_milestone m
    JOIN indicator i ON i.indicator_id = m.indicator_id
    WHERE i.year = 2025;
    
    IF v_count = 0 THEN
        RAISE NOTICE '开始复制里程碑数据...';
        
        -- 复制里程碑（通过指标描述匹配）
        INSERT INTO indicator_milestone (
            indicator_id, milestone_name, milestone_desc, due_date,
            status, sort_order, inherited_from, target_progress, is_paired,
            created_at, updated_at
        )
        SELECT 
            i2025.indicator_id,
            m2026.milestone_name,
            m2026.milestone_desc,
            m2026.due_date - INTERVAL '1 year' as due_date,  -- 日期减一年
            CASE 
                WHEN m2026.due_date - INTERVAL '1 year' < NOW() THEN 'COMPLETED'
                ELSE 'NOT_STARTED'
            END as status,
            m2026.sort_order,
            NULL as inherited_from,
            m2026.target_progress,
            m2026.is_paired,
            NOW() - INTERVAL '1 year' as created_at,
            NOW() - INTERVAL '6 months' as updated_at
        FROM indicator_milestone m2026
        JOIN indicator i2026 ON i2026.indicator_id = m2026.indicator_id
        JOIN indicator i2025 ON 
            REPLACE(i2025.indicator_desc, ' (2025年度)', '') = i2026.indicator_desc
            AND i2025.year = 2025
        WHERE i2026.year = 2026
          AND (i2026.is_deleted = false OR i2026.is_deleted IS NULL)
        ORDER BY m2026.id;
        
        GET DIAGNOSTICS v_count = ROW_COUNT;
        RAISE NOTICE '✅ 已复制 % 个里程碑', v_count;
    ELSE
        RAISE NOTICE '⚠️  2025 年已有 % 个里程碑，跳过复制', v_count;
    END IF;
END $$;

-- =====================================================
-- 3. 验证生成结果
-- =====================================================
DO $$
DECLARE
    v_indicator_count INTEGER;
    v_milestone_count INTEGER;
    v_2026_indicator_count INTEGER;
    v_2026_milestone_count INTEGER;
BEGIN
    -- 统计 2025 年数据
    SELECT COUNT(*) INTO v_indicator_count FROM indicator WHERE year = 2025 AND (is_deleted = false OR is_deleted IS NULL);
    SELECT COUNT(*) INTO v_milestone_count 
    FROM indicator_milestone m
    JOIN indicator i ON i.indicator_id = m.indicator_id
    WHERE i.year = 2025;
    
    -- 统计 2026 年数据（对比）
    SELECT COUNT(*) INTO v_2026_indicator_count FROM indicator WHERE year = 2026 AND (is_deleted = false OR is_deleted IS NULL);
    SELECT COUNT(*) INTO v_2026_milestone_count 
    FROM indicator_milestone m
    JOIN indicator i ON i.indicator_id = m.indicator_id
    WHERE i.year = 2026;
    
    RAISE NOTICE '';
    RAISE NOTICE '========================================';
    RAISE NOTICE '📊 数据生成完成统计:';
    RAISE NOTICE '========================================';
    RAISE NOTICE '指标:';
    RAISE NOTICE '  2026 年: % 个', v_2026_indicator_count;
    RAISE NOTICE '  2025 年: % 个 (新生成)', v_indicator_count;
    RAISE NOTICE '';
    RAISE NOTICE '里程碑:';
    RAISE NOTICE '  2026 年: % 个', v_2026_milestone_count;
    RAISE NOTICE '  2025 年: % 个 (新生成)', v_milestone_count;
    RAISE NOTICE '========================================';
END $$;

COMMIT;

-- 最终验证查询
SELECT 
    year,
    COUNT(*) as indicator_count,
    COUNT(CASE WHEN status = 'ACTIVE' THEN 1 END) as active_count,
    ROUND(AVG(progress)) as avg_progress
FROM indicator
WHERE is_deleted = false OR is_deleted IS NULL
GROUP BY year
ORDER BY year DESC;
