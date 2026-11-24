-- Remove is_skippable column from subtasks and recurring_subtasks tables
ALTER TABLE subtasks DROP COLUMN IF EXISTS is_skippable;
ALTER TABLE recurring_subtasks DROP COLUMN IF EXISTS is_skippable;
