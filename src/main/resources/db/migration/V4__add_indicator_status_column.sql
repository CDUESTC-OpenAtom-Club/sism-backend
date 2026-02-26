-- Add missing columns to indicator table to match the entity definition
-- Note: indicator_status exists from V1, but indicator_level needs to be created

-- Create indicator_level enum type if it doesn't exist
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'indicator_level') THEN
        CREATE TYPE public.indicator_level AS ENUM (
            'STRAT_TO_FUNC',
            'FUNC_TO_COLLEGE'
        );
    END IF;
END $$;

-- Add level column (required field) - only if not exists
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='indicator' AND column_name='level') THEN
        ALTER TABLE public.indicator ADD COLUMN level public.indicator_level;
    END IF;
END $$;

-- Add owner_org_id and target_org_id (required fields) - only if not exists
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='indicator' AND column_name='owner_org_id') THEN
        ALTER TABLE public.indicator ADD COLUMN owner_org_id bigint;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='indicator' AND column_name='target_org_id') THEN
        ALTER TABLE public.indicator ADD COLUMN target_org_id bigint;
    END IF;
END $$;

-- Add status column - only if not exists
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns 
                   WHERE table_name='indicator' AND column_name='status') THEN
        ALTER TABLE public.indicator ADD COLUMN status public.indicator_status DEFAULT 'ACTIVE'::public.indicator_status;
    END IF;
END $$;

-- Add extended fields for frontend data alignment - only if not exists
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='indicator' AND column_name='actual_value') THEN
        ALTER TABLE public.indicator ADD COLUMN actual_value numeric(10,2);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='indicator' AND column_name='target_value') THEN
        ALTER TABLE public.indicator ADD COLUMN target_value numeric(10,2);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='indicator' AND column_name='unit') THEN
        ALTER TABLE public.indicator ADD COLUMN unit varchar(50);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='indicator' AND column_name='responsible_person') THEN
        ALTER TABLE public.indicator ADD COLUMN responsible_person varchar(100);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='indicator' AND column_name='can_withdraw') THEN
        ALTER TABLE public.indicator ADD COLUMN can_withdraw boolean;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='indicator' AND column_name='is_qualitative') THEN
        ALTER TABLE public.indicator ADD COLUMN is_qualitative boolean;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='indicator' AND column_name='pending_progress') THEN
        ALTER TABLE public.indicator ADD COLUMN pending_progress integer;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='indicator' AND column_name='pending_remark') THEN
        ALTER TABLE public.indicator ADD COLUMN pending_remark text;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='indicator' AND column_name='pending_attachments') THEN
        ALTER TABLE public.indicator ADD COLUMN pending_attachments jsonb;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='indicator' AND column_name='progress_approval_status') THEN
        ALTER TABLE public.indicator ADD COLUMN progress_approval_status varchar(20);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='indicator' AND column_name='status_audit') THEN
        ALTER TABLE public.indicator ADD COLUMN status_audit jsonb;
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='indicator' AND column_name='type1') THEN
        ALTER TABLE public.indicator ADD COLUMN type1 varchar(20);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='indicator' AND column_name='type2') THEN
        ALTER TABLE public.indicator ADD COLUMN type2 varchar(20);
    END IF;
END $$;

-- Add department fields for dashboard support - only if not exists
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='indicator' AND column_name='owner_dept') THEN
        ALTER TABLE public.indicator ADD COLUMN owner_dept varchar(100);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='indicator' AND column_name='responsible_dept') THEN
        ALTER TABLE public.indicator ADD COLUMN responsible_dept varchar(100);
    END IF;
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name='indicator' AND column_name='year') THEN
        ALTER TABLE public.indicator ADD COLUMN year integer;
    END IF;
END $$;

-- Update existing records to have default values
UPDATE public.indicator 
SET status = 'ACTIVE'::public.indicator_status 
WHERE status IS NULL;

UPDATE public.indicator 
SET level = 'STRAT_TO_FUNC'::public.indicator_level 
WHERE level IS NULL;

-- Set NOT NULL constraints for required fields
ALTER TABLE public.indicator 
ALTER COLUMN status SET NOT NULL,
ALTER COLUMN level SET NOT NULL;

-- Add foreign key constraints (only if not exists)
DO $$ 
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_indicator_owner_org') THEN
        ALTER TABLE public.indicator 
        ADD CONSTRAINT fk_indicator_owner_org 
        FOREIGN KEY (owner_org_id) REFERENCES public.sys_org(id);
    END IF;
    
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_indicator_target_org') THEN
        ALTER TABLE public.indicator 
        ADD CONSTRAINT fk_indicator_target_org 
        FOREIGN KEY (target_org_id) REFERENCES public.sys_org(id);
    END IF;
END $$;

-- Add indexes for better query performance (only if not exists)
CREATE INDEX IF NOT EXISTS idx_indicator_status ON public.indicator(status);
CREATE INDEX IF NOT EXISTS idx_indicator_level ON public.indicator(level);
CREATE INDEX IF NOT EXISTS idx_indicator_owner_org ON public.indicator(owner_org_id);
CREATE INDEX IF NOT EXISTS idx_indicator_target_org ON public.indicator(target_org_id);
CREATE INDEX IF NOT EXISTS idx_indicator_year ON public.indicator(year);

-- Add comments
COMMENT ON COLUMN public.indicator.status IS 'Indicator status: ACTIVE or ARCHIVED';
COMMENT ON COLUMN public.indicator.level IS 'Indicator level: STRAT_TO_FUNC, FUNC_TO_COLLEGE, etc.';
COMMENT ON COLUMN public.indicator.owner_org_id IS 'Owner organization (functional department)';
COMMENT ON COLUMN public.indicator.target_org_id IS 'Target organization (functional department or college)';
COMMENT ON COLUMN public.indicator.year IS 'Indicator year';
