DROP INDEX IF EXISTS idx_subtasks_task_cancelled;
ALTER TABLE subtasks DROP COLUMN IF EXISTS is_cancelled;
