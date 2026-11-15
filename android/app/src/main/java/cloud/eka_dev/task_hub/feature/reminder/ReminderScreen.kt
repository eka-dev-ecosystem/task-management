package cloud.eka_dev.task_hub.feature.reminder

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import cloud.eka_dev.task_hub.R
import cloud.eka_dev.task_hub.core.designsystem.*
import cloud.eka_dev.task_hub.core.model.*
import cloud.eka_dev.task_hub.core.network.ApiClient
import cloud.eka_dev.task_hub.core.network.SSEClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Calendar

private fun parseIsoToZonedDateTime(isoString: String?): ZonedDateTime? {
    if (isoString.isNullOrBlank()) return null
    return try {
        ZonedDateTime.parse(isoString)
    } catch (_: Exception) {
        try {
            val ldt = LocalDateTime.parse(isoString.replace(" ", "T"))
            ldt.atZone(ZoneId.systemDefault())
        } catch (_: Exception) {
            null
        }
    }
}

private fun formatToIso(zdt: ZonedDateTime): String {
    return zdt.withZoneSameInstant(ZoneId.of("UTC")).format(DateTimeFormatter.ISO_INSTANT)
}

private fun formatFriendlyDate(isoString: String): String {
    val zdt = parseIsoToZonedDateTime(isoString) ?: return isoString
    val local = zdt.withZoneSameInstant(ZoneId.systemDefault())
    val formatter = DateTimeFormatter.ofPattern("d MMM yyyy, HH:mm")
    return local.format(formatter)
}

