CREATE TYPE public.alert_severity AS ENUM (
    'INFO',
    'WARNING',
    'CRITICAL'
);

CREATE TYPE public.alert_status AS ENUM (
    'OPEN',
    'CLOSED',
    'IN_PROGRESS',
    'RESOLVED'
);

CREATE TYPE public.approval_action AS ENUM (
    'APPROVE',
    'REJECT',
    'RETURN'
);

CREATE TYPE public.audit_action AS ENUM (
    'CREATE',
    'UPDATE',
    'DELETE',
    'APPROVE',
    'ARCHIVE',
    'RESTORE'
);

CREATE TYPE public.audit_entity_type AS ENUM (
    'ORG',
    'USER',
    'CYCLE',
    'TASK',
    'INDICATOR',
    'MILESTONE',
    'REPORT',
    'ADHOC_TASK',
    'ALERT'
);

CREATE TYPE public.indicator_status AS ENUM (
    'ACTIVE',
    'ARCHIVED'
);

CREATE TYPE public.milestone_status AS ENUM (
    'NOT_STARTED',
    'IN_PROGRESS',
    'COMPLETED',
    'DELAYED',
    'CANCELED'
);

CREATE TYPE public.org_type AS ENUM (
    'STRATEGY_DEPT',
    'FUNCTION_DEPT',
    'COLLEGE',
    'DIVISION',
    'SCHOOL',
    'FUNCTIONAL_DEPT',
    'OTHER'
);

CREATE TYPE public.plan_level AS ENUM (
    'STRAT_TO_FUNC',
    'FUNC_TO_COLLEGE'
);

CREATE TYPE public.report_status AS ENUM (
    'DRAFT',
    'SUBMITTED',
    'RETURNED',
    'APPROVED',
    'REJECTED'
);

CREATE TYPE public.task_type AS ENUM (
    'BASIC',
    'DEVELOPMENT',
    'REGULAR',
    'KEY',
    'SPECIAL',
    'QUANTITATIVE'
);

CREATE SEQUENCE public.attachment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.audit_action_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.audit_flow_def_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.audit_instance_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.audit_step_def_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.common_log_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.cycle_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.idempotency_records_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.indicator_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.indicator_milestone_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.org_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.plan_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.plan_report_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.plan_report_indicator_attachment_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.plan_report_indicator_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.refresh_tokens_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.sys_permission_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.sys_role_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.sys_role_permission_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.sys_user_role_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.sys_task_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.task_plan_assessment_cycle_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.user_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.warn_event_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.warn_level_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.warn_rule_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE SEQUENCE public.warn_summary_daily_id_seq
    START WITH 1
    INCREMENT BY 1
    NO MINVALUE
    NO MAXVALUE
    CACHE 1;

CREATE TABLE public.attachment (
id bigint NOT NULL,
    storage_driver character varying(16) DEFAULT 'FILE'::character varying NOT NULL,
    bucket character varying(128),
    object_key text NOT NULL,
    public_url text,
    original_name text NOT NULL,
    content_type character varying(128),
    file_ext character varying(16),
    size_bytes bigint NOT NULL,
    sha256 character(64),
    etag text,
    uploaded_by bigint NOT NULL,
    uploaded_at timestamp with time zone DEFAULT now() NOT NULL,
    remark text,
    is_deleted boolean DEFAULT false NOT NULL,
    deleted_at timestamp with time zone,
    CONSTRAINT attachment_size_bytes_check CHECK ((size_bytes >= 0))
);

CREATE TABLE public.audit_action_log (
id bigint NOT NULL,
    instance_id bigint NOT NULL,
    step_id bigint,
    action character varying(32) NOT NULL,
    from_step_id bigint,
    to_step_id bigint,
    operator_id bigint NOT NULL,
    comment character varying(2000),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT audit_action_log_action_check CHECK (((action)::text = ANY ((ARRAY['SUBMIT'::character varying, 'APPROVE'::character varying, 'AUTO_APPROVE'::character varying, 'RETURN'::character varying, 'REJECT'::character varying, 'WITHDRAW'::character varying])::text[])))
);

