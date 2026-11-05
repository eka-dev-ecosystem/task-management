package cloud.eka_dev.task_hub.feature.auth

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cloud.eka_dev.task_hub.BuildConfig
import cloud.eka_dev.task_hub.R
import cloud.eka_dev.task_hub.core.designsystem.*

@Composable
fun AuthScreen(
    viewModel: AuthViewModel,
    onAuthSuccess: () -> Unit
) {
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(state) {
        if (state is AuthState.Authenticated) {
            onAuthSuccess()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 40.dp)
        ) {
            Spacer(modifier = Modifier.height(20.dp))

            // Brand Logo & Header
            BrandLogo(size = 80.dp)

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Task Hub",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = BrandPrimary
            )

            Text(
                text = stringResource(R.string.auth_tagline),
                fontSize = 15.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 4.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Feature Highlights Box
            Card(
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    FeatureHighlightRow(
                        icon = Icons.Default.CheckCircle,
                        title = stringResource(R.string.feature_realtime_title),
                        desc = stringResource(R.string.feature_realtime_desc),
                        color = BrandPrimary
                    )
                    FeatureHighlightRow(
                        icon = Icons.Default.Folder,
                        title = stringResource(R.string.feature_projects_title),
                        desc = stringResource(R.string.feature_projects_desc),
                        color = BrandViolet
                    )
                    FeatureHighlightRow(
                        icon = Icons.Default.NotificationsActive,
                        title = stringResource(R.string.feature_reminders_title),
                        desc = stringResource(R.string.feature_reminders_desc),
                        color = PriorityUrgent
                    )
                }
            }

            Spacer(modifier = Modifier.height(28.dp))

            if (state is AuthState.Error) {
                val errorState = state as AuthState.Error
                val errorMessage = when {
                    errorState.messageRes != null -> stringResource(
                        errorState.messageRes,
                        *errorState.formatArgs.toTypedArray()
                    )
                    !errorState.message.isNullOrEmpty() -> errorState.message
                    else -> stringResource(R.string.auth_error_unknown)
                }
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                ) {
                    Text(
                        text = errorMessage,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(14.dp),
                        fontSize = 13.sp
                    )
                }
            }

            // Google 1-Tap Sign-In
            AppButton(
                text = stringResource(R.string.sign_in_google),
                onClick = {
                    val activityContext = context.findActivity() ?: context
                    viewModel.signInWithGoogle(activityContext, BuildConfig.GOOGLE_WEB_CLIENT_ID)
                },
                isLoading = state is AuthState.Loading,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
fun FeatureHighlightRow(
    icon: ImageVector,
    title: String,
    desc: String,
    color: Color
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
        }
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private fun Context.findActivity(): Activity? {
    var currentContext = this
    while (currentContext is ContextWrapper) {
        if (currentContext is Activity) return currentContext
        currentContext = currentContext.baseContext
    }
    return null
}
