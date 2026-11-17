package cloud.eka_dev.task_hub

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.annotation.StringRes
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.*
import cloud.eka_dev.task_hub.core.designsystem.LanguageManager
import cloud.eka_dev.task_hub.core.designsystem.ProvideAppLocale
import cloud.eka_dev.task_hub.core.designsystem.SplashScreen
import cloud.eka_dev.task_hub.core.designsystem.TaskManagementTheme
import cloud.eka_dev.task_hub.feature.auth.AuthScreen
import cloud.eka_dev.task_hub.feature.auth.AuthViewModel
import cloud.eka_dev.task_hub.feature.calendar.CalendarScreen
import cloud.eka_dev.task_hub.feature.calendar.CalendarViewModel
import cloud.eka_dev.task_hub.feature.hub.HubLinkConfirmationBottomSheet
import cloud.eka_dev.task_hub.feature.hub.HubLinkSessionPayload
import cloud.eka_dev.task_hub.feature.mcp.McpScreen
import cloud.eka_dev.task_hub.feature.mcp.McpViewModel
import cloud.eka_dev.task_hub.feature.note.NoteScreen
import cloud.eka_dev.task_hub.feature.note.NoteViewModel
import cloud.eka_dev.task_hub.feature.profile.ProfileScreen
import cloud.eka_dev.task_hub.feature.profile.ProfileViewModel
import cloud.eka_dev.task_hub.feature.project.ProjectScreen
import cloud.eka_dev.task_hub.feature.project.ProjectViewModel
import cloud.eka_dev.task_hub.feature.reminder.ReminderScreen
import cloud.eka_dev.task_hub.feature.reminder.ReminderViewModel
import cloud.eka_dev.task_hub.feature.task.TaskListScreen
import cloud.eka_dev.task_hub.feature.task.TaskViewModel

class MainActivity : ComponentActivity() {
    private var pendingHubLink by mutableStateOf<HubLinkSessionPayload?>(null)
    private var taskViewModel: TaskViewModel? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val app = application as TaskApp
        LanguageManager.initialize(app.tokenManager)

        val authViewModel = AuthViewModel(app.apiClient, app.tokenManager)
        val taskVm = TaskViewModel(app.apiClient)
        taskViewModel = taskVm
        val projectViewModel = ProjectViewModel(app.apiClient)
        val calendarViewModel = CalendarViewModel(app.apiClient)
        val noteViewModel = NoteViewModel(app.apiClient)
        val reminderViewModel = ReminderViewModel(app.apiClient, app.sseClient)
        val profileViewModel = ProfileViewModel(app.apiClient, app.tokenManager)
        val mcpViewModel = McpViewModel(app.apiClient)

        handleDeepLink(intent)

        setContent {
            val currentLanguage by LanguageManager.currentLanguage.collectAsState()

            ProvideAppLocale(language = currentLanguage) {
                TaskManagementTheme {
                    val tokenState by app.tokenManager.accessTokenFlow.collectAsState(initial = "loading")
                    var splashCompleted by remember { mutableStateOf(false) }

                    Box(modifier = Modifier.fillMaxSize()) {
                    if (tokenState != "loading") {
                        if (tokenState.isNullOrEmpty()) {
                            AuthScreen(
                                viewModel = authViewModel,
                                onAuthSuccess = {
                                    taskVm.loadTasks()
                                    projectViewModel.loadData()
                                    calendarViewModel.loadMonthData(calendarViewModel.currentMonth.value)
                                    noteViewModel.loadNotes()
                                    reminderViewModel.loadReminders()
                                    profileViewModel.loadProfile()
                                }
                            )
                        } else {
                            MainNavigationScaffold(
                                taskViewModel = taskVm,
                                projectViewModel = projectViewModel,
                                calendarViewModel = calendarViewModel,
                                noteViewModel = noteViewModel,
                                reminderViewModel = reminderViewModel,
                                profileViewModel = profileViewModel,
                                mcpViewModel = mcpViewModel,
                                onSignOut = { authViewModel.signOut() }
                            )
                        }
                    }

                    AnimatedVisibility(
                        visible = (!splashCompleted || tokenState == "loading") && pendingHubLink == null,
                        enter = EnterTransition.None,
                        exit = fadeOut(animationSpec = tween(380))
                    ) {
                        SplashScreen(
                            onComplete = {
                                splashCompleted = true
                            }
                        )
                    }

                    pendingHubLink?.let { payload ->
                        HubLinkConfirmationBottomSheet(
                            payload = payload,
                            tokenManager = app.tokenManager,
                            onDismiss = { pendingHubLink = null }
                        )
                    }
                }
            }
        }
    }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "taskhub") {
            val host = uri.host ?: ""
            val path = uri.path ?: ""
            if (host == "connect-hub" || path.contains("connect-hub")) {
                val sessionCode = uri.getQueryParameter("session_code")
                val callbackUrl = uri.getQueryParameter("callback_url") ?: "ekahub://link-callback"
                if (!sessionCode.isNullOrEmpty()) {
                    pendingHubLink = HubLinkSessionPayload(
                        sessionCode = sessionCode,
                        callbackUrl = callbackUrl
                    )
                }
            } else if (host == "tasks" || host == "task" || path.contains("tasks")) {
                val taskIdStr = uri.getQueryParameter("id")
                    ?: uri.getQueryParameter("task_id")
                    ?: uri.lastPathSegment?.takeIf { it != "tasks" && it != "task" }
                val status = uri.getQueryParameter("status")
                val projectIdStr = uri.getQueryParameter("project_id")
                val search = uri.getQueryParameter("search")
                val filter = uri.getQueryParameter("filter")

                val vm = taskViewModel
                if (vm != null) {
                    val projectId = projectIdStr?.toLongOrNull()
                    val focusOnly = if (filter == "focus") true else null
                    if (status != null || projectId != null || search != null || focusOnly != null) {
                        vm.applyFilters(
                            status = status,
                            projectId = projectId,
                            search = search,
                            focusOnly = focusOnly
                        )
                    }

                    val taskId = taskIdStr?.toLongOrNull()
                    if (taskId != null) {
                        vm.selectTaskById(taskId)
                    }
                }
            }
        }
    }
}

