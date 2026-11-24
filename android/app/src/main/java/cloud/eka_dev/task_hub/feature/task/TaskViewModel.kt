package cloud.eka_dev.task_hub.feature.task

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.eka_dev.task_hub.core.model.*
import cloud.eka_dev.task_hub.core.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class TaskUiState(
    val isLoading: Boolean = false,
    val isLoadingMore: Boolean = false,
    val hasMore: Boolean = false,
    val nextCursor: Long? = null,
    val tasks: List<Task> = emptyList(),
    val selectedStatus: String? = null,
    val selectedProjectId: Long? = null,
    val searchQuery: String = "",
    val filterFocusOnly: Boolean = false,
    val error: String? = null,
    val selectedTask: Task? = null,
    val taskActivities: List<TaskActivity> = emptyList(),
    val isLoadingActivities: Boolean = false,
    val projects: List<Project> = emptyList(),
    val statusMetrics: TaskStatusMetrics? = null,
    val selectedTab: Int = 0, // 0 = Tasks, 1 = Recurring Templates
    val recurringTasks: List<RecurringTaskItem> = emptyList(),
    val isLoadingRecurring: Boolean = false,
    val recurringFilter: String = "all" // "all", "active", "paused"
)

class TaskViewModel(private val apiClient: ApiClient) : ViewModel() {

    private val _uiState = MutableStateFlow(TaskUiState())
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    val focusTasks: List<Task>
        get() = _uiState.value.tasks.filter { it.isFocus && it.status != "done" }

    val filteredRecurringTasks: List<RecurringTaskItem>
        get() = when (_uiState.value.recurringFilter) {
            "active" -> _uiState.value.recurringTasks.filter { it.isActive }
            "paused" -> _uiState.value.recurringTasks.filter { !it.isActive }
            else -> _uiState.value.recurringTasks
        }

    init {
        loadTasks(reset = true)
        loadProjects()
        loadRecurringTasks()
        loadTaskMetrics()
    }

    fun setSelectedTab(tab: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
        if (tab == 1 && _uiState.value.recurringTasks.isEmpty()) {
            loadRecurringTasks()
        }
    }

    fun setRecurringFilter(filter: String) {
        _uiState.value = _uiState.value.copy(recurringFilter = filter)
    }