CREATE TABLE public.audit_flow_def (
id bigint NOT NULL,
    flow_code character varying(64) NOT NULL,
    flow_name character varying(128) NOT NULL,
    biz_type character varying(32) NOT NULL,
    is_enabled boolean DEFAULT true NOT NULL,
    remark character varying(512),
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT audit_flow_def_biz_type_check CHECK (((biz_type)::text = ANY ((ARRAY['PLAN'::character varying, 'PLAN_REPORT'::character varying])::text[])))
);

CREATE TABLE public.audit_instance (
id bigint NOT NULL,
    flow_id bigint NOT NULL,
    biz_type character varying(32) NOT NULL,
    biz_id bigint NOT NULL,
    current_step_id bigint,
    status character varying(32) DEFAULT 'DRAFT'::character varying NOT NULL,
    started_at timestamp with time zone,
    finished_at timestamp with time zone,
    created_by bigint NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT audit_instance_biz_type_check CHECK (((biz_type)::text = ANY ((ARRAY['PLAN'::character varying, 'PLAN_REPORT'::character varying])::text[]))),
    CONSTRAINT audit_instance_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'IN_REVIEW'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying, 'WITHDRAWN'::character varying])::text[])))
);

CREATE TABLE public.audit_step_def (
id bigint NOT NULL,
    flow_id bigint NOT NULL,
    step_no integer NOT NULL,
    step_code character varying(64) NOT NULL,
    step_name character varying(128) NOT NULL,
    role_id bigint CONSTRAINT audit_step_def_role_code_not_null NOT NULL,
    is_terminal boolean DEFAULT false NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL,
    updated_at timestamp with time zone DEFAULT now() NOT NULL,
    CONSTRAINT audit_step_def_step_no_check CHECK ((step_no > 0))
);

CREATE TABLE public.common_log (
log_id bigint CONSTRAINT audit_log_log_id_not_null NOT NULL,
    entity_type public.audit_entity_type CONSTRAINT audit_log_entity_type_not_null NOT NULL,
    entity_id bigint CONSTRAINT audit_log_entity_id_not_null NOT NULL,
    action public.audit_action CONSTRAINT audit_log_action_not_null NOT NULL,
    before_json jsonb,
    after_json jsonb,
    changed_fields jsonb,
    reason text,
    actor_user_id bigint,
    actor_org_id bigint,
    created_at timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP CONSTRAINT audit_log_created_at_not_null NOT NULL
);

CREATE TABLE public.cycle (
id bigint CONSTRAINT assessment_cycle_cycle_id_not_null NOT NULL,
    cycle_name character varying(100) CONSTRAINT assessment_cycle_cycle_name_not_null NOT NULL,
    year integer CONSTRAINT assessment_cycle_year_not_null NOT NULL,
    start_date date CONSTRAINT assessment_cycle_start_date_not_null NOT NULL,
    end_date date CONSTRAINT assessment_cycle_end_date_not_null NOT NULL,
    description text,
    created_at timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP CONSTRAINT assessment_cycle_created_at_not_null NOT NULL,
    updated_at timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP CONSTRAINT assessment_cycle_updated_at_not_null NOT NULL
);

CREATE TABLE public.idempotency_records (
id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    expires_at timestamp(6) without time zone NOT NULL,
    http_method character varying(10),
    idempotency_key character varying(64) NOT NULL,
    request_path character varying(255),
    response_body text,
    status character varying(20),
    status_code integer,
    CONSTRAINT idempotency_records_status_check CHECK (((status)::text = ANY ((ARRAY['PENDING'::character varying, 'COMPLETED'::character varying, 'FAILED'::character varying])::text[])))
);

