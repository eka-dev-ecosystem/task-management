package cloud.eka_dev.task_hub.feature.task

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cloud.eka_dev.task_hub.R
import cloud.eka_dev.task_hub.core.designsystem.*
import cloud.eka_dev.task_hub.core.model.Project
import cloud.eka_dev.task_hub.core.model.Subtask
import cloud.eka_dev.task_hub.core.model.Task
import cloud.eka_dev.task_hub.core.model.TaskActivity
import java.util.Calendar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailBottomSheet(
    task: Task,
    viewModel: TaskViewModel,
    onDismiss: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val currentTask = if (uiState.selectedTask?.id == task.id) uiState.selectedTask!! else task
    var selectedTab by remember { mutableIntStateOf(0) }
    var newNoteText by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MaterialTheme.colorScheme.surface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 32.dp)
        ) {
            // Task Header
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = currentTask.title,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (!currentTask.projectName.isNullOrEmpty()) {
                                ProjectPill(name = currentTask.projectName, colorHex = currentTask.projectColor)
                            }
                            PriorityBadge(priority = currentTask.priority)
                            StatusBadge(status = currentTask.status)
                        }
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.toggleTaskFocus(currentTask) }) {
                            Icon(
                                imageVector = if (currentTask.isFocus) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = stringResource(R.string.filter_focus),
                                tint = if (currentTask.isFocus) Color(0xFFF59E0B) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(onClick = { showEditDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = stringResource(R.string.edit_task),
                                tint = BrandPrimary
                            )
                        }
                    }
                }

                if (!currentTask.description.isNullOrEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = currentTask.description,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 3
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Tab Selector
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    leadingIcon = { Icon(Icons.Default.Checklist, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = { Text(stringResource(R.string.tab_details_subtasks)) },
                    modifier = Modifier.weight(1f)
                )

                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    leadingIcon = { Icon(Icons.Default.History, contentDescription = null, modifier = Modifier.size(16.dp)) },
                    label = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(stringResource(R.string.tab_activity_logs))
                            if (uiState.taskActivities.isNotEmpty()) {
                                Surface(
                                    shape = CircleShape,
                                    color = BrandPrimary.copy(alpha = 0.2f),
                                    modifier = Modifier.size(18.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text(
                                            "${uiState.taskActivities.size}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandPrimary
                                        )
                                    }
                                }
                            }
                        }
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Tab Content
            if (selectedTab == 0) {
                DetailTabContent(task = currentTask, viewModel = viewModel, onTaskDeleted = onDismiss)
            } else {
                ActivityLogsTabContent(
                    task = currentTask,
                    activities = uiState.taskActivities,
                    isLoading = uiState.isLoadingActivities,
                    newNoteText = newNoteText,
                    onNoteChange = { newNoteText = it },
                    onPostNote = {
                        viewModel.addActivityNote(currentTask.id, newNoteText)
                        newNoteText = ""
                    }
                )
            }
        }
    }

    if (showEditDialog) {
        EditTaskDialog(
            task = currentTask,
            projects = uiState.projects,
            onDismiss = { showEditDialog = false },
            onSave = { updatedTitle, updatedDesc, updatedStatus, updatedPriority, updatedIsFocus, updatedProjectId, clearProjectId, updatedDueDate, clearDueDate, updatedTags ->
                viewModel.updateTask(
                    id = currentTask.id,
                    title = updatedTitle,
                    description = updatedDesc,
                    status = updatedStatus,
                    priority = updatedPriority,
                    isFocus = updatedIsFocus,
                    projectId = updatedProjectId,
                    clearProjectId = clearProjectId,
                    dueDate = updatedDueDate,
                    clearDueDate = clearDueDate,
                    tags = updatedTags,
                    onSuccess = {
                        showEditDialog = false
                    }
                )
            }
        )
    }
}