class ReminderViewModel(
    private val apiClient: ApiClient,
    private val sseClient: SSEClient
) : ViewModel() {

    private val _reminders = MutableStateFlow<List<Reminder>>(emptyList())
    val reminders: StateFlow<List<Reminder>> = _reminders.asStateFlow()

    private val _tasks = MutableStateFlow<List<Task>>(emptyList())
    val tasks: StateFlow<List<Task>> = _tasks.asStateFlow()

    private val _selectedStatus = MutableStateFlow<String?>("pending")
    val selectedStatus: StateFlow<String?> = _selectedStatus.asStateFlow()

    private val _activeAlert = MutableStateFlow<String?>(null)
    val activeAlert: StateFlow<String?> = _activeAlert.asStateFlow()

    val filteredReminders: List<Reminder>
        get() = when (_selectedStatus.value) {
            "pending" -> _reminders.value.filter { it.status == "pending" }
            "completed" -> _reminders.value.filter { it.status == "sent" || it.status == "dismissed" }
            else -> _reminders.value
        }

    init {
        loadReminders()
        loadTasks()
        listenToAlerts()
    }

    fun setSelectedStatus(status: String?) {
        _selectedStatus.value = status
    }

    fun loadReminders() {
        viewModelScope.launch {
            try {
                val res = apiClient.apiService.getReminders()
                if (res.success && res.data != null) {
                    _reminders.value = res.data
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun loadTasks() {
        viewModelScope.launch {
            try {
                val res = apiClient.apiService.getTasks(status = null)
                if (res.success && res.data != null) {
                    _tasks.value = res.data
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    private fun listenToAlerts() {
        viewModelScope.launch {
            sseClient.events.collect { event ->
                if (event.data.contains("reminder") || event.type?.contains("reminder") == true) {
                    _activeAlert.value = "Pengingat Baru Diterima: ${event.data}"
                    loadReminders()
                }
            }
        }
    }

    fun snooze(id: Long) {
        viewModelScope.launch {
            try {
                apiClient.apiService.snoozeReminder(id)
                loadReminders()
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun dismiss(id: Long) {
        viewModelScope.launch {
            try {
                apiClient.apiService.dismissReminder(id)
                loadReminders()
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun createReminder(req: CreateReminderRequest, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val res = apiClient.apiService.createReminder(req)
                if (res.success && res.data != null) {
                    _reminders.value = listOf(res.data) + _reminders.value
                    onSuccess?.invoke()
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun updateReminder(id: Long, req: UpdateReminderRequest, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val res = apiClient.apiService.updateReminder(id, req)
                if (res.success && res.data != null) {
                    _reminders.value = _reminders.value.map { if (it.id == id) res.data else it }
                    onSuccess?.invoke()
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun deleteReminder(id: Long, onSuccess: (() -> Unit)? = null) {
        viewModelScope.launch {
            try {
                val res = apiClient.apiService.deleteReminder(id)
                if (res.success) {
                    _reminders.value = _reminders.value.filter { it.id != id }
                    onSuccess?.invoke()
                }
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun registerPushToken(fcmToken: String) {
        viewModelScope.launch {
            try {
                apiClient.apiService.registerDeviceToken(
                    RegisterDeviceTokenRequest(token = fcmToken, platform = "android")
                )
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    fun clearActiveAlert() {
        _activeAlert.value = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReminderScreen(viewModel: ReminderViewModel) {
    val activeAlert by viewModel.activeAlert.collectAsState()
    val selectedStatus by viewModel.selectedStatus.collectAsState()
    val tasks by viewModel.tasks.collectAsState()

    var showCreateDialog by remember { mutableStateOf(false) }
    var editingReminder by remember { mutableStateOf<Reminder?>(null) }
    var deletingReminder by remember { mutableStateOf<Reminder?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.reminders_title), fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = MaterialTheme.colorScheme.background)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = BrandPrimary,
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.create_reminder))
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 16.dp)
        ) {
            // Live SSE Banner
            if (activeAlert != null) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(activeAlert ?: "", modifier = Modifier.weight(1f), fontSize = 13.sp)
                        TextButton(onClick = { viewModel.clearActiveAlert() }) {
                            Text(stringResource(R.string.close))
                        }
                    }
                }
            }

            // Filter Tabs (Upcoming, Completed, All)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedStatus == "pending",
                    onClick = { viewModel.setSelectedStatus("pending") },
                    label = { Text(stringResource(R.string.filter_upcoming)) }
                )
                FilterChip(
                    selected = selectedStatus == "completed",
                    onClick = { viewModel.setSelectedStatus("completed") },
                    label = { Text(stringResource(R.string.filter_completed)) }
                )
                FilterChip(
                    selected = selectedStatus == null,
                    onClick = { viewModel.setSelectedStatus(null) },
                    label = { Text(stringResource(R.string.filter_all)) }
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            val displayedReminders = viewModel.filteredReminders

            if (displayedReminders.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(BrandPrimary.copy(alpha = 0.12f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Notifications,
                                contentDescription = null,
                                tint = BrandPrimary,
                                modifier = Modifier.size(32.dp)
                            )
                        }
                        Text(
                            stringResource(R.string.empty_reminders),
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(displayedReminders, key = { it.id }) { reminder ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        reminder.title,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.weight(1f)
                                    )

                                    // Status Badge
                                    val statusColor = when (reminder.status) {
                                        "sent", "dismissed" -> StatusDone
                                        else -> BrandPrimary
                                    }
                                    Surface(
                                        color = statusColor.copy(alpha = 0.15f),
                                        shape = RoundedCornerShape(10.dp)
                                    ) {
                                        Text(
                                            reminder.status.uppercase(),
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = statusColor
                                        )
                                    }
                                }

                                if (reminder.description.isNotBlank()) {
                                    Text(
                                        reminder.description,
                                        fontSize = 13.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }

                                // Linked Task Badge if any
                                if (!reminder.taskTitle.isNullOrBlank()) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(BrandPrimary.copy(alpha = 0.08f))
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Icon(Icons.Default.TaskAlt, contentDescription = null, modifier = Modifier.size(12.dp), tint = BrandPrimary)
                                        Text(
                                            reminder.taskTitle,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = BrandPrimary
                                        )
                                    }
                                }

                                // Schedule Time
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(12.dp), tint = MaterialTheme.colorScheme.primary)
                                    Text(
                                        formatFriendlyDate(reminder.remindAt),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }

                                Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f), modifier = Modifier.padding(vertical = 4.dp))

                                // Action Buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        if (reminder.status == "pending") {
                                            OutlinedButton(
                                                onClick = { viewModel.snooze(reminder.id) },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(stringResource(R.string.snooze_15m), fontSize = 11.sp)
                                            }
                                            Button(
                                                onClick = { viewModel.dismiss(reminder.id) },
                                                contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp)
                                            ) {
                                                Text(stringResource(R.string.dismiss), fontSize = 11.sp)
                                            }
                                        }
                                    }

                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        IconButton(onClick = { editingReminder = reminder }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.edit), modifier = Modifier.size(16.dp), tint = BrandPrimary)
                                        }
                                        IconButton(onClick = { deletingReminder = reminder }, modifier = Modifier.size(32.dp)) {
                                            Icon(Icons.Default.DeleteOutline, contentDescription = stringResource(R.string.delete), modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.error)
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Create Reminder Dialog
    if (showCreateDialog) {
        CreateReminderDialog(
            tasks = tasks,
            onDismiss = { showCreateDialog = false },
            onCreate = { req ->
                viewModel.createReminder(req) {
                    showCreateDialog = false
                }
            }
        )
    }

    // Edit Reminder Dialog
    editingReminder?.let { reminder ->
        EditReminderDialog(
            reminder = reminder,
            tasks = tasks,
            onDismiss = { editingReminder = null },
            onSave = { req ->
                viewModel.updateReminder(reminder.id, req) {
                    editingReminder = null
                }
            },
            onDelete = {
                viewModel.deleteReminder(reminder.id) {
                    editingReminder = null
                }
            }
        )
    }

    // Delete Reminder Confirmation Alert
    deletingReminder?.let { reminder ->
        AlertDialog(
            onDismissRequest = { deletingReminder = null },
            title = { Text(stringResource(R.string.delete_reminder_confirm_title), fontWeight = FontWeight.Bold) },
            text = { Text(stringResource(R.string.delete_reminder_confirm_desc)) },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteReminder(reminder.id) {
                            deletingReminder = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { deletingReminder = null }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }
}

// ==========================================
// Create Reminder Dialog
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateReminderDialog(
    tasks: List<Task>,
    onDismiss: () -> Unit,
    onCreate: (CreateReminderRequest) -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var selectedTask by remember { mutableStateOf<Task?>(null) }

    val initialCal = Calendar.getInstance().apply { add(Calendar.HOUR_OF_DAY, 1) }
    var dateString by remember {
        mutableStateOf(String.format("%04d-%02d-%02d", initialCal.get(Calendar.YEAR), initialCal.get(Calendar.MONTH) + 1, initialCal.get(Calendar.DAY_OF_MONTH)))
    }
    var timeString by remember {
        mutableStateOf(String.format("%02d:%02d", initialCal.get(Calendar.HOUR_OF_DAY), initialCal.get(Calendar.MINUTE)))
    }

    // Task due date presets
    val taskZdt = remember(selectedTask) {
        selectedTask?.dueDate?.let { parseIsoToZonedDateTime(it) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.create_reminder_title), fontWeight = FontWeight.Bold) },
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
                    label = { Text(stringResource(R.string.reminder_title_label)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.reminder_desc_label)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                // Task Link Picker
                Text(stringResource(R.string.associated_task_label), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedTask == null,
                        onClick = { selectedTask = null },
                        label = { Text(stringResource(R.string.no_associated_task), fontSize = 11.sp) }
                    )
                    tasks.forEach { t ->
                        FilterChip(
                            selected = selectedTask?.id == t.id,
                            onClick = {
                                selectedTask = t
                                if (title.isBlank()) {
                                    title = t.title
                                }
                            },
                            label = { Text(t.title, fontSize = 11.sp) }
                        )
                    }
                }

                // Presets Section
                Text("Preset Waktu Cepat", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)

                // Task Due Date Presets (if task has deadline)
                if (taskZdt != null) {
                    Text("Berdasarkan Deadline Tugas:", fontSize = 11.sp, color = BrandPrimary, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SuggestionChip(
                            onClick = {
                                val local = taskZdt.withZoneSameInstant(ZoneId.systemDefault())
                                dateString = local.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                timeString = local.format(DateTimeFormatter.ofPattern("HH:mm"))
                            },
                            label = { Text(stringResource(R.string.preset_deadline_exact), fontSize = 11.sp) }
                        )
                        SuggestionChip(
                            onClick = {
                                val local = taskZdt.minusMinutes(15).withZoneSameInstant(ZoneId.systemDefault())
                                dateString = local.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                timeString = local.format(DateTimeFormatter.ofPattern("HH:mm"))
                            },
                            label = { Text(stringResource(R.string.preset_deadline_15m), fontSize = 11.sp) }
                        )
                        SuggestionChip(
                            onClick = {
                                val local = taskZdt.minusHours(1).withZoneSameInstant(ZoneId.systemDefault())
                                dateString = local.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                timeString = local.format(DateTimeFormatter.ofPattern("HH:mm"))
                            },
                            label = { Text(stringResource(R.string.preset_deadline_1h), fontSize = 11.sp) }
                        )
                        SuggestionChip(
                            onClick = {
                                val local = taskZdt.minusDays(1).withZoneSameInstant(ZoneId.systemDefault())
                                dateString = local.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                timeString = local.format(DateTimeFormatter.ofPattern("HH:mm"))
                            },
                            label = { Text(stringResource(R.string.preset_deadline_1d), fontSize = 11.sp) }
                        )
                    }
                }

                // Relative Quick Presets
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    val now = ZonedDateTime.now()
                    SuggestionChip(
                        onClick = {
                            val target = now.plusMinutes(10)
                            dateString = target.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            timeString = target.format(DateTimeFormatter.ofPattern("HH:mm"))
                        },
                        label = { Text(stringResource(R.string.preset_relative_10m), fontSize = 11.sp) }
                    )
                    SuggestionChip(
                        onClick = {
                            val target = now.plusHours(1)
                            dateString = target.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            timeString = target.format(DateTimeFormatter.ofPattern("HH:mm"))
                        },
                        label = { Text(stringResource(R.string.preset_relative_1h), fontSize = 11.sp) }
                    )
                    SuggestionChip(
                        onClick = {
                            val target = now.plusDays(1).withHour(9).withMinute(0)
                            dateString = target.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                            timeString = target.format(DateTimeFormatter.ofPattern("HH:mm"))
                        },
                        label = { Text(stringResource(R.string.preset_relative_tomorrow), fontSize = 11.sp) }
                    )
                }

                // Date & Time pickers
                Text("Waktu Pengingat:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            android.app.DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    dateString = String.format("%04d-%02d-%02d", y, m + 1, d)
                                },
                                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(dateString, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            android.app.TimePickerDialog(
                                context,
                                { _, h, m ->
                                    timeString = String.format("%02d:%02d", h, m)
                                },
                                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(timeString, fontSize = 12.sp)
                    }
                }
            }
        },
        confirmButton = {
            AppButton(
                text = stringResource(R.string.save),
                enabled = title.isNotBlank() && dateString.isNotBlank() && timeString.isNotBlank(),
                onClick = {
                    val fullIso = "${dateString}T${timeString}:00Z"
                    val req = CreateReminderRequest(
                        taskId = selectedTask?.id,
                        title = title.trim(),
                        description = description.trim(),
                        remindAt = fullIso
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
// Edit Reminder Dialog
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditReminderDialog(
    reminder: Reminder,
    tasks: List<Task>,
    onDismiss: () -> Unit,
    onSave: (UpdateReminderRequest) -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var title by remember { mutableStateOf(reminder.title) }
    var description by remember { mutableStateOf(reminder.description) }
    var selectedTaskId by remember { mutableStateOf(reminder.taskId) }

    val initialZdt = parseIsoToZonedDateTime(reminder.remindAt)
    val localZdt = initialZdt?.withZoneSameInstant(ZoneId.systemDefault())

    var dateString by remember {
        mutableStateOf(localZdt?.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) ?: "")
    }
    var timeString by remember {
        mutableStateOf(localZdt?.format(DateTimeFormatter.ofPattern("HH:mm")) ?: "09:00")
    }

    val selectedTask = remember(selectedTaskId, tasks) {
        tasks.firstOrNull { it.id == selectedTaskId }
    }
    val taskZdt = remember(selectedTask) {
        selectedTask?.dueDate?.let { parseIsoToZonedDateTime(it) }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.edit_reminder_title), fontWeight = FontWeight.Bold) },
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
                    label = { Text(stringResource(R.string.reminder_title_label)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    value = description,
                    onValueChange = { description = it },
                    label = { Text(stringResource(R.string.reminder_desc_label)) },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(),
                    maxLines = 3
                )

                // Task Link Picker
                Text(stringResource(R.string.associated_task_label), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    FilterChip(
                        selected = selectedTaskId == null,
                        onClick = { selectedTaskId = null },
                        label = { Text(stringResource(R.string.no_associated_task), fontSize = 11.sp) }
                    )
                    tasks.forEach { t ->
                        FilterChip(
                            selected = selectedTaskId == t.id,
                            onClick = { selectedTaskId = t.id },
                            label = { Text(t.title, fontSize = 11.sp) }
                        )
                    }
                }

                // Presets Section
                if (taskZdt != null) {
                    Text("Berdasarkan Deadline Tugas:", fontSize = 11.sp, color = BrandPrimary, fontWeight = FontWeight.Medium)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SuggestionChip(
                            onClick = {
                                val local = taskZdt.withZoneSameInstant(ZoneId.systemDefault())
                                dateString = local.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                timeString = local.format(DateTimeFormatter.ofPattern("HH:mm"))
                            },
                            label = { Text(stringResource(R.string.preset_deadline_exact), fontSize = 11.sp) }
                        )
                        SuggestionChip(
                            onClick = {
                                val local = taskZdt.minusMinutes(15).withZoneSameInstant(ZoneId.systemDefault())
                                dateString = local.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                timeString = local.format(DateTimeFormatter.ofPattern("HH:mm"))
                            },
                            label = { Text(stringResource(R.string.preset_deadline_15m), fontSize = 11.sp) }
                        )
                        SuggestionChip(
                            onClick = {
                                val local = taskZdt.minusHours(1).withZoneSameInstant(ZoneId.systemDefault())
                                dateString = local.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                timeString = local.format(DateTimeFormatter.ofPattern("HH:mm"))
                            },
                            label = { Text(stringResource(R.string.preset_deadline_1h), fontSize = 11.sp) }
                        )
                        SuggestionChip(
                            onClick = {
                                val local = taskZdt.minusDays(1).withZoneSameInstant(ZoneId.systemDefault())
                                dateString = local.format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))
                                timeString = local.format(DateTimeFormatter.ofPattern("HH:mm"))
                            },
                            label = { Text(stringResource(R.string.preset_deadline_1d), fontSize = 11.sp) }
                        )
                    }
                }

                // Date & Time pickers
                Text("Waktu Pengingat:", fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            android.app.DatePickerDialog(
                                context,
                                { _, y, m, d ->
                                    dateString = String.format("%04d-%02d-%02d", y, m + 1, d)
                                },
                                cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH)
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(dateString.ifEmpty { "Pilih Tanggal" }, fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = {
                            val cal = Calendar.getInstance()
                            android.app.TimePickerDialog(
                                context,
                                { _, h, m ->
                                    timeString = String.format("%02d:%02d", h, m)
                                },
                                cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE), true
                            ).show()
                        },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Schedule, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(timeString, fontSize = 12.sp)
                    }
                }

                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                ) {
                    Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.delete), fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            AppButton(
                text = stringResource(R.string.save),
                enabled = title.isNotBlank() && dateString.isNotBlank(),
                onClick = {
                    val fullIso = "${dateString}T${timeString}:00Z"
                    val req = UpdateReminderRequest(
                        taskId = selectedTaskId,
                        clearTaskId = selectedTaskId == null,
                        title = title.trim(),
                        description = description.trim(),
                        remindAt = fullIso
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
}