CREATE TABLE public.indicator (
id bigint CONSTRAINT indicator_indicator_id_not_null NOT NULL,
    task_id bigint NOT NULL,
    parent_indicator_id bigint,
    indicator_desc text NOT NULL,
    weight_percent numeric(5,2) DEFAULT 0 NOT NULL,
    sort_order integer DEFAULT 0 NOT NULL,
    remark text,
    created_at timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    type character varying(20) DEFAULT '定量'::character varying NOT NULL,
    progress integer DEFAULT 0,
    is_deleted boolean DEFAULT false NOT NULL
);

CREATE TABLE public.indicator_milestone (
id bigint CONSTRAINT milestone_milestone_id_not_null NOT NULL,
    indicator_id bigint CONSTRAINT milestone_indicator_id_not_null NOT NULL,
    milestone_name character varying(200) CONSTRAINT milestone_milestone_name_not_null NOT NULL,
    milestone_desc text,
    due_date date CONSTRAINT milestone_due_date_not_null NOT NULL,
    status public.milestone_status DEFAULT 'NOT_STARTED'::public.milestone_status CONSTRAINT milestone_status_not_null NOT NULL,
    sort_order integer DEFAULT 0 CONSTRAINT milestone_sort_order_not_null NOT NULL,
    created_at timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP CONSTRAINT milestone_created_at_not_null NOT NULL,
    updated_at timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP CONSTRAINT milestone_updated_at_not_null NOT NULL,
    target_progress integer DEFAULT 0,
    is_paired boolean DEFAULT false
);

CREATE TABLE public.sys_org (
id bigint CONSTRAINT org_org_id_not_null NOT NULL,
    name character varying(100) CONSTRAINT org_org_name_not_null NOT NULL,
    type public.org_type CONSTRAINT org_org_type_not_null NOT NULL,
    parent_org_id bigint,
    is_active boolean DEFAULT true CONSTRAINT org_is_active_not_null NOT NULL,
    sort_order integer DEFAULT 0 CONSTRAINT org_sort_order_not_null NOT NULL,
    created_at timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP CONSTRAINT org_created_at_not_null NOT NULL,
    updated_at timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP CONSTRAINT org_updated_at_not_null NOT NULL
);

CREATE TABLE public.plan (
id bigint CONSTRAINT task_plan_id_not_null NOT NULL,
    cycle_id bigint CONSTRAINT task_plan_assessment_cycle_id_not_null NOT NULL,
    created_at timestamp without time zone CONSTRAINT task_plan_create_at_not_null NOT NULL,
    updated_at timestamp without time zone CONSTRAINT plan_update_at_not_null NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    target_org_id bigint CONSTRAINT plan_org_id_not_null NOT NULL,
    created_by_org_id bigint NOT NULL,
    plan_level public.plan_level NOT NULL,
    status character varying DEFAULT 'DRAFT'::character varying NOT NULL,
    CONSTRAINT ck_plan_status CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'IN_REVIEW'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying, 'RETURNED'::character varying, 'WITHDRAWN'::character varying])::text[])))
);

CREATE TABLE public.plan_report (
id bigint NOT NULL,
    plan_id bigint NOT NULL,
    report_month character(6) NOT NULL,
    created_by bigint NOT NULL,
    report_org_type character varying(16) NOT NULL,
    report_org_id bigint NOT NULL,
    status character varying(16) DEFAULT 'DRAFT'::character varying NOT NULL,
    submitted_at timestamp with time zone,
    remark text,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    is_deleted boolean DEFAULT false NOT NULL,
    CONSTRAINT plan_report_report_month_check CHECK ((report_month ~ '^[0-9]{6}$'::text)),
    CONSTRAINT plan_report_report_org_type_check CHECK (((report_org_type)::text = ANY ((ARRAY['FUNC_DEPT'::character varying, 'COLLEGE'::character varying])::text[]))),
    CONSTRAINT plan_report_status_check CHECK (((status)::text = ANY ((ARRAY['DRAFT'::character varying, 'IN_REVIEW'::character varying, 'APPROVED'::character varying, 'REJECTED'::character varying])::text[])))
);