sealed class Screen(val route: String, @StringRes val titleRes: Int) {
    object Tasks : Screen("tasks", R.string.nav_tasks)
    object Projects : Screen("projects", R.string.nav_projects)
    object Calendar : Screen("calendar", R.string.nav_calendar)
    object Notes : Screen("notes", R.string.nav_notes)
    object Reminders : Screen("reminders", R.string.nav_reminders)
    object Profile : Screen("profile", R.string.nav_profile)
    object Mcp : Screen("mcp", R.string.nav_mcp)
}

@Composable
fun MainNavigationScaffold(
    taskViewModel: TaskViewModel,
    projectViewModel: ProjectViewModel,
    calendarViewModel: CalendarViewModel,
    noteViewModel: NoteViewModel,
    reminderViewModel: ReminderViewModel,
    profileViewModel: ProfileViewModel,
    mcpViewModel: McpViewModel,
    onSignOut: () -> Unit
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    Scaffold(
        bottomBar = {
            if (currentRoute != Screen.Mcp.route) {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentRoute == Screen.Tasks.route,
                        onClick = {
                            navController.navigate(Screen.Tasks.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Checklist, contentDescription = null) },
                        label = { Text(stringResource(Screen.Tasks.titleRes)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Projects.route,
                        onClick = {
                            navController.navigate(Screen.Projects.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Folder, contentDescription = null) },
                        label = { Text(stringResource(Screen.Projects.titleRes)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Calendar.route,
                        onClick = {
                            navController.navigate(Screen.Calendar.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.CalendarMonth, contentDescription = null) },
                        label = { Text(stringResource(Screen.Calendar.titleRes)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Notes.route,
                        onClick = {
                            navController.navigate(Screen.Notes.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.EditNote, contentDescription = null) },
                        label = { Text(stringResource(Screen.Notes.titleRes)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Reminders.route,
                        onClick = {
                            navController.navigate(Screen.Reminders.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                        label = { Text(stringResource(Screen.Reminders.titleRes)) }
                    )
                    NavigationBarItem(
                        selected = currentRoute == Screen.Profile.route,
                        onClick = {
                            navController.navigate(Screen.Profile.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(Icons.Default.Person, contentDescription = null) },
                        label = { Text(stringResource(Screen.Profile.titleRes)) }
                    )
                }
            }
        }
    ) { padding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Tasks.route,
            modifier = Modifier.padding(padding)
        ) {
            composable(Screen.Tasks.route) {
                TaskListScreen(
                    viewModel = taskViewModel,
                    onTaskClick = { /* detail */ }
                )
            }
            composable(Screen.Projects.route) {
                ProjectScreen(viewModel = projectViewModel)
            }
            composable(Screen.Calendar.route) {
                CalendarScreen(viewModel = calendarViewModel)
            }
            composable(Screen.Notes.route) {
                NoteScreen(viewModel = noteViewModel)
            }
            composable(Screen.Reminders.route) {
                ReminderScreen(viewModel = reminderViewModel)
            }
            composable(Screen.Profile.route) {
                ProfileScreen(
                    viewModel = profileViewModel,
                    onNavigateToMcp = { navController.navigate(Screen.Mcp.route) },
                    onSignOut = onSignOut
                )
            }
            composable(Screen.Mcp.route) {
                McpScreen(
                    viewModel = mcpViewModel,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