    fun loadProjects() {
        viewModelScope.launch {
            try {
                val res = apiClient.apiService.getProjects()
                if (res.success && res.data != null) {
                    _uiState.value = _uiState.value.copy(projects = res.data)
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun loadTaskMetrics() {
        viewModelScope.launch {
            try {
                val res = apiClient.apiService.getTaskMetrics(projectId = _uiState.value.selectedProjectId)
                if (res.success && res.data != null) {
                    _uiState.value = _uiState.value.copy(statusMetrics = res.data)
                }
            } catch (e: Exception) {
                // ignore
            }
        }
    }

    fun loadTasks(reset: Boolean = true) {
        if (reset) {
            loadTaskMetrics()
        }
        viewModelScope.launch {
            if (reset) {
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    nextCursor = null,
                    hasMore = false,
                    error = null
                )
            }
            try {
                val res = apiClient.apiService.getTasksPaginated(
                    status = _uiState.value.selectedStatus,
                    projectId = _uiState.value.selectedProjectId,
                    search = _uiState.value.searchQuery.ifEmpty { null },
                    limit = 15,
                    cursor = if (reset) null else _uiState.value.nextCursor
                )
                if (res.success && res.data != null) {
                    val newItems = res.data.items
                    val combined = if (reset) newItems else (_uiState.value.tasks + newItems)
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        tasks = combined.distinctBy { it.id },
                        nextCursor = res.data.nextCursor,
                        hasMore = res.data.hasMore
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isLoadingMore = false,
                        error = res.message
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isLoadingMore = false,
                    error = e.localizedMessage ?: "Failed to load tasks"
                )
            }
        }
    }

    fun loadMoreTasks() {
        val state = _uiState.value
        if (state.isLoading || state.isLoadingMore || !state.hasMore || state.nextCursor == null) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingMore = true)
            try {
                val res = apiClient.apiService.getTasksPaginated(
                    status = state.selectedStatus,
                    projectId = state.selectedProjectId,
                    search = state.searchQuery.ifEmpty { null },
                    limit = 15,
                    cursor = state.nextCursor
                )
                if (res.success && res.data != null) {
                    val combined = state.tasks + res.data.items
                    _uiState.value = _uiState.value.copy(
                        isLoadingMore = false,
                        tasks = combined.distinctBy { it.id },
                        nextCursor = res.data.nextCursor,
                        hasMore = res.data.hasMore
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoadingMore = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingMore = false)
            }
        }
    }

    fun setStatusFilter(status: String?) {
        _uiState.value = _uiState.value.copy(selectedStatus = status)
        loadTasks(reset = true)
    }

    fun setProjectFilter(projectId: Long?) {
        _uiState.value = _uiState.value.copy(selectedProjectId = projectId)
        loadTasks(reset = true)
    }

    fun toggleFocusFilter() {
        _uiState.value = _uiState.value.copy(filterFocusOnly = !_uiState.value.filterFocusOnly)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        loadTasks(reset = true)
    }

    fun toggleTaskFocus(task: Task) {
        viewModelScope.launch {
            val updatedIsFocus = !task.isFocus
            val updatedTasks = _uiState.value.tasks.map {
                if (it.id == task.id) it.copy(isFocus = updatedIsFocus) else it
            }
            _uiState.value = _uiState.value.copy(tasks = updatedTasks)
            if (_uiState.value.selectedTask?.id == task.id) {
                _uiState.value = _uiState.value.copy(
                    selectedTask = _uiState.value.selectedTask?.copy(isFocus = updatedIsFocus)
                )
            }
            try {
                val res = apiClient.apiService.toggleTaskFocus(task.id)
                if (res.success && res.data != null) {
                    val finalTasks = _uiState.value.tasks.map {
                        if (it.id == task.id) res.data else it
                    }
                    _uiState.value = _uiState.value.copy(tasks = finalTasks)
                    if (_uiState.value.selectedTask?.id == task.id) {
                        _uiState.value = _uiState.value.copy(selectedTask = res.data)
                    }
                }
            } catch (e: Exception) {
                loadTasks()
            }
        }
    }

    fun toggleTaskStatus(task: Task) {
        viewModelScope.launch {
            val newStatus = if (task.status == "done") "todo" else "done"
            try {
                val res = apiClient.apiService.updateTask(task.id, UpdateTaskRequest(status = newStatus))
                if (res.success && res.data != null) {
                    if (_uiState.value.selectedTask?.id == task.id) {
                        _uiState.value = _uiState.value.copy(selectedTask = res.data)
                    }
                }
                loadTasks()
                loadProjects()
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun updateTaskStatus(task: Task, newStatus: String) {
        viewModelScope.launch {
            try {
                val updatedTasks = _uiState.value.tasks.map {
                    if (it.id == task.id) it.copy(status = newStatus) else it
                }
                _uiState.value = _uiState.value.copy(tasks = updatedTasks)
                if (_uiState.value.selectedTask?.id == task.id) {
                    _uiState.value = _uiState.value.copy(selectedTask = _uiState.value.selectedTask?.copy(status = newStatus))
                }

                val res = apiClient.apiService.updateTask(task.id, UpdateTaskRequest(status = newStatus))
                if (res.success && res.data != null) {
                    if (_uiState.value.selectedTask?.id == task.id) {
                        _uiState.value = _uiState.value.copy(selectedTask = res.data)
                    }
                }
                loadTasks()
                loadProjects()
            } catch (e: Exception) {
                loadTasks()
            }
        }
    }

    fun updateTask(
        id: Long,
        title: String? = null,
        description: String? = null,
        status: String? = null,
        priority: String? = null,
        dueDate: String? = null,
        clearDueDate: Boolean = false,
        isFocus: Boolean? = null,
        tags: List<String>? = null,
        projectId: Long? = null,
        clearProjectId: Boolean = false,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val res = apiClient.apiService.updateTask(
                    id,
                    UpdateTaskRequest(
                        title = title,
                        description = description,
                        status = status,
                        priority = priority,
                        dueDate = dueDate,
                        clearDueDate = clearDueDate,
                        isFocus = isFocus,
                        tags = tags,
                        projectId = projectId,
                        clearProjectId = clearProjectId
                    )
                )
                if (res.success && res.data != null) {
                    if (_uiState.value.selectedTask?.id == id) {
                        _uiState.value = _uiState.value.copy(selectedTask = res.data)
                    }
                    loadTasks()
                    loadProjects()
                    onSuccess?.invoke()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.localizedMessage ?: "Failed to update task"
                )
            }
        }
    }

    fun createTask(
        req: CreateTaskRequest,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val res = apiClient.apiService.createTask(req)
                if (res.success) {
                    loadTasks()
                    loadProjects()
                    onSuccess?.invoke()
                } else {
                    _uiState.value = _uiState.value.copy(error = res.message)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.localizedMessage)
            }
        }
    }

    fun createTask(
        title: String,
        description: String,
        priority: String,
        isFocus: Boolean = false,
        projectId: Long? = null
    ) {
        createTask(
            CreateTaskRequest(
                title = title,
                description = description,
                priority = priority,
                isFocus = isFocus,
                projectId = projectId
            )
        )
    }

    fun deleteTask(taskId: Long, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val res = apiClient.apiService.deleteTask(taskId)
                if (res.success) {
                    val updatedTasks = _uiState.value.tasks.filter { it.id != taskId }
                    _uiState.value = _uiState.value.copy(
                        tasks = updatedTasks,
                        selectedTask = if (_uiState.value.selectedTask?.id == taskId) null else _uiState.value.selectedTask
                    )
                    onSuccess?.invoke()
                    loadTasks()
                    loadProjects()
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun createSubtask(
        taskId: Long,
        title: String,
        description: String? = null,
        dueDate: String? = null,
        isSkippable: Boolean? = null
    ) {
        if (title.isBlank()) return
        viewModelScope.launch {
            try {
                val res = apiClient.apiService.createSubtask(
                    taskId,
                    CreateSubtaskRequest(
                        title = title.trim(),
                        description = description?.trim()?.ifEmpty { null },
                        dueDate = dueDate,
                        isSkippable = isSkippable
                    )
                )
                if (res.success && res.data != null) {
                    val currentTask = _uiState.value.selectedTask
                    if (currentTask != null && currentTask.id == taskId) {
                        val updatedSubtasks = currentTask.subtasks + res.data
                        val newCount = updatedSubtasks.size
                        val newCompleted = updatedSubtasks.count { it.isCompleted }
                        val newPercent = if (newCount > 0) (newCompleted * 100) / newCount else 0
                        val updatedTask = currentTask.copy(
                            subtasks = updatedSubtasks,
                            subtasksCount = newCount,
                            subtasksCompleted = newCompleted,
                            progressPercent = newPercent
                        )
                        _uiState.value = _uiState.value.copy(
                            selectedTask = updatedTask,
                            tasks = _uiState.value.tasks.map { if (it.id == taskId) updatedTask else it }
                        )
                    }
                    loadTasks()
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun updateSubtask(
        taskId: Long,
        subtaskId: Long,
        title: String? = null,
        description: String? = null,
        dueDate: String? = null,
        isCompleted: Boolean? = null,
        isCancelled: Boolean? = null,
        isSkippable: Boolean? = null
    ) {
        viewModelScope.launch {
            try {
                val res = apiClient.apiService.updateSubtask(
                    taskId,
                    subtaskId,
                    UpdateSubtaskRequest(
                        title = title,
                        description = description,
                        dueDate = dueDate,
                        isCompleted = isCompleted,
                        isCancelled = isCancelled,
                        isSkippable = isSkippable
                    )
                )
                if (res.success && res.data != null) {
                    val currentTask = _uiState.value.selectedTask
                    if (currentTask != null && currentTask.id == taskId) {
                        val updatedSubtasks = currentTask.subtasks.map {
                            if (it.id == subtaskId) res.data else it
                        }
                        val newCount = updatedSubtasks.size
                        val newCompleted = updatedSubtasks.count { it.isCompleted }
                        val newPercent = if (newCount > 0) (newCompleted * 100) / newCount else 0
                        val updatedTask = currentTask.copy(
                            subtasks = updatedSubtasks,
                            subtasksCount = newCount,
                            subtasksCompleted = newCompleted,
                            progressPercent = newPercent
                        )
                        _uiState.value = _uiState.value.copy(
                            selectedTask = updatedTask,
                            tasks = _uiState.value.tasks.map { if (it.id == taskId) updatedTask else it }
                        )
                    }
                    loadTasks()
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun deleteSubtask(taskId: Long, subtaskId: Long) {
        viewModelScope.launch {
            try {
                val res = apiClient.apiService.deleteSubtask(taskId, subtaskId)
                if (res.success) {
                    val currentTask = _uiState.value.selectedTask
                    if (currentTask != null && currentTask.id == taskId) {
                        val updatedSubtasks = currentTask.subtasks.filter { it.id != subtaskId }
                        val newCount = updatedSubtasks.size
                        val newCompleted = updatedSubtasks.count { it.isCompleted }
                        val newPercent = if (newCount > 0) (newCompleted * 100) / newCount else 0
                        val updatedTask = currentTask.copy(
                            subtasks = updatedSubtasks,
                            subtasksCount = newCount,
                            subtasksCompleted = newCompleted,
                            progressPercent = newPercent
                        )
                        _uiState.value = _uiState.value.copy(
                            selectedTask = updatedTask,
                            tasks = _uiState.value.tasks.map { if (it.id == taskId) updatedTask else it }
                        )
                    }
                    loadTasks()
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun toggleSubtask(taskId: Long, subtaskId: Long, currentCompleted: Boolean) {
        viewModelScope.launch {
            try {
                apiClient.apiService.updateSubtask(
                    taskId,
                    subtaskId,
                    UpdateSubtaskRequest(isCompleted = !currentCompleted)
                )
                loadTasks()
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun toggleSubtaskCancelled(taskId: Long, subtaskId: Long, currentCancelled: Boolean) {
        viewModelScope.launch {
            try {
                apiClient.apiService.updateSubtask(
                    taskId,
                    subtaskId,
                    UpdateSubtaskRequest(isCancelled = !currentCancelled)
                )
                loadTasks()
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun selectTask(task: Task?) {
        _uiState.value = _uiState.value.copy(selectedTask = task, taskActivities = emptyList())
        if (task != null) {
            loadActivities(task.id)
        }
    }

    fun selectTaskById(taskId: Long) {
        viewModelScope.launch {
            val existing = _uiState.value.tasks.find { it.id == taskId }
            if (existing != null) {
                selectTask(existing)
            } else {
                try {
                    val res = apiClient.apiService.getTaskById(taskId)
                    if (res.success && res.data != null) {
                        selectTask(res.data)
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun applyFilters(
        status: String? = null,
        projectId: Long? = null,
        search: String? = null,
        focusOnly: Boolean? = null
    ) {
        var state = _uiState.value
        if (status != null) state = state.copy(selectedStatus = if (status.isEmpty() || status.lowercase() == "all") null else status)
        if (projectId != null) state = state.copy(selectedProjectId = projectId)
        if (search != null) state = state.copy(searchQuery = search)
        if (focusOnly != null) state = state.copy(filterFocusOnly = focusOnly)
        _uiState.value = state
        loadTasks(reset = true)
    }

    fun loadActivities(taskId: Long) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingActivities = true)
            try {
                val res = apiClient.apiService.getTaskActivities(taskId)
                if (res.success && res.data != null) {
                    _uiState.value = _uiState.value.copy(
                        isLoadingActivities = false,
                        taskActivities = res.data
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoadingActivities = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingActivities = false)
            }
        }
    }

    fun addActivityNote(taskId: Long, note: String) {
        if (note.isBlank()) return
        viewModelScope.launch {
            try {
                val res = apiClient.apiService.createTaskActivity(
                    taskId,
                    CreateTaskActivityRequest(description = note.trim())
                )
                if (res.success && res.data != null) {
                    val currentList = _uiState.value.taskActivities
                    _uiState.value = _uiState.value.copy(
                        taskActivities = listOf(res.data) + currentList
                    )
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    // ==========================================
    // Recurring Tasks Management
    // ==========================================

    fun loadRecurringTasks() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoadingRecurring = true)
            try {
                val res = apiClient.apiService.getRecurringTasks()
                if (res.success && res.data != null) {
                    _uiState.value = _uiState.value.copy(
                        recurringTasks = res.data,
                        isLoadingRecurring = false
                    )
                } else {
                    _uiState.value = _uiState.value.copy(isLoadingRecurring = false)
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(isLoadingRecurring = false)
            }
        }
    }

    fun createRecurringTask(
        req: CreateRecurringTaskRequest,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val res = apiClient.apiService.createRecurringTask(req)
                if (res.success && res.data != null) {
                    val current = _uiState.value.recurringTasks
                    _uiState.value = _uiState.value.copy(recurringTasks = listOf(res.data) + current)
                    onSuccess?.invoke()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.localizedMessage)
            }
        }
    }

    fun updateRecurringTask(
        id: Long,
        req: UpdateRecurringTaskRequest,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val res = apiClient.apiService.updateRecurringTask(id, req)
                if (res.success && res.data != null) {
                    val updated = _uiState.value.recurringTasks.map {
                        if (it.id == id) res.data else it
                    }
                    _uiState.value = _uiState.value.copy(recurringTasks = updated)
                    onSuccess?.invoke()
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(error = e.localizedMessage)
            }
        }
    }

    fun deleteRecurringTask(
        id: Long,
        onSuccess: (() -> Unit)? = null
    ) {
        viewModelScope.launch {
            try {
                val res = apiClient.apiService.deleteRecurringTask(id)
                if (res.success) {
                    val updated = _uiState.value.recurringTasks.filter { it.id != id }
                    _uiState.value = _uiState.value.copy(recurringTasks = updated)
                    onSuccess?.invoke()
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun toggleActiveRecurringTask(id: Long) {
        viewModelScope.launch {
            try {
                val res = apiClient.apiService.toggleActiveRecurringTask(id)
                if (res.success && res.data != null) {
                    val updated = _uiState.value.recurringTasks.map {
                        if (it.id == id) res.data else it
                    }
                    _uiState.value = _uiState.value.copy(recurringTasks = updated)
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }
}