CREATE TABLE public.plan_report_indicator (
id bigint NOT NULL,
    report_id bigint NOT NULL,
    indicator_id bigint NOT NULL,
    progress integer DEFAULT 0 NOT NULL,
    milestone_note text,
    comment text,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT plan_report_indicator_progress_check CHECK (((progress >= 0) AND (progress <= 100)))
);

CREATE TABLE public.plan_report_indicator_attachment (
id bigint NOT NULL,
    plan_report_indicator_id bigint CONSTRAINT plan_report_indicator_attachm_plan_report_indicator_id_not_null NOT NULL,
    attachment_id bigint NOT NULL,
    sort_order integer DEFAULT 0 CONSTRAINT plan_report_indicator_attachment_display_order_not_null NOT NULL,
    created_by bigint NOT NULL,
    created_at timestamp with time zone DEFAULT now() NOT NULL
);

CREATE TABLE public.refresh_tokens (
id bigint NOT NULL,
    created_at timestamp(6) without time zone NOT NULL,
    device_info character varying(255),
    expires_at timestamp(6) without time zone NOT NULL,
    ip_address character varying(45),
    revoked_at timestamp(6) without time zone,
    token_hash character varying(64) NOT NULL,
    user_id bigint NOT NULL
);

CREATE TABLE public.sys_permission (
id bigint NOT NULL,
    perm_code character varying(128) NOT NULL,
    perm_name character varying(128) NOT NULL,
    perm_type character varying(16) NOT NULL,
    parent_id bigint,
    route_path character varying(256),
    page_key character varying(128),
    action_key character varying(128),
    sort_order integer DEFAULT 0 NOT NULL,
    is_enabled boolean DEFAULT true NOT NULL,
    remark text,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT sys_permission_perm_type_check CHECK (((perm_type)::text = ANY ((ARRAY['PAGE'::character varying, 'BUTTON'::character varying])::text[])))
);

CREATE TABLE public.sys_role (
id bigint NOT NULL,
    role_code character varying(64) NOT NULL,
    role_name character varying(128) NOT NULL,
    data_access_mode character varying(16) DEFAULT 'OWN_ORG'::character varying NOT NULL,
    is_enabled boolean DEFAULT true NOT NULL,
    remark text,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    updated_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL,
    CONSTRAINT sys_role_data_access_mode_check CHECK (((data_access_mode)::text = ANY ((ARRAY['ALL'::character varying, 'OWN_ORG'::character varying])::text[])))
);

