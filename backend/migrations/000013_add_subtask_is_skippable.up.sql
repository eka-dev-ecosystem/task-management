-- Add is_skippable column to subtasks table (default TRUE for backward compatibility)
ALTER TABLE subtasks ADD COLUMN IF NOT EXISTS is_skippable BOOLEAN NOT NULL DEFAULT TRUE;

-- Add is_skippable column to recurring_subtasks template table (default TRUE)
ALTER TABLE recurring_subtasks ADD COLUMN IF NOT EXISTS is_skippable BOOLEAN NOT NULL DEFAULT TRUE;
