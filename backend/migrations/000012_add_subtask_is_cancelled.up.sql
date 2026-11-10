-- Add is_cancelled column to subtasks table
ALTER TABLE subtasks ADD COLUMN IF NOT EXISTS is_cancelled BOOLEAN NOT NULL DEFAULT FALSE;

-- Index for queries filtering active vs cancelled subtasks
CREATE INDEX IF NOT EXISTS idx_subtasks_task_cancelled ON subtasks(task_id, is_cancelled);