CREATE TABLE public.sys_role_permission (
id bigint NOT NULL,
    role_id bigint NOT NULL,
    perm_id bigint NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE public.sys_user (
id bigint CONSTRAINT app_user_user_id_not_null NOT NULL,
    username character varying(50) CONSTRAINT app_user_username_not_null NOT NULL,
    real_name character varying(50) CONSTRAINT app_user_real_name_not_null NOT NULL,
    org_id bigint CONSTRAINT app_user_org_id_not_null NOT NULL,
    password_hash character varying(255) CONSTRAINT app_user_password_hash_not_null NOT NULL,
    sso_id character varying(100),
    is_active boolean DEFAULT true CONSTRAINT app_user_is_active_not_null NOT NULL,
    created_at timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP CONSTRAINT app_user_created_at_not_null NOT NULL,
    updated_at timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP CONSTRAINT app_user_updated_at_not_null NOT NULL
);

CREATE TABLE public.sys_user_role (
id bigint NOT NULL,
    user_id bigint NOT NULL,
    role_id bigint NOT NULL,
    created_at timestamp with time zone DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE TABLE public.sys_task (
id bigint CONSTRAINT sys_task_id_not_null NOT NULL,
    plan_id bigint CONSTRAINT sys_task_plan_id_not_null NOT NULL,
    name character varying(200) CONSTRAINT sys_task_name_not_null NOT NULL,
    "desc" text,
    type public.task_type DEFAULT 'BASIC'::public.task_type CONSTRAINT sys_task_type_not_null NOT NULL,
    sort_order integer DEFAULT 0 CONSTRAINT sys_task_sort_order_not_null NOT NULL,
    remark text,
    created_at timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP CONSTRAINT sys_task_created_at_not_null NOT NULL,
    updated_at timestamp(6) without time zone DEFAULT CURRENT_TIMESTAMP CONSTRAINT sys_task_updated_at_not_null NOT NULL,
    is_deleted boolean DEFAULT false CONSTRAINT sys_task_is_deleted_not_null NOT NULL
);

CREATE TABLE public.warn_level (
id bigint NOT NULL,
    level_code character varying(32) NOT NULL,
    level_name character varying(64) NOT NULL,
    severity integer NOT NULL,
    remark text,
    CONSTRAINT warn_level_severity_check CHECK ((severity >= 0))
);

CREATE TABLE public.warn_rule (
id bigint NOT NULL,
    metric_type character varying(16) NOT NULL,
    min_gap numeric(12,2) NOT NULL,
    max_gap numeric(12,2),
    level_id bigint NOT NULL,
    message_tpl character varying(500),
    sort_order integer DEFAULT 0 NOT NULL,
    CONSTRAINT ck_warn_gap CHECK (((max_gap IS NULL) OR (max_gap > min_gap))),
    CONSTRAINT warn_rule_metric_type_check CHECK (((metric_type)::text = ANY ((ARRAY['PROGRESS'::character varying, 'VALUE'::character varying])::text[])))
);

ALTER TABLE ONLY public.attachment ALTER COLUMN id SET DEFAULT nextval('public.attachment_id_seq'::regclass);

ALTER TABLE ONLY public.audit_action_log ALTER COLUMN id SET DEFAULT nextval('public.audit_action_log_id_seq'::regclass);

ALTER TABLE ONLY public.audit_flow_def ALTER COLUMN id SET DEFAULT nextval('public.audit_flow_def_id_seq'::regclass);

ALTER TABLE ONLY public.audit_instance ALTER COLUMN id SET DEFAULT nextval('public.audit_instance_id_seq'::regclass);

ALTER TABLE ONLY public.audit_step_def ALTER COLUMN id SET DEFAULT nextval('public.audit_step_def_id_seq'::regclass);

ALTER TABLE ONLY public.common_log ALTER COLUMN log_id SET DEFAULT nextval('public.common_log_id_seq'::regclass);

ALTER TABLE ONLY public.cycle ALTER COLUMN id SET DEFAULT nextval('public.cycle_id_seq'::regclass);

ALTER TABLE ONLY public.idempotency_records ALTER COLUMN id SET DEFAULT nextval('public.idempotency_records_id_seq'::regclass);

ALTER TABLE ONLY public.indicator ALTER COLUMN id SET DEFAULT nextval('public.indicator_id_seq'::regclass);

ALTER TABLE ONLY public.indicator_milestone ALTER COLUMN id SET DEFAULT nextval('public.indicator_milestone_id_seq'::regclass);

ALTER TABLE ONLY public.plan ALTER COLUMN id SET DEFAULT nextval('public.plan_id_seq'::regclass);

ALTER TABLE ONLY public.plan_report ALTER COLUMN id SET DEFAULT nextval('public.plan_report_id_seq'::regclass);

ALTER TABLE ONLY public.plan_report_indicator ALTER COLUMN id SET DEFAULT nextval('public.plan_report_indicator_id_seq'::regclass);

ALTER TABLE ONLY public.plan_report_indicator_attachment ALTER COLUMN id SET DEFAULT nextval('public.plan_report_indicator_attachment_id_seq'::regclass);

ALTER TABLE ONLY public.refresh_tokens ALTER COLUMN id SET DEFAULT nextval('public.refresh_tokens_id_seq'::regclass);

ALTER TABLE ONLY public.sys_org ALTER COLUMN id SET DEFAULT nextval('public.org_id_seq'::regclass);

ALTER TABLE ONLY public.sys_permission ALTER COLUMN id SET DEFAULT nextval('public.sys_permission_id_seq'::regclass);

ALTER TABLE ONLY public.sys_role ALTER COLUMN id SET DEFAULT nextval('public.sys_role_id_seq'::regclass);

ALTER TABLE ONLY public.sys_role_permission ALTER COLUMN id SET DEFAULT nextval('public.sys_role_permission_id_seq'::regclass);

ALTER TABLE ONLY public.sys_user ALTER COLUMN id SET DEFAULT nextval('public.user_id_seq'::regclass);

ALTER TABLE ONLY public.sys_user_role ALTER COLUMN id SET DEFAULT nextval('public.sys_user_role_id_seq'::regclass);

ALTER TABLE ONLY public.sys_task ALTER COLUMN id SET DEFAULT nextval('public.sys_task_id_seq'::regclass);

ALTER TABLE ONLY public.warn_level ALTER COLUMN id SET DEFAULT nextval('public.warn_level_id_seq'::regclass);

ALTER TABLE ONLY public.warn_rule ALTER COLUMN id SET DEFAULT nextval('public.warn_rule_id_seq'::regclass);

ALTER TABLE ONLY public.sys_user
    ADD CONSTRAINT app_user_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.sys_user
    ADD CONSTRAINT app_user_username_key UNIQUE (username);

ALTER TABLE ONLY public.cycle
    ADD CONSTRAINT assessment_cycle_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.attachment
    ADD CONSTRAINT attachment_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.audit_action_log
    ADD CONSTRAINT audit_action_log_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.audit_flow_def
    ADD CONSTRAINT audit_flow_def_flow_code_key UNIQUE (flow_code);

ALTER TABLE ONLY public.audit_flow_def
    ADD CONSTRAINT audit_flow_def_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.audit_instance
    ADD CONSTRAINT audit_instance_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.common_log
    ADD CONSTRAINT audit_log_pkey PRIMARY KEY (log_id);

ALTER TABLE ONLY public.audit_step_def
    ADD CONSTRAINT audit_step_def_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.idempotency_records
    ADD CONSTRAINT idempotency_records_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.indicator
    ADD CONSTRAINT indicator_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.indicator_milestone
    ADD CONSTRAINT milestone_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.sys_org
    ADD CONSTRAINT org_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.plan_report_indicator_attachment
    ADD CONSTRAINT plan_report_indicator_attachm_plan_report_indicator_id_atta_key UNIQUE (plan_report_indicator_id, attachment_id);

ALTER TABLE ONLY public.plan_report_indicator_attachment
    ADD CONSTRAINT plan_report_indicator_attachment_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.plan_report_indicator
    ADD CONSTRAINT plan_report_indicator_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.plan_report
    ADD CONSTRAINT plan_report_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT refresh_tokens_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.sys_task
    ADD CONSTRAINT sys_task_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.sys_permission
    ADD CONSTRAINT sys_permission_perm_code_key UNIQUE (perm_code);

ALTER TABLE ONLY public.sys_permission
    ADD CONSTRAINT sys_permission_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.sys_role_permission
    ADD CONSTRAINT sys_role_permission_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.sys_role
    ADD CONSTRAINT sys_role_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.sys_role
    ADD CONSTRAINT sys_role_role_code_key UNIQUE (role_code);

ALTER TABLE ONLY public.sys_user_role
    ADD CONSTRAINT sys_user_role_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.plan
    ADD CONSTRAINT task_plan_pk PRIMARY KEY (id);

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT uk_o2mlirhldriil2y7krapq4frt UNIQUE (token_hash);

ALTER TABLE ONLY public.idempotency_records
    ADD CONSTRAINT uk_ol0gjg0uap11mq1y9ug506f1i UNIQUE (idempotency_key);

ALTER TABLE ONLY public.audit_instance
    ADD CONSTRAINT uq_audit_instance_biz UNIQUE (biz_type, biz_id);

ALTER TABLE ONLY public.audit_step_def
    ADD CONSTRAINT uq_audit_step_flow_code UNIQUE (flow_id, step_code);

ALTER TABLE ONLY public.audit_step_def
    ADD CONSTRAINT uq_audit_step_flow_no UNIQUE (flow_id, step_no);

ALTER TABLE ONLY public.plan_report
    ADD CONSTRAINT uq_plan_report UNIQUE (plan_id, report_month, report_org_type, report_org_id);

ALTER TABLE ONLY public.plan_report_indicator
    ADD CONSTRAINT uq_report_indicator UNIQUE (report_id, indicator_id);

ALTER TABLE ONLY public.sys_role_permission
    ADD CONSTRAINT uq_sys_role_perm UNIQUE (role_id, perm_id);

ALTER TABLE ONLY public.sys_user_role
    ADD CONSTRAINT uq_sys_user_role UNIQUE (user_id, role_id);

ALTER TABLE ONLY public.warn_level
    ADD CONSTRAINT warn_level_level_code_key UNIQUE (level_code);

ALTER TABLE ONLY public.warn_level
    ADD CONSTRAINT warn_level_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.warn_rule
    ADD CONSTRAINT warn_rule_pkey PRIMARY KEY (id);

ALTER TABLE ONLY public.audit_action_log
    ADD CONSTRAINT audit_action_log_from_step_id_fkey FOREIGN KEY (from_step_id) REFERENCES public.audit_step_def(id);

ALTER TABLE ONLY public.audit_action_log
    ADD CONSTRAINT audit_action_log_instance_id_fkey FOREIGN KEY (instance_id) REFERENCES public.audit_instance(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.audit_action_log
    ADD CONSTRAINT audit_action_log_step_id_fkey FOREIGN KEY (step_id) REFERENCES public.audit_step_def(id);

ALTER TABLE ONLY public.audit_action_log
    ADD CONSTRAINT audit_action_log_to_step_id_fkey FOREIGN KEY (to_step_id) REFERENCES public.audit_step_def(id);

ALTER TABLE ONLY public.audit_instance
    ADD CONSTRAINT audit_instance_current_step_id_fkey FOREIGN KEY (current_step_id) REFERENCES public.audit_step_def(id);

ALTER TABLE ONLY public.audit_instance
    ADD CONSTRAINT audit_instance_flow_id_fkey FOREIGN KEY (flow_id) REFERENCES public.audit_flow_def(id);

ALTER TABLE ONLY public.common_log
    ADD CONSTRAINT audit_log_actor_org_id_fkey FOREIGN KEY (actor_org_id) REFERENCES public.sys_org(id);

ALTER TABLE ONLY public.common_log
    ADD CONSTRAINT audit_log_actor_user_id_fkey FOREIGN KEY (actor_user_id) REFERENCES public.sys_user(id);

ALTER TABLE ONLY public.audit_step_def
    ADD CONSTRAINT audit_step_def_flow_id_fkey FOREIGN KEY (flow_id) REFERENCES public.audit_flow_def(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.refresh_tokens
    ADD CONSTRAINT fk5a9ypl7oycxycfscqnsepj5t8 FOREIGN KEY (user_id) REFERENCES public.sys_user(id);

ALTER TABLE ONLY public.indicator
    ADD CONSTRAINT indicator_parent_indicator_id_fkey FOREIGN KEY (parent_indicator_id) REFERENCES public.indicator(id);

ALTER TABLE ONLY public.indicator
    ADD CONSTRAINT indicator_task_id_fkey FOREIGN KEY (task_id) REFERENCES public.sys_task(id);

ALTER TABLE ONLY public.indicator_milestone
    ADD CONSTRAINT milestone_indicator_id_fkey FOREIGN KEY (indicator_id) REFERENCES public.indicator(id);

ALTER TABLE ONLY public.sys_org
    ADD CONSTRAINT org_parent_org_id_fkey FOREIGN KEY (parent_org_id) REFERENCES public.sys_org(id);

ALTER TABLE ONLY public.plan_report_indicator_attachment
    ADD CONSTRAINT plan_report_indicator_attachment_attachment_id_fkey FOREIGN KEY (attachment_id) REFERENCES public.attachment(id);

ALTER TABLE ONLY public.plan_report_indicator_attachment
    ADD CONSTRAINT plan_report_indicator_attachment_plan_report_indicator_id_fkey FOREIGN KEY (plan_report_indicator_id) REFERENCES public.plan_report_indicator(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.plan_report_indicator
    ADD CONSTRAINT plan_report_indicator_report_id_fkey FOREIGN KEY (report_id) REFERENCES public.plan_report(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.sys_permission
    ADD CONSTRAINT sys_permission_parent_id_fkey FOREIGN KEY (parent_id) REFERENCES public.sys_permission(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.sys_role_permission
    ADD CONSTRAINT sys_role_permission_perm_id_fkey FOREIGN KEY (perm_id) REFERENCES public.sys_permission(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.sys_role_permission
    ADD CONSTRAINT sys_role_permission_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.sys_role(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.sys_user_role
    ADD CONSTRAINT sys_user_role_role_id_fkey FOREIGN KEY (role_id) REFERENCES public.sys_role(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.sys_user_role
    ADD CONSTRAINT sys_user_role_user_id_fkey FOREIGN KEY (user_id) REFERENCES public.sys_user(id) ON DELETE CASCADE;

ALTER TABLE ONLY public.warn_rule
    ADD CONSTRAINT warn_rule_level_id_fkey FOREIGN KEY (level_id) REFERENCES public.warn_level(id);

ALTER SEQUENCE public.attachment_id_seq OWNED BY public.attachment.id;

ALTER SEQUENCE public.audit_action_log_id_seq OWNED BY public.audit_action_log.id;

ALTER SEQUENCE public.audit_flow_def_id_seq OWNED BY public.audit_flow_def.id;

ALTER SEQUENCE public.audit_instance_id_seq OWNED BY public.audit_instance.id;

ALTER SEQUENCE public.audit_step_def_id_seq OWNED BY public.audit_step_def.id;

ALTER SEQUENCE public.common_log_id_seq OWNED BY public.common_log.log_id;

ALTER SEQUENCE public.cycle_id_seq OWNED BY public.cycle.id;

ALTER SEQUENCE public.idempotency_records_id_seq OWNED BY public.idempotency_records.id;

ALTER SEQUENCE public.indicator_id_seq OWNED BY public.indicator.id;

ALTER SEQUENCE public.indicator_milestone_id_seq OWNED BY public.indicator_milestone.id;

ALTER SEQUENCE public.org_id_seq OWNED BY public.sys_org.id;

ALTER SEQUENCE public.plan_id_seq OWNED BY public.plan.id;

ALTER SEQUENCE public.plan_report_id_seq OWNED BY public.plan_report.id;

ALTER SEQUENCE public.plan_report_indicator_attachment_id_seq OWNED BY public.plan_report_indicator_attachment.id;

ALTER SEQUENCE public.plan_report_indicator_id_seq OWNED BY public.plan_report_indicator.id;

ALTER SEQUENCE public.refresh_tokens_id_seq OWNED BY public.refresh_tokens.id;

ALTER SEQUENCE public.sys_permission_id_seq OWNED BY public.sys_permission.id;

ALTER SEQUENCE public.sys_role_id_seq OWNED BY public.sys_role.id;

ALTER SEQUENCE public.sys_role_permission_id_seq OWNED BY public.sys_role_permission.id;

ALTER SEQUENCE public.sys_user_role_id_seq OWNED BY public.sys_user_role.id;

ALTER SEQUENCE public.task_id_seq OWNED BY public.task.id;

ALTER SEQUENCE public.task_plan_assessment_cycle_id_seq OWNED BY public.plan.cycle_id;

ALTER SEQUENCE public.user_id_seq OWNED BY public.sys_user.id;

ALTER SEQUENCE public.warn_level_id_seq OWNED BY public.warn_level.id;

ALTER SEQUENCE public.warn_rule_id_seq OWNED BY public.warn_rule.id;
