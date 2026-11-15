package cloud.eka_dev.task_hub.feature.task

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cloud.eka_dev.task_hub.R
import cloud.eka_dev.task_hub.core.designsystem.*
import cloud.eka_dev.task_hub.core.model.*
import java.util.Calendar

private fun showDatePicker(
    context: Context,
    initialDate: String?,
    onDateSelected: (String) -> Unit
) {
    val cal = Calendar.getInstance()
    if (!initialDate.isNullOrBlank()) {
        try {
            val parts = initialDate.split("-")
            if (parts.size >= 3) {
                cal.set(Calendar.YEAR, parts[0].toInt())
                cal.set(Calendar.MONTH, parts[1].toInt() - 1)
                cal.set(Calendar.DAY_OF_MONTH, parts[2].substring(0, 2).toInt())
            }
        } catch (_: Exception) {}
    }
    android.app.DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val formatted = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
            onDateSelected(formatted)
        },
        cal.get(Calendar.YEAR),
        cal.get(Calendar.MONTH),
        cal.get(Calendar.DAY_OF_MONTH)
    ).show()
}

private fun showTimePicker(
    context: Context,
    initialTime: String?,
    onTimeSelected: (String) -> Unit
) {
    val cal = Calendar.getInstance()
    var hour = 9
    var minute = 0
    if (!initialTime.isNullOrBlank()) {
        try {
            val parts = initialTime.split(":")
            if (parts.size >= 2) {
                hour = parts[0].toInt()
                minute = parts[1].toInt()
            }
        } catch (_: Exception) {}
    }
    android.app.TimePickerDialog(
        context,
        { _, selectedHour, selectedMinute ->
            val formatted = String.format("%02d:%02d", selectedHour, selectedMinute)
            onTimeSelected(formatted)
        },
        hour,
        minute,
        true
    ).show()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskListScreen(
    viewModel: TaskViewModel,
    onTaskClick: (Long) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    var showCreateDialog by remember { mutableStateOf(false) }
    var showCreateRecurringDialog by remember { mutableStateOf(false) }
    var editingRecurringItem by remember { mutableStateOf<RecurringTaskItem?>(null) }
    var deletingRecurringItem by remember { mutableStateOf<RecurringTaskItem?>(null) }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(MaterialTheme.colorScheme.background)) {
                TopAppBar(
                    title = {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            BrandLogo(size = 28.dp)
                            Text(
                                stringResource(R.string.task_list_title),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background
                    )
                )

                // Tab Switcher (Tasks vs Recurring)
                TabRow(
                    selectedTabIndex = uiState.selectedTab,
                    containerColor = MaterialTheme.colorScheme.background,
                    contentColor = BrandPrimary,
                    divider = { Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)) }
                ) {
                    Tab(
                        selected = uiState.selectedTab == 0,
                        onClick = { viewModel.setSelectedTab(0) },
                        text = {
                            Text(
                                stringResource(R.string.tab_tasks),
                                fontWeight = if (uiState.selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                    Tab(
                        selected = uiState.selectedTab == 1,
                        onClick = { viewModel.setSelectedTab(1) },
                        text = {
                            Text(
                                stringResource(R.string.tab_recurring),
                                fontWeight = if (uiState.selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                            )
                        }
                    )
                }
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    if (uiState.selectedTab == 0) {
                        showCreateDialog = true
                    } else {
                        showCreateRecurringDialog = true
                    }
                },
                containerColor = BrandPrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = if (uiState.selectedTab == 0) stringResource(R.string.create_task) else stringResource(R.string.create_recurring_title)
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            Spacer(modifier = Modifier.height(10.dp))

            if (uiState.selectedTab == 0) {
                // ==========================================
                // Normal Tasks Tab Content
                // ==========================================

                // Search Input
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    placeholder = { Text(stringResource(R.string.search_tasks_hint), fontSize = 14.sp) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = BrandPrimary) },
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    )
                )

                Spacer(modifier = Modifier.height(10.dp))

                // Filter Chips Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilterChip(
                        selected = uiState.selectedStatus == null && uiState.selectedProjectId == null,
                        onClick = {
                            viewModel.setStatusFilter(null)
                            viewModel.setProjectFilter(null)
                        },
                        leadingIcon = { Icon(Icons.Default.Inbox, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        label = { Text(stringResource(R.string.filter_all)) }
                    )
                    FilterChip(
                        selected = uiState.selectedStatus == "todo",
                        onClick = { viewModel.setStatusFilter(if (uiState.selectedStatus == "todo") null else "todo") },
                        leadingIcon = { Icon(Icons.Default.RadioButtonUnchecked, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        label = { Text(stringResource(R.string.filter_todo)) }
                    )
                    FilterChip(
                        selected = uiState.selectedStatus == "in_progress",
                        onClick = { viewModel.setStatusFilter(if (uiState.selectedStatus == "in_progress") null else "in_progress") },
                        leadingIcon = { Icon(Icons.Default.Sync, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        label = { Text(stringResource(R.string.filter_in_progress)) }
                    )
                    FilterChip(
                        selected = uiState.selectedStatus == "review",
                        onClick = { viewModel.setStatusFilter(if (uiState.selectedStatus == "review") null else "review") },
                        leadingIcon = { Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        label = { Text(stringResource(R.string.filter_review)) }
                    )
                    FilterChip(
                        selected = uiState.selectedStatus == "done",
                        onClick = { viewModel.setStatusFilter(if (uiState.selectedStatus == "done") null else "done") },
                        leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(14.dp)) },
                        label = { Text(stringResource(R.string.filter_done)) }
                    )
                    FilterChip(
                        selected = uiState.filterFocusOnly,
                        onClick = { viewModel.toggleFocusFilter() },
                        leadingIcon = {
                            Icon(
                                if (uiState.filterFocusOnly) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = null,
                                tint = if (uiState.filterFocusOnly) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(14.dp)
                            )
                        },
                        label = {
                            val count = viewModel.focusTasks.size
                            Text(if (count > 0) stringResource(R.string.filter_focus_count, count) else stringResource(R.string.filter_focus))
                        }
                    )

                    // Project Filter Chips
                    uiState.projects.forEach { proj ->
                        val projColor = Color(android.graphics.Color.parseColor(proj.color.ifEmpty { "#3B82F6" }))
                        FilterChip(
                            selected = uiState.selectedProjectId == proj.id,
                            onClick = {
                                viewModel.setProjectFilter(if (uiState.selectedProjectId == proj.id) null else proj.id)
                            },
                            leadingIcon = {
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .clip(CircleShape)
                                        .background(projColor)
                                )
                            },
                            label = { Text(proj.name, fontSize = 11.sp) }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Task List Content
                if (uiState.isLoading) {
                    TaskListSkeleton(count = 6)
                } else if (uiState.tasks.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(BrandPrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Checklist,
                                    contentDescription = null,
                                    tint = BrandPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                stringResource(R.string.empty_tasks),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                stringResource(R.string.empty_tasks_sub),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    val listState = rememberLazyListState()

                    val displayedTasks = remember(uiState.tasks, uiState.filterFocusOnly) {
                        if (uiState.filterFocusOnly) {
                            uiState.tasks.filter { it.isFocus }
                        } else {
                            uiState.tasks
                        }
                    }

                    val shouldLoadMore = remember {
                        derivedStateOf {
                            val total = listState.layoutInfo.totalItemsCount
                            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
                            total > 0 && lastVisible >= total - 3
                        }
                    }

                    LaunchedEffect(shouldLoadMore.value) {
                        if (shouldLoadMore.value && uiState.hasMore && !uiState.isLoadingMore && !uiState.filterFocusOnly) {
                            viewModel.loadMoreTasks()
                        }
                    }

                    LazyColumn(
                        state = listState,
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        // Today's Focus Horizon Banner
                        if (viewModel.focusTasks.isNotEmpty() && !uiState.filterFocusOnly) {
                            item {
                                TaskFocusHorizonBanner(
                                    focusTasks = viewModel.focusTasks,
                                    onTaskClick = { task ->
                                        viewModel.selectTask(task)
                                        onTaskClick(task.id)
                                    },
                                    onFocusToggle = { task -> viewModel.toggleTaskFocus(task) }
                                )
                            }
                        }

                        items(displayedTasks, key = { it.id }) { task ->
                            TaskItemCard(
                                task = task,
                                onTaskClick = {
                                    viewModel.selectTask(task)
                                    onTaskClick(task.id)
                                },
                                onStatusToggle = { viewModel.toggleTaskStatus(task) },
                                onFocusToggle = { viewModel.toggleTaskFocus(task) },
                                onStatusChange = { newStatus -> viewModel.updateTaskStatus(task, newStatus) }
                            )
                        }

                        if (uiState.isLoadingMore && !uiState.filterFocusOnly) {
                            item {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(24.dp),
                                        strokeWidth = 2.dp,
                                        color = BrandPrimary
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // ==========================================
                // Recurring Templates Tab Content
                // ==========================================

                // Recurring Filter Chips
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = uiState.recurringFilter == "all",
                        onClick = { viewModel.setRecurringFilter("all") },
                        label = { Text(stringResource(R.string.filter_all)) }
                    )
                    FilterChip(
                        selected = uiState.recurringFilter == "active",
                        onClick = { viewModel.setRecurringFilter("active") },
                        label = { Text(stringResource(R.string.recurring_active)) }
                    )
                    FilterChip(
                        selected = uiState.recurringFilter == "paused",
                        onClick = { viewModel.setRecurringFilter("paused") },
                        label = { Text(stringResource(R.string.recurring_paused)) }
                    )
                }

                Spacer(modifier = Modifier.height(14.dp))

                val recurringItems = viewModel.filteredRecurringTasks

                if (uiState.isLoadingRecurring) {
                    TaskListSkeleton(count = 4)
                } else if (recurringItems.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .clip(CircleShape)
                                    .background(BrandPrimary.copy(alpha = 0.12f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.Repeat,
                                    contentDescription = null,
                                    tint = BrandPrimary,
                                    modifier = Modifier.size(32.dp)
                                )
                            }
                            Text(
                                stringResource(R.string.recurring_empty_title),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                stringResource(R.string.recurring_empty_desc),
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                } else {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(recurringItems, key = { it.id }) { item ->
                            RecurringTaskCard(
                                item = item,
                                onEdit = { editingRecurringItem = item },
                                onToggleActive = { viewModel.toggleActiveRecurringTask(item.id) },
                                onDelete = { deletingRecurringItem = item }
                            )
                        }
                    }
                }
            }
        }
    }

    // Task Detail Bottom Sheet
    uiState.selectedTask?.let { task ->
        TaskDetailBottomSheet(
            task = task,
            viewModel = viewModel,
            onDismiss = { viewModel.selectTask(null) }
        )
    }

    // Create Task Dialog
    if (showCreateDialog) {
        CreateTaskDialog(
            projects = uiState.projects,
            onDismiss = { showCreateDialog = false },
            onCreate = { req ->
                viewModel.createTask(req) {
                    showCreateDialog = false
                }
            }
        )
    }

    // Create Recurring Template Dialog
    if (showCreateRecurringDialog) {
        CreateRecurringTaskDialog(
            projects = uiState.projects,
            onDismiss = { showCreateRecurringDialog = false },
            onCreate = { req ->
                viewModel.createRecurringTask(req) {
                    showCreateRecurringDialog = false
                }
            }
        )
    }

    // Edit Recurring Template Dialog
    editingRecurringItem?.let { item ->
        EditRecurringTaskDialog(
            item = item,
            projects = uiState.projects,
            onDismiss = { editingRecurringItem = null },
            onSave = { req ->
                viewModel.updateRecurringTask(item.id, req) {
                    editingRecurringItem = null
                }
            },
            onDelete = {
                viewModel.deleteRecurringTask(item.id) {
                    editingRecurringItem = null
                }
            }
        )
    }

    // Delete Recurring Template Confirmation Alert
    deletingRecurringItem?.let { item ->
        AlertDialog(
            onDismissRequest = { deletingRecurringItem = null },
            title = { Text(stringResource(R.string.delete_recurring_confirm_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.delete_recurring_confirm_desc, item.title)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteRecurringTask(item.id) {
                            deletingRecurringItem = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingRecurringItem = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

// ==========================================
// Recurring Task Card
// ==========================================

@Composable
fun RecurringTaskCard(
    item: RecurringTaskItem,
    onEdit: () -> Unit,
    onToggleActive: () -> Unit,
    onDelete: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // Badges Row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
            ) {
                // Recurrence Badge
                val patternText = when (item.recurrencePattern) {
                    "daily" -> if (item.recurrenceInterval > 1) "${item.recurrenceInterval} ${stringResource(R.string.unit_days)}" else stringResource(R.string.pattern_daily)
                    "weekdays" -> stringResource(R.string.pattern_weekdays)
                    "weekly" -> if (item.recurrenceInterval > 1) "${item.recurrenceInterval} ${stringResource(R.string.unit_weeks)}" else stringResource(R.string.pattern_weekly)
                    "monthly" -> if (item.recurrenceInterval > 1) "${item.recurrenceInterval} ${stringResource(R.string.unit_months)}" else stringResource(R.string.pattern_monthly)
                    else -> item.recurrencePattern.replaceFirstChar { it.uppercase() }
                }
                Surface(
                    color = BrandPrimary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(Icons.Default.Repeat, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(12.dp))
                        Text(patternText, fontSize = 11.sp, fontWeight = FontWeight.Bold, color = BrandPrimary)
                    }
                }

                // Due Time Badge (Formatted as HH:mm)
                if (!item.dueTime.isNullOrBlank()) {
                    val formattedTime = if (item.dueTime.length >= 5) item.dueTime.substring(0, 5) else item.dueTime
                    Surface(
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.08f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formattedTime, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }

                // Priority Badge
                val (pColor, pLabel) = when (item.priority.lowercase()) {
                    "urgent" -> PriorityUrgent to stringResource(R.string.priority_urgent)
                    "high" -> PriorityHigh to stringResource(R.string.priority_high)
                    "medium" -> PriorityMedium to stringResource(R.string.priority_medium)
                    else -> PriorityLow to stringResource(R.string.priority_low)
                }
                Surface(
                    color = pColor.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        pLabel.uppercase(),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = pColor
                    )
                }

                // Project Badge
                if (!item.projectName.isNullOrEmpty()) {
                    ProjectPill(name = item.projectName, colorHex = item.projectColor)
                }

                // Active / Paused Badge
                Surface(
                    color = if (item.isActive) StatusDone.copy(alpha = 0.15f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.15f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        if (item.isActive) stringResource(R.string.recurring_active) else stringResource(R.string.recurring_paused),
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (item.isActive) StatusDone else MaterialTheme.colorScheme.outline
                    )
                }
            }

            // Title & Description
            Text(
                item.title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            if (item.description.isNotBlank()) {
                Text(
                    item.description,
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2
                )
            }

            // Subtasks preview
            if (item.subtasks.isNotEmpty()) {
                Text(
                    stringResource(R.string.subtasks_count, item.subtasks.size, item.subtasks.size),
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f))

            // Action Buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                TextButton(
                    onClick = onToggleActive,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = if (item.isActive) Color(0xFFEAB308) else StatusDone
                    )
                ) {
                    Icon(
                        if (item.isActive) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        if (item.isActive) stringResource(R.string.pause) else stringResource(R.string.resume),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = stringResource(R.string.edit),
                            modifier = Modifier.size(16.dp),
                            tint = BrandPrimary
                        )
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(
                            Icons.Default.DeleteOutline,
                            contentDescription = stringResource(R.string.delete),
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// Comprehensive Create Task Dialog / Sheet
// ==========================================

@Composable
fun CreateTaskDialog(
    projects: List<Project>,
    onDismiss: () -> Unit,
    onCreate: (CreateTaskRequest) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("todo") }
    var priority by remember { mutableStateOf("medium") }
    var selectedProjectId by remember { mutableStateOf<Long?>(null) }
    var isFocus by remember { mutableStateOf(false) }
    var tagsText by remember { mutableStateOf("") }

    // Due date
    var hasDueDate by remember { mutableStateOf(false) }
    var dueDateDate by remember { mutableStateOf("") }
    var dueDateTime by remember { mutableStateOf("23:59") }

    // Subtasks
    val subtasks = remember { mutableStateListOf<CreateTaskSubtaskItem>() }
    var newSubtaskTitle by remember { mutableStateOf("") }
    var newSubtaskDesc by remember { mutableStateOf("") }
    var showSubtaskOptions by remember { mutableStateOf(false) }
    var hasSubtaskDueDate by remember { mutableStateOf(false) }
    var subtaskDueDateDate by remember { mutableStateOf("") }
    var subtaskDueDateTime by remember { mutableStateOf("17:00") }

    val fullTaskDueDate = if (hasDueDate && dueDateDate.isNotBlank()) "${dueDateDate}T${dueDateTime}:00Z" else null
    val fullSubtaskDueDate = if (hasSubtaskDueDate && subtaskDueDateDate.isNotBlank()) "${subtaskDueDateDate}T${subtaskDueDateTime}:00Z" else null
    val isSubtaskExceeding = fullTaskDueDate != null && fullSubtaskDueDate != null && fullSubtaskDueDate > fullTaskDueDate

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_task), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Title
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.task_title_label)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                // Description
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.task_desc_label)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                // Status Chips
                Text(stringResource(R.string.status_header), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("todo", "in_progress", "review", "done").forEach { st ->
                        val label = when (st) {
                            "todo" -> stringResource(R.string.filter_todo)
                            "in_progress" -> stringResource(R.string.filter_in_progress)
                            "review" -> stringResource(R.string.filter_review)
                            "done" -> stringResource(R.string.filter_done)
                            else -> st
                        }
                        FilterChip(
                            selected = status == st,
                            onClick = { status = st },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                // Priority Chips
                Text(stringResource(R.string.priority_label), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("low", "medium", "high", "urgent").forEach { p ->
                        val pLabel = when (p) {
                            "low" -> stringResource(R.string.priority_low)
                            "medium" -> stringResource(R.string.priority_medium)
                            "high" -> stringResource(R.string.priority_high)
                            "urgent" -> stringResource(R.string.priority_urgent)
                            else -> p
                        }
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(pLabel, fontSize = 11.sp) }
                        )
                    }
                }

                // Project Selector
                if (projects.isNotEmpty()) {
                    Text(stringResource(R.string.project_label), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedProjectId == null,
                            onClick = { selectedProjectId = null },
                            label = { Text(stringResource(R.string.no_project), fontSize = 11.sp) }
                        )
                        projects.forEach { proj ->
                            val projColor = Color(android.graphics.Color.parseColor(proj.color.ifEmpty { "#3B82F6" }))
                            FilterChip(
                                selected = selectedProjectId == proj.id,
                                onClick = { selectedProjectId = proj.id },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(projColor)
                                    )
                                },
                                label = { Text(proj.name, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Due Date Toggle & Pickers
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.due_date_label), fontSize = 13.sp)
                    Switch(checked = hasDueDate, onCheckedChange = { hasDueDate = it })
                }
                if (hasDueDate) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                showDatePicker(context, dueDateDate) { dueDateDate = it }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(dueDateDate.ifEmpty { "Pilih Tanggal" }, fontSize = 12.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                showTimePicker(context, dueDateTime) { dueDateTime = it }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(dueDateTime, fontSize = 12.sp)
                        }
                    }
                }

                // Focus Today
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.focus_today), fontSize = 13.sp)
                    Switch(checked = isFocus, onCheckedChange = { isFocus = it })
                }

                // Tags Input
                OutlinedTextField(
                    value = tagsText,
                    onValueChange = { tagsText = it },
                    label = { Text(stringResource(R.string.tags_label)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                // Subtasks Section
                Text(
                    stringResource(R.string.subtasks_header, subtasks.size, subtasks.size),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )

                // Subtask list items
                subtasks.forEachIndexed { idx, st ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(st.title, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                            if (!st.description.isNullOrBlank()) {
                                Text(st.description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                            if (!st.dueDate.isNullOrBlank()) {
                                Text(st.dueDate, fontSize = 10.sp, color = BrandPrimary)
                            }
                        }
                        IconButton(onClick = { subtasks.removeAt(idx) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                // Add Subtask Form
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        OutlinedTextField(
                            value = newSubtaskTitle,
                            onValueChange = { newSubtaskTitle = it },
                            placeholder = { Text(stringResource(R.string.add_subtask_placeholder), fontSize = 12.sp) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        IconButton(
                            onClick = {
                                if (newSubtaskTitle.isNotBlank()) {
                                    subtasks.add(
                                        CreateTaskSubtaskItem(
                                            title = newSubtaskTitle.trim(),
                                            description = newSubtaskDesc.trim().ifEmpty { null },
                                            dueDate = fullSubtaskDueDate
                                        )
                                    )
                                    newSubtaskTitle = ""
                                    newSubtaskDesc = ""
                                    hasSubtaskDueDate = false
                                    subtaskDueDateDate = ""
                                    showSubtaskOptions = false
                                }
                            },
                            enabled = newSubtaskTitle.isNotBlank()
                        ) {
                            Icon(Icons.Default.AddCircle, contentDescription = null, tint = BrandPrimary)
                        }
                    }

                    // Toggle Subtask Options (Desc & Deadline)
                    TextButton(
                        onClick = { showSubtaskOptions = !showSubtaskOptions },
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(
                            if (showSubtaskOptions) "Sembunyikan Opsi Detail" else "+ Tambah Deskripsi & Deadline Subtask",
                            fontSize = 11.sp,
                            color = BrandPrimary
                        )
                    }

                    if (showSubtaskOptions) {
                        OutlinedTextField(
                            value = newSubtaskDesc,
                            onValueChange = { newSubtaskDesc = it },
                            label = { Text(stringResource(R.string.subtask_description_label), fontSize = 11.sp) },
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 2
                        )

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(stringResource(R.string.subtask_due_date_label), fontSize = 11.sp)
                            Switch(
                                checked = hasSubtaskDueDate,
                                onCheckedChange = { hasSubtaskDueDate = it },
                                modifier = Modifier.scale(0.8f)
                            )
                        }

                        if (hasSubtaskDueDate) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                OutlinedButton(
                                    onClick = { showDatePicker(context, subtaskDueDateDate) { subtaskDueDateDate = it } },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(subtaskDueDateDate.ifEmpty { "Tanggal" }, fontSize = 11.sp)
                                }
                                OutlinedButton(
                                    onClick = { showTimePicker(context, subtaskDueDateTime) { subtaskDueDateTime = it } },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(subtaskDueDateTime, fontSize = 11.sp)
                                }
                            }
                            if (isSubtaskExceeding) {
                                Text(
                                    stringResource(R.string.subtask_due_date_exceed_warning),
                                    fontSize = 10.sp,
                                    color = Color(0xFFEF4444)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                text = stringResource(R.string.save),
                enabled = title.isNotBlank(),
                onClick = {
                    val parsedTags = tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                    val req = CreateTaskRequest(
                        title = title.trim(),
                        description = description.trim(),
                        status = status,
                        priority = priority,
                        dueDate = fullTaskDueDate,
                        isFocus = isFocus,
                        tags = parsedTags,
                        projectId = selectedProjectId,
                        subtasks = subtasks.toList()
                    )
                    onCreate(req)
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

// ==========================================
// Create Recurring Task Dialog
// ==========================================

@Composable
fun CreateRecurringTaskDialog(
    projects: List<Project>,
    onDismiss: () -> Unit,
    onCreate: (CreateRecurringTaskRequest) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var priority by remember { mutableStateOf("medium") }
    var selectedProjectId by remember { mutableStateOf<Long?>(null) }
    var recurrencePattern by remember { mutableStateOf("daily") }
    var recurrenceInterval by remember { mutableStateOf(1) }
    var hasDueTime by remember { mutableStateOf(false) }
    var dueTime by remember { mutableStateOf("09:00") }

    val subtasks = remember { mutableStateListOf<String>() }
    var newSubtaskText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_recurring_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.task_title_label)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.task_desc_label)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                // Recurrence Pattern
                Text(stringResource(R.string.repeat_pattern), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("daily", "weekdays", "weekly", "monthly").forEach { pat ->
                        val label = when (pat) {
                            "daily" -> stringResource(R.string.pattern_daily)
                            "weekdays" -> stringResource(R.string.pattern_weekdays)
                            "weekly" -> stringResource(R.string.pattern_weekly)
                            "monthly" -> stringResource(R.string.pattern_monthly)
                            else -> pat
                        }
                        FilterChip(
                            selected = recurrencePattern == pat,
                            onClick = { recurrencePattern = pat },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                // Recurrence Interval Stepper (for daily, weekly, monthly)
                if (recurrencePattern != "weekdays") {
                    val unit = when (recurrencePattern) {
                        "monthly" -> stringResource(R.string.unit_months)
                        "weekly" -> stringResource(R.string.unit_weeks)
                        else -> stringResource(R.string.unit_days)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.repeat_interval, recurrenceInterval, unit), fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (recurrenceInterval > 1) recurrenceInterval-- },
                                enabled = recurrenceInterval > 1
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = null)
                            }
                            Text("$recurrenceInterval", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(onClick = { if (recurrenceInterval < 365) recurrenceInterval++ }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                            }
                        }
                    }
                }

                // Due Time Toggle & Picker (HH:mm)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.recurring_due_time_label), fontSize = 13.sp)
                    Switch(checked = hasDueTime, onCheckedChange = { hasDueTime = it })
                }
                if (hasDueTime) {
                    OutlinedButton(
                        onClick = { showTimePicker(context, dueTime) { dueTime = it } },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.due_time_at, dueTime), fontSize = 13.sp)
                    }
                }

                // Priority
                Text(stringResource(R.string.priority_label), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("low", "medium", "high", "urgent").forEach { p ->
                        val pLabel = when (p) {
                            "low" -> stringResource(R.string.priority_low)
                            "medium" -> stringResource(R.string.priority_medium)
                            "high" -> stringResource(R.string.priority_high)
                            "urgent" -> stringResource(R.string.priority_urgent)
                            else -> p
                        }
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(pLabel, fontSize = 11.sp) }
                        )
                    }
                }

                // Project Selector
                if (projects.isNotEmpty()) {
                    Text(stringResource(R.string.project_label), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedProjectId == null,
                            onClick = { selectedProjectId = null },
                            label = { Text(stringResource(R.string.no_project), fontSize = 11.sp) }
                        )
                        projects.forEach { proj ->
                            val projColor = Color(android.graphics.Color.parseColor(proj.color.ifEmpty { "#3B82F6" }))
                            FilterChip(
                                selected = selectedProjectId == proj.id,
                                onClick = { selectedProjectId = proj.id },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(projColor)
                                    )
                                },
                                label = { Text(proj.name, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Subtasks
                Text(stringResource(R.string.subtasks_header, subtasks.size, subtasks.size), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                subtasks.forEachIndexed { idx, st ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(st, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { subtasks.removeAt(idx) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newSubtaskText,
                        onValueChange = { newSubtaskText = it },
                        placeholder = { Text(stringResource(R.string.add_subtask_placeholder), fontSize = 12.sp) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (newSubtaskText.isNotBlank()) {
                                subtasks.add(newSubtaskText.trim())
                                newSubtaskText = ""
                            }
                        },
                        enabled = newSubtaskText.isNotBlank()
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, tint = BrandPrimary)
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                text = stringResource(R.string.save),
                enabled = title.isNotBlank(),
                onClick = {
                    val subtaskItems = if (subtasks.isEmpty()) null else subtasks.mapIndexed { idx, s ->
                        CreateRecurringSubtaskItem(title = s, orderIndex = idx)
                    }
                    val req = CreateRecurringTaskRequest(
                        title = title.trim(),
                        description = description.trim().ifEmpty { null },
                        priority = priority,
                        projectId = selectedProjectId,
                        recurrencePattern = recurrencePattern,
                        recurrenceInterval = recurrenceInterval,
                        dueTime = if (hasDueTime) (if (dueTime.length == 5) "$dueTime:00" else dueTime) else null,
                        isActive = true,
                        subtasks = subtaskItems
                    )
                    onCreate(req)
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

// ==========================================
// Edit Recurring Task Dialog
// ==========================================

@Composable
fun EditRecurringTaskDialog(
    item: RecurringTaskItem,
    projects: List<Project>,
    onDismiss: () -> Unit,
    onSave: (UpdateRecurringTaskRequest) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(item.title) }
    var description by remember { mutableStateOf(item.description) }
    var priority by remember { mutableStateOf(item.priority) }
    var selectedProjectId by remember { mutableStateOf(item.projectId) }
    var recurrencePattern by remember { mutableStateOf(item.recurrencePattern) }
    var recurrenceInterval by remember { mutableStateOf(item.recurrenceInterval) }
    var isActive by remember { mutableStateOf(item.isActive) }

    var hasDueTime by remember { mutableStateOf(!item.dueTime.isNullOrBlank()) }
    var dueTime by remember {
        mutableStateOf(
            if (!item.dueTime.isNullOrBlank() && item.dueTime.length >= 5) item.dueTime.substring(0, 5) else "09:00"
        )
    }

    val subtasks = remember {
        mutableStateListOf<String>().apply {
            addAll(item.subtasks.map { it.title })
        }
    }
    var newSubtaskText by remember { mutableStateOf("") }
    var showDeleteAlert by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_recurring_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.task_title_label)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.task_desc_label)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                // Active toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.recurring_active), fontSize = 13.sp)
                    Switch(checked = isActive, onCheckedChange = { isActive = it })
                }

                // Recurrence Pattern
                Text(stringResource(R.string.repeat_pattern), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("daily", "weekdays", "weekly", "monthly").forEach { pat ->
                        val label = when (pat) {
                            "daily" -> stringResource(R.string.pattern_daily)
                            "weekdays" -> stringResource(R.string.pattern_weekdays)
                            "weekly" -> stringResource(R.string.pattern_weekly)
                            "monthly" -> stringResource(R.string.pattern_monthly)
                            else -> pat
                        }
                        FilterChip(
                            selected = recurrencePattern == pat,
                            onClick = { recurrencePattern = pat },
                            label = { Text(label, fontSize = 11.sp) }
                        )
                    }
                }

                // Recurrence Interval Stepper
                if (recurrencePattern != "weekdays") {
                    val unit = when (recurrencePattern) {
                        "monthly" -> stringResource(R.string.unit_months)
                        "weekly" -> stringResource(R.string.unit_weeks)
                        else -> stringResource(R.string.unit_days)
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(stringResource(R.string.repeat_interval, recurrenceInterval, unit), fontSize = 12.sp)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { if (recurrenceInterval > 1) recurrenceInterval-- },
                                enabled = recurrenceInterval > 1
                            ) {
                                Icon(Icons.Default.Remove, contentDescription = null)
                            }
                            Text("$recurrenceInterval", fontWeight = FontWeight.Bold, modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(onClick = { if (recurrenceInterval < 365) recurrenceInterval++ }) {
                                Icon(Icons.Default.Add, contentDescription = null)
                            }
                        }
                    }
                }

                // Due Time
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.recurring_due_time_label), fontSize = 13.sp)
                    Switch(checked = hasDueTime, onCheckedChange = { hasDueTime = it })
                }
                if (hasDueTime) {
                    OutlinedButton(
                        onClick = { showTimePicker(context, dueTime) { dueTime = it } },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.due_time_at, dueTime), fontSize = 13.sp)
                    }
                }

                // Priority
                Text(stringResource(R.string.priority_label), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf("low", "medium", "high", "urgent").forEach { p ->
                        val pLabel = when (p) {
                            "low" -> stringResource(R.string.priority_low)
                            "medium" -> stringResource(R.string.priority_medium)
                            "high" -> stringResource(R.string.priority_high)
                            "urgent" -> stringResource(R.string.priority_urgent)
                            else -> p
                        }
                        FilterChip(
                            selected = priority == p,
                            onClick = { priority = p },
                            label = { Text(pLabel, fontSize = 11.sp) }
                        )
                    }
                }

                // Project Selector
                if (projects.isNotEmpty()) {
                    Text(stringResource(R.string.project_label), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        FilterChip(
                            selected = selectedProjectId == null,
                            onClick = { selectedProjectId = null },
                            label = { Text(stringResource(R.string.no_project), fontSize = 11.sp) }
                        )
                        projects.forEach { proj ->
                            val projColor = Color(android.graphics.Color.parseColor(proj.color.ifEmpty { "#3B82F6" }))
                            FilterChip(
                                selected = selectedProjectId == proj.id,
                                onClick = { selectedProjectId = proj.id },
                                leadingIcon = {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(projColor)
                                    )
                                },
                                label = { Text(proj.name, fontSize = 11.sp) }
                            )
                        }
                    }
                }

                // Subtasks
                Text(stringResource(R.string.subtasks_header, subtasks.size, subtasks.size), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                subtasks.forEachIndexed { idx, st ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(st, fontSize = 13.sp, modifier = Modifier.weight(1f))
                        IconButton(onClick = { subtasks.removeAt(idx) }, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp))
                        }
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = newSubtaskText,
                        onValueChange = { newSubtaskText = it },
                        placeholder = { Text(stringResource(R.string.add_subtask_placeholder), fontSize = 12.sp) },
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier.weight(1f),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = {
                            if (newSubtaskText.isNotBlank()) {
                                subtasks.add(newSubtaskText.trim())
                                newSubtaskText = ""
                            }
                        },
                        enabled = newSubtaskText.isNotBlank()
                    ) {
                        Icon(Icons.Default.AddCircle, contentDescription = null, tint = BrandPrimary)
                    }
                }

                // Delete button
                OutlinedButton(
                    onClick = { showDeleteAlert = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.delete_recurring), fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            AppButton(
                text = stringResource(R.string.save),
                enabled = title.isNotBlank(),
                onClick = {
                    val subtaskItems = subtasks.mapIndexed { idx, s ->
                        CreateRecurringSubtaskItem(title = s, orderIndex = idx)
                    }
                    val req = UpdateRecurringTaskRequest(
                        title = title.trim(),
                        description = description.trim().ifEmpty { null },
                        priority = priority,
                        projectId = selectedProjectId,
                        clearProjectId = selectedProjectId == null,
                        recurrencePattern = recurrencePattern,
                        recurrenceInterval = recurrenceInterval,
                        dueTime = if (hasDueTime) (if (dueTime.length == 5) "$dueTime:00" else dueTime) else null,
                        clearDueTime = !hasDueTime,
                        isActive = isActive,
                        subtasks = subtaskItems
                    )
                    onSave(req)
                }
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )

    if (showDeleteAlert) {
        AlertDialog(
            onDismissRequest = { showDeleteAlert = false },
            title = { Text(stringResource(R.string.delete_recurring_confirm_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.delete_recurring_confirm_desc, item.title)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAlert = false
                        onDelete()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAlert = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}
