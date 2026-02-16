-- Add missing columns to indicator table to match the entity definition
-- The indicator_status and indicator_level enums already exist from V1__baseline_schema.sql

-- Add level column (required field)
ALTER TABLE public.indicator 
ADD COLUMN level public.indicator_level;

-- Add owner_org_id and target_org_id (required fields)
ALTER TABLE public.indicator 
ADD COLUMN owner_org_id bigint,
ADD COLUMN target_org_id bigint;

-- Add status column
ALTER TABLE public.indicator 
ADD COLUMN status public.indicator_status DEFAULT 'ACTIVE'::public.indicator_status;

-- Add extended fields for frontend data alignment
ALTER TABLE public.indicator 
ADD COLUMN actual_value numeric(10,2),
ADD COLUMN target_value numeric(10,2),
ADD COLUMN unit varchar(50),
ADD COLUMN responsible_person varchar(100),
ADD COLUMN can_withdraw boolean,
ADD COLUMN is_qualitative boolean,
ADD COLUMN pending_progress integer,
ADD COLUMN pending_remark text,
ADD COLUMN pending_attachments jsonb,
ADD COLUMN progress_approval_status varchar(20),
ADD COLUMN status_audit jsonb,
ADD COLUMN type1 varchar(20),
ADD COLUMN type2 varchar(20);

-- Add department fields for dashboard support
ALTER TABLE public.indicator 
ADD COLUMN owner_dept varchar(100),
ADD COLUMN responsible_dept varchar(100),
ADD COLUMN year integer;

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

-- Add foreign key constraints
ALTER TABLE public.indicator 
ADD CONSTRAINT fk_indicator_owner_org 
FOREIGN KEY (owner_org_id) REFERENCES public.sys_org(id);

ALTER TABLE public.indicator 
ADD CONSTRAINT fk_indicator_target_org 
FOREIGN KEY (target_org_id) REFERENCES public.sys_org(id);

-- Add indexes for better query performance
CREATE INDEX idx_indicator_status ON public.indicator(status);
CREATE INDEX idx_indicator_level ON public.indicator(level);
CREATE INDEX idx_indicator_owner_org ON public.indicator(owner_org_id);
CREATE INDEX idx_indicator_target_org ON public.indicator(target_org_id);
CREATE INDEX idx_indicator_year ON public.indicator(year);

-- Add comments
COMMENT ON COLUMN public.indicator.status IS 'Indicator status: ACTIVE or ARCHIVED';
COMMENT ON COLUMN public.indicator.level IS 'Indicator level: STRAT_TO_FUNC, FUNC_TO_COLLEGE, etc.';
COMMENT ON COLUMN public.indicator.owner_org_id IS 'Owner organization (functional department)';
COMMENT ON COLUMN public.indicator.target_org_id IS 'Target organization (functional department or college)';
COMMENT ON COLUMN public.indicator.year IS 'Indicator year';
