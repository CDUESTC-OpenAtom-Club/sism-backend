-- Keep indicator.year aligned with the authoritative cycle.year carried by sys_task.cycle_id.

UPDATE public.indicator AS i
SET year = c.year
FROM public.sys_task AS t
JOIN public.cycle AS c ON c.id = t.cycle_id
WHERE i.task_id = t.task_id
  AND i.year IS DISTINCT FROM c.year;

CREATE OR REPLACE FUNCTION public.resolve_indicator_year_from_task(p_task_id BIGINT)
RETURNS INTEGER
LANGUAGE SQL
STABLE
AS $$
    SELECT c.year
    FROM public.sys_task t
    JOIN public.cycle c ON c.id = t.cycle_id
    WHERE t.task_id = p_task_id
    LIMIT 1;
$$;

CREATE OR REPLACE FUNCTION public.sync_indicator_year_from_task()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    IF NEW.task_id IS NULL THEN
        RETURN NEW;
    END IF;

    NEW.year := public.resolve_indicator_year_from_task(NEW.task_id);
    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_indicator_sync_year_from_task ON public.indicator;

CREATE TRIGGER trg_indicator_sync_year_from_task
BEFORE INSERT OR UPDATE OF task_id
ON public.indicator
FOR EACH ROW
EXECUTE FUNCTION public.sync_indicator_year_from_task();

CREATE OR REPLACE FUNCTION public.refresh_indicator_years_for_task()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE public.indicator
    SET year = public.resolve_indicator_year_from_task(NEW.task_id)
    WHERE task_id = NEW.task_id;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_sys_task_refresh_indicator_year ON public.sys_task;

CREATE TRIGGER trg_sys_task_refresh_indicator_year
AFTER INSERT OR UPDATE OF cycle_id
ON public.sys_task
FOR EACH ROW
EXECUTE FUNCTION public.refresh_indicator_years_for_task();

CREATE OR REPLACE FUNCTION public.refresh_indicator_years_for_cycle()
RETURNS TRIGGER
LANGUAGE plpgsql
AS $$
BEGIN
    UPDATE public.indicator AS i
    SET year = NEW.year
    FROM public.sys_task AS t
    WHERE i.task_id = t.task_id
      AND t.cycle_id = NEW.id;

    RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS trg_cycle_refresh_indicator_year ON public.cycle;

CREATE TRIGGER trg_cycle_refresh_indicator_year
AFTER UPDATE OF year
ON public.cycle
FOR EACH ROW
EXECUTE FUNCTION public.refresh_indicator_years_for_cycle();

COMMENT ON COLUMN public.indicator.year IS '指标所属年份；与 sys_task.cycle_id -> cycle.year 保持同步';