@Composable
private fun DetailTabContent(
    task: Task,
    viewModel: TaskViewModel,
    onTaskDeleted: () -> Unit
) {
    val context = LocalContext.current
    var newSubtaskText by remember { mutableStateOf("") }
    var newSubtaskDesc by remember { mutableStateOf("") }
    var newSubtaskSkippable by remember { mutableStateOf(true) }
    var showSubtaskOptions by remember { mutableStateOf(false) }
    var hasSubtaskDueDate by remember { mutableStateOf(false) }
    var subtaskDueDateDate by remember { mutableStateOf("") }
    var subtaskDueDateTime by remember { mutableStateOf("17:00") }
    var editingSubtask by remember { mutableStateOf<Subtask?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val fullSubtaskDueDate = if (hasSubtaskDueDate && subtaskDueDateDate.isNotBlank()) "${subtaskDueDateDate}T${subtaskDueDateTime}:00Z" else null
    val isSubtaskExceeding = task.dueDate != null && fullSubtaskDueDate != null && fullSubtaskDueDate > task.dueDate

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Status Stepper / Selector
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                stringResource(R.string.status_header),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    Triple("todo", stringResource(R.string.filter_todo), StatusTodo),
                    Triple("in_progress", stringResource(R.string.filter_in_progress), StatusInProgress),
                    Triple("review", stringResource(R.string.filter_review), StatusReview),
                    Triple("done", stringResource(R.string.filter_done), StatusDone)
                ).forEach { (st, label, color) ->
                    val isSelected = task.status.lowercase() == st
                    FilterChip(
                        selected = isSelected,
                        onClick = { viewModel.updateTaskStatus(task, st) },
                        leadingIcon = {
                            Icon(
                                when (st) {
                                    "done" -> Icons.Default.CheckCircle
                                    "in_progress" -> Icons.Default.Sync
                                    "review" -> Icons.Default.Visibility
                                    else -> Icons.Default.RadioButtonUnchecked
                                },
                                contentDescription = null,
                                tint = if (isSelected) color else MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                        },
                        label = {
                            Text(
                                label,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) color else MaterialTheme.colorScheme.onSurface
                            )
                        }
                    )
                }
            }
        }

        // Subtask Progress Header
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val subtasksHeaderText = if (task.subtasksCancelled > 0) {
                    "${stringResource(R.string.subtasks_header, task.subtasksCompleted, task.subtasksCount)} • ${task.subtasksCancelled} ${stringResource(R.string.subtask_skipped).lowercase()}"
                } else {
                    stringResource(R.string.subtasks_header, task.subtasksCompleted, task.subtasksCount)
                }
                Text(
                    subtasksHeaderText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${task.progressPercent}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary
                )
            }

            LinearProgressIndicator(
                progress = { task.progressPercent / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = BrandPrimary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
        }

        // Add Subtask Form
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f))
                .padding(8.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = newSubtaskText,
                    onValueChange = { newSubtaskText = it },
                    placeholder = { Text(stringResource(R.string.add_subtask_placeholder), fontSize = 12.sp) },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                        focusedContainerColor = MaterialTheme.colorScheme.surface
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                IconButton(
                    onClick = {
                        if (newSubtaskText.isNotBlank()) {
                            viewModel.createSubtask(
                                taskId = task.id,
                                title = newSubtaskText.trim(),
                                description = newSubtaskDesc.trim().ifEmpty { null },
                                dueDate = fullSubtaskDueDate,
                                isSkippable = newSubtaskSkippable
                            )
                            newSubtaskText = ""
                            newSubtaskDesc = ""
                            hasSubtaskDueDate = false
                            subtaskDueDateDate = ""
                            newSubtaskSkippable = true
                            showSubtaskOptions = false
                        }
                    },
                    enabled = newSubtaskText.isNotBlank()
                ) {
                    Icon(
                        Icons.Default.AddCircle,
                        contentDescription = stringResource(R.string.add_subtask),
                        tint = if (newSubtaskText.isNotBlank()) BrandPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f)
                    )
                }
            }

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
                    Text(stringResource(R.string.subtask_skippable), fontSize = 11.sp)
                    Switch(
                        checked = newSubtaskSkippable,
                        onCheckedChange = { newSubtaskSkippable = it },
                        modifier = Modifier.scale(0.8f)
                    )
                }

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
                        val cal = Calendar.getInstance()
                        OutlinedButton(
                            onClick = {
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        subtaskDueDateDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                    },
                                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(subtaskDueDateDate.ifEmpty { "Tanggal" }, fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                android.app.TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        subtaskDueDateTime = String.format("%02d:%02d", h, m)
                                    },
                                    cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true
                                ).show()
                            },
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

        // Subtask Checklist Items
        if (task.subtasks.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                task.subtasks.forEach { subtask ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (subtask.isCancelled) {
                            IconButton(
                                onClick = {
                                    viewModel.toggleSubtask(task.id, subtask.id, false)
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    Icons.Default.Remove,
                                    contentDescription = stringResource(R.string.subtask_skipped),
                                    tint = Color(0xFFF97316),
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        } else {
                            Checkbox(
                                checked = subtask.isCompleted,
                                onCheckedChange = {
                                    viewModel.toggleSubtask(task.id, subtask.id, subtask.isCompleted)
                                },
                                colors = CheckboxDefaults.colors(checkedColor = StatusDone)
                            )
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = subtask.title,
                                fontSize = 13.sp,
                                textDecoration = if (subtask.isCompleted || subtask.isCancelled) TextDecoration.LineThrough else TextDecoration.None,
                                color = if (subtask.isCompleted || subtask.isCancelled) MaterialTheme.colorScheme.onSurfaceVariant else MaterialTheme.colorScheme.onSurface
                            )
                            if (!subtask.isSkippable) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Surface(
                                    color = MaterialTheme.colorScheme.error.copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.subtask_mandatory),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            if (subtask.isCancelled) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Surface(
                                    color = Color(0xFFF97316).copy(alpha = 0.15f),
                                    shape = RoundedCornerShape(4.dp)
                                ) {
                                    Text(
                                        text = stringResource(R.string.subtask_skipped),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color(0xFFEA580C),
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 1.dp)
                                    )
                                }
                            }
                            if (subtask.description.isNotBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = subtask.description,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                                    maxLines = 2
                                )
                            }
                            if (!subtask.dueDate.isNullOrBlank()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = subtask.dueDate,
                                    fontSize = 10.sp,
                                    color = BrandPrimary
                                )
                            }
                        }
                        IconButton(
                            onClick = { editingSubtask = subtask },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = stringResource(R.string.edit),
                                tint = BrandPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        if (subtask.isCancelled || subtask.isSkippable) {
                            IconButton(
                                onClick = { viewModel.toggleSubtaskCancelled(task.id, subtask.id, subtask.isCancelled) },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    if (subtask.isCancelled) Icons.Default.Undo else Icons.Default.Remove,
                                    contentDescription = stringResource(if (subtask.isCancelled) R.string.unskip_subtask else R.string.skip_subtask),
                                    tint = if (subtask.isCancelled) BrandPrimary else MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        IconButton(
                            onClick = { viewModel.deleteSubtask(task.id, subtask.id) },
                            modifier = Modifier.size(24.dp)
                        ) {
                            Icon(
                                Icons.Default.DeleteOutline,
                                contentDescription = stringResource(R.string.delete_subtask),
                                tint = MaterialTheme.colorScheme.error.copy(alpha = 0.7f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        } else {
            Text(
                stringResource(R.string.no_subtasks),
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Delete Task Button
        OutlinedButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
        ) {
            Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(6.dp))
            Text(stringResource(R.string.delete_task_button), fontWeight = FontWeight.SemiBold)
        }
    }

    editingSubtask?.let { st ->
        EditSubtaskDialog(
            subtask = st,
            taskDueDate = task.dueDate,
            onDismiss = { editingSubtask = null },
            onSave = { updatedTitle, updatedDesc, updatedDueDate, updatedIsSkippable ->
                viewModel.updateSubtask(
                    taskId = task.id,
                    subtaskId = st.id,
                    title = updatedTitle,
                    description = updatedDesc,
                    dueDate = updatedDueDate,
                    isSkippable = updatedIsSkippable
                )
                editingSubtask = null
            }
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.delete_task_confirm_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.delete_task_confirm_desc, task.title)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteTask(task.id) {
                            showDeleteDialog = false
                            onTaskDeleted()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete), color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

@Composable
private fun ActivityLogsTabContent(
    task: Task,
    activities: List<TaskActivity>,
    isLoading: Boolean,
    newNoteText: String,
    onNoteChange: (String) -> Unit,
    onPostNote: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    ) {
        // Post Note Form
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(BrandPrimary.copy(alpha = 0.08f))
                .padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OutlinedTextField(
                value = newNoteText,
                onValueChange = onNoteChange,
                placeholder = { Text(stringResource(R.string.post_note_placeholder), fontSize = 12.sp) },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.weight(1f),
                singleLine = true,
                colors = OutlinedTextFieldDefaults.colors(
                    unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                    focusedContainerColor = MaterialTheme.colorScheme.surface
                )
            )
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onPostNote,
                enabled = newNoteText.isNotBlank(),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Icon(Icons.Default.Send, contentDescription = stringResource(R.string.send), modifier = Modifier.size(14.dp))
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Activity Timeline
        if (isLoading && activities.isEmpty()) {
            TaskListSkeleton(count = 2)
        } else if (activities.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.History, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(36.dp))
                Text(stringResource(R.string.activity_logs_empty), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                Text(
                    stringResource(R.string.activity_logs_empty_desc),
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth().heightIn(max = 350.dp)
            ) {
                itemsIndexed(activities, key = { _, act -> act.id }) { index, act ->
                    val badge = getActivityBadge(act.activityType)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = badge.color.copy(alpha = 0.15f),
                            modifier = Modifier.size(32.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(badge.icon, contentDescription = null, tint = badge.color, modifier = Modifier.size(16.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = stringResource(badge.labelRes),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = badge.color
                                )
                                Text(
                                    text = act.createdAt.take(16).replace("T", " "),
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = act.description,
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class ActivityBadge(val icon: androidx.compose.ui.graphics.vector.ImageVector, val color: Color, @StringRes val labelRes: Int)

private fun getActivityBadge(type: String): ActivityBadge {
    return when (type) {
        "task_created" -> ActivityBadge(Icons.Default.AutoAwesome, Color(0xFF10B981), R.string.activity_task_created)
        "task_updated" -> ActivityBadge(Icons.Default.Edit, Color(0xFF3B82F6), R.string.activity_task_updated)
        "progress_logged" -> ActivityBadge(Icons.Default.History, Color(0xFF6366F1), R.string.activity_progress_logged)
        "subtask_created" -> ActivityBadge(Icons.Default.Add, Color(0xFF06B6D4), R.string.activity_subtask_created)
        "subtask_toggled" -> ActivityBadge(Icons.Default.CheckCircle, Color(0xFF14B8A6), R.string.activity_subtask_toggled)
        "subtask_deleted" -> ActivityBadge(Icons.Default.Delete, Color(0xFFF43F5E), R.string.activity_subtask_deleted)
        "focus_toggled" -> ActivityBadge(Icons.Default.Star, Color(0xFFF59E0B), R.string.activity_focus_toggled)
        else -> ActivityBadge(Icons.Default.Comment, Color(0xFF64748B), R.string.activity_general_log)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditTaskDialog(
    task: Task,
    projects: List<Project>,
    onDismiss: () -> Unit,
    onSave: (
        title: String,
        description: String,
        status: String,
        priority: String,
        isFocus: Boolean,
        projectId: Long?,
        clearProjectId: Boolean,
        dueDate: String?,
        clearDueDate: Boolean,
        tags: List<String>
    ) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(task.title) }
    var description by remember { mutableStateOf(task.description) }
    var status by remember { mutableStateOf(task.status) }
    var priority by remember { mutableStateOf(task.priority) }
    var isFocus by remember { mutableStateOf(task.isFocus) }
    var selectedProjectId by remember { mutableStateOf(task.projectId) }
    var tagsText by remember { mutableStateOf(task.tags.joinToString(", ")) }

    var hasDueDate by remember { mutableStateOf(!task.dueDate.isNullOrBlank()) }
    var dueDateDate by remember {
        mutableStateOf(
            if (!task.dueDate.isNullOrBlank() && task.dueDate.length >= 10) task.dueDate.substring(0, 10) else ""
        )
    }
    var dueDateTime by remember {
        mutableStateOf(
            if (!task.dueDate.isNullOrBlank() && task.dueDate.length >= 16) task.dueDate.substring(11, 16) else "23:59"
        )
    }

    val fullDueDate = if (hasDueDate && dueDateDate.isNotBlank()) "${dueDateDate}T${dueDateTime}:00Z" else null

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_task), fontWeight = FontWeight.Bold) },
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
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.task_desc_label)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    maxLines = 5
                )

                Text(stringResource(R.string.status_header), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
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

                Text(stringResource(R.string.priority_label), fontWeight = FontWeight.SemiBold, fontSize = 12.sp)
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
                        val cal = Calendar.getInstance()
                        OutlinedButton(
                            onClick = {
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        dueDateDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                    },
                                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(dueDateDate.ifEmpty { "Pilih Tanggal" }, fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                android.app.TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        dueDateTime = String.format("%02d:%02d", h, m)
                                    },
                                    cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(dueDateTime, fontSize = 11.sp)
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.focus_today), fontSize = 13.sp)
                    Switch(
                        checked = isFocus,
                        onCheckedChange = { isFocus = it }
                    )
                }

                OutlinedTextField(
                    value = tagsText,
                    onValueChange = { tagsText = it },
                    label = { Text(stringResource(R.string.tags_label)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
            }
        },
        confirmButton = {
            AppButton(
                text = stringResource(R.string.save),
                onClick = {
                    if (title.isNotBlank()) {
                        val parsedTags = tagsText.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        onSave(
                            title.trim(),
                            description.trim(),
                            status,
                            priority,
                            isFocus,
                            selectedProjectId,
                            selectedProjectId == null,
                            fullDueDate,
                            !hasDueDate,
                            parsedTags
                        )
                    }
                },
                enabled = title.isNotBlank()
            )
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
fun EditSubtaskDialog(
    subtask: Subtask,
    taskDueDate: String?,
    onDismiss: () -> Unit,
    onSave: (title: String, description: String?, dueDate: String?, isSkippable: Boolean) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(subtask.title) }
    var description by remember { mutableStateOf(subtask.description) }
    var isSkippable by remember { mutableStateOf(subtask.isSkippable) }
    var hasDueDate by remember { mutableStateOf(!subtask.dueDate.isNullOrBlank()) }
    var dueDateDate by remember {
        mutableStateOf(
            if (!subtask.dueDate.isNullOrBlank() && subtask.dueDate.length >= 10) subtask.dueDate.substring(0, 10) else ""
        )
    }
    var dueDateTime by remember {
        mutableStateOf(
            if (!subtask.dueDate.isNullOrBlank() && subtask.dueDate.length >= 16) subtask.dueDate.substring(11, 16) else "17:00"
        )
    }

    val fullSubtaskDueDate = if (hasDueDate && dueDateDate.isNotBlank()) "${dueDateDate}T${dueDateTime}:00Z" else null
    val isExceeding = taskDueDate != null && fullSubtaskDueDate != null && fullSubtaskDueDate > taskDueDate

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_subtask_title), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.task_title_label)) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.subtask_description_label)) },
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.subtask_skippable), fontSize = 13.sp)
                    Switch(checked = isSkippable, onCheckedChange = { isSkippable = it })
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(stringResource(R.string.subtask_due_date_label), fontSize = 13.sp)
                    Switch(checked = hasDueDate, onCheckedChange = { hasDueDate = it })
                }
                if (hasDueDate) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        val cal = Calendar.getInstance()
                        OutlinedButton(
                            onClick = {
                                android.app.DatePickerDialog(
                                    context,
                                    { _, year, month, dayOfMonth ->
                                        dueDateDate = String.format("%04d-%02d-%02d", year, month + 1, dayOfMonth)
                                    },
                                    cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(dueDateDate.ifEmpty { "Tanggal" }, fontSize = 11.sp)
                        }
                        OutlinedButton(
                            onClick = {
                                android.app.TimePickerDialog(
                                    context,
                                    { _, h, m ->
                                        dueDateTime = String.format("%02d:%02d", h, m)
                                    },
                                    cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true
                                ).show()
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(dueDateTime, fontSize = 11.sp)
                        }
                    }
                    if (isExceeding) {
                        Text(
                            stringResource(R.string.subtask_due_date_exceed_warning),
                            fontSize = 10.sp,
                            color = Color(0xFFEF4444)
                        )
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                text = stringResource(R.string.save),
                enabled = title.isNotBlank(),
                onClick = {
                    onSave(title.trim(), description.trim().ifEmpty { null }, fullSubtaskDueDate, isSkippable)
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
