-- Rebaseline Flyway history to the new V1 baseline.
-- Intended for databases whose physical schema already matches the current approved baseline.
-- This script recreates flyway_schema_history and seeds a single V1 baseline marker.

BEGIN;

DROP TABLE IF EXISTS public.flyway_schema_history;

CREATE TABLE public.flyway_schema_history (
    installed_rank integer NOT NULL,
    version character varying(50),
    description character varying(200) NOT NULL,
    type character varying(20) NOT NULL,
    script character varying(1000) NOT NULL,
    checksum integer,
    installed_by character varying(100) NOT NULL,
    installed_on timestamp without time zone DEFAULT now() NOT NULL,
    execution_time integer NOT NULL,
    success boolean NOT NULL
);

ALTER TABLE ONLY public.flyway_schema_history
    ADD CONSTRAINT flyway_schema_history_pk PRIMARY KEY (installed_rank);

CREATE INDEX flyway_schema_history_s_idx
    ON public.flyway_schema_history USING btree (success);

INSERT INTO public.flyway_schema_history (
    installed_rank,
    version,
    description,
    type,
    script,
    checksum,
    installed_by,
    execution_time,
    success
)
VALUES (
    1,
    '1',
    '<< Flyway Baseline >>',
    'BASELINE',
    '<< Flyway Baseline >>',
    NULL,
    current_user,
    0,
    true
);

COMMIT;
