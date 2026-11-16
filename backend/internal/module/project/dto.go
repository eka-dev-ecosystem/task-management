package project

import "time"

// CreateProjectRequest payload for creating a project/module.
type CreateProjectRequest struct {
	Name        string `json:"name" validate:"required,min=2,max=100"`
	Color       string `json:"color" validate:"omitempty,max=30"`
	Description string `json:"description" validate:"omitempty,max=500"`
}

// UpdateProjectRequest payload for modifying a project/module.
type UpdateProjectRequest struct {
	Name        *string `json:"name" validate:"omitempty,min=2,max=100"`
	Color       *string `json:"color" validate:"omitempty,max=30"`
	Description *string `json:"description" validate:"omitempty,max=500"`
}

// ProjectResponse represents a single project entity with summary stats.
type ProjectResponse struct {
	ID                  int64     `json:"id" db:"id"`
	UserID              string    `json:"user_id" db:"user_id"`
	Name                string    `json:"name" db:"name"`
	Color               string    `json:"color" db:"color"`
	Description         string    `json:"description" db:"description"`
	TaskCount           int       `json:"task_count" db:"task_count"`
	TodoTaskCount       int       `json:"todo_task_count" db:"todo_task_count"`
	InProgressTaskCount int       `json:"in_progress_task_count" db:"in_progress_task_count"`
	ReviewTaskCount     int       `json:"review_task_count" db:"review_task_count"`
	BacklogTaskCount    int       `json:"backlog_task_count" db:"backlog_task_count"`
	CompletedTaskCount  int       `json:"completed_task_count" db:"completed_task_count"`
	CreatedAt           time.Time `json:"created_at" db:"created_at"`
	UpdatedAt           time.Time `json:"updated_at" db:"updated_at"`
}
