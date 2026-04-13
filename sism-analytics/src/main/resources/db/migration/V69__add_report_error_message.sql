-- Add persistent failure message support for analytics reports.
ALTER TABLE public.progress_report
    ADD COLUMN IF NOT EXISTS error_message text;
