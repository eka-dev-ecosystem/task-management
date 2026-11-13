package cloud.eka_dev.task_hub.feature.hub

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cloud.eka_dev.task_hub.core.datastore.TokenManager
import cloud.eka_dev.task_hub.core.designsystem.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class HubLinkSessionPayload(
    val sessionCode: String,
    val callbackUrl: String = "ekahub://link-callback"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HubLinkConfirmationBottomSheet(
    payload: HubLinkSessionPayload,
    tokenManager: TokenManager,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var userName by remember { mutableStateOf("Pengguna Task Hub") }
    var userEmail by remember { mutableStateOf("") }
    var userId by remember { mutableStateOf("") }
    var token by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        userId = tokenManager.getUserId() ?: ""
        userName = tokenManager.getUserName() ?: "Pengguna Task Hub"
        userEmail = tokenManager.getUserEmail() ?: ""
        token = tokenManager.getAccessToken() ?: ""
    }

    ModalBottomSheet(
        onDismissRequest = {
            if (!isLoading) {
                launchCallback(context, payload.callbackUrl, status = "cancelled")
                onDismiss()
            }
        },
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surface,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(8.dp))

            // Logos connection
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                BrandLogo(size = 56.dp)

                Spacer(modifier = Modifier.width(16.dp))
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))

                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                colors = listOf(Color(0xFF3B82F6), Color(0xFF8B5CF6))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "Hubungkan ke Personal AI Hub",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Personal AI Hub meminta izin membaca ringkasan tugas dan status deadline Anda untuk asisten AI.",
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                lineHeight = 18.sp
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Account info card
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(BrandPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (userName.isNotEmpty()) userName.take(1).uppercase() else "T",
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary,
                            fontSize = 16.sp
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = userName,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (userEmail.isNotEmpty()) {
                            Text(
                                text = userEmail,
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = StatusDone.copy(alpha = 0.15f)
                    ) {
                        Text(
                            text = "Aktif",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = StatusDone,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (errorMessage != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.5f))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = errorMessage ?: "",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            AppButton(
                text = "Izinkan Akses",
                onClick = {
                    if (token.isEmpty()) {
                        errorMessage = "Sesi akun tidak aktif. Harap masuk ulang."
                        return@AppButton
                    }
                    isLoading = true
                    errorMessage = null

                    scope.launch {
                        val success = performExchange(
                            sessionCode = payload.sessionCode,
                            uid = if (userId.isNotEmpty()) userId else userEmail,
                            email = if (userEmail.isNotEmpty()) userEmail else "user@taskhub.eka-dev.cloud",
                            token = token
                        )
                        isLoading = false
                        if (success) {
                            launchCallback(context, payload.callbackUrl, status = "success")
                            onDismiss()
                        } else {
                            errorMessage = "Gagal menghubungkan ke Hub. Pastikan koneksi internet stabil."
                        }
                    }
                },
                isLoading = isLoading,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(10.dp))

            TextButton(
                onClick = {
                    launchCallback(context, payload.callbackUrl, status = "cancelled")
                    onDismiss()
                },
                enabled = !isLoading
            ) {
                Text(
                    text = "Batalkan",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

private suspend fun performExchange(
    sessionCode: String,
    uid: String,
    email: String,
    token: String
): Boolean = withContext(Dispatchers.IO) {
    try {
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .build()

        val json = JSONObject().apply {
            put("session_code", sessionCode)
            put("app_id", "taskmanagement")
            put("app_name", "Task Management")
            put("connected_uid", uid)
            put("connected_email", email)
            put("api_base_url", "https://be-task-hub.eka-dev.cloud")
            put("read_only_token", token)
            put("custom_url_scheme", "taskhub://")
        }

        val body = json.toString().toRequestBody("application/json".toMediaType())
        val request = Request.Builder()
            .url("https://hub-api.eka-dev.cloud/api/v1/hub/link/exchange")
            .post(body)
            .build()

        val response = client.newCall(request).execute()
        response.isSuccessful
    } catch (e: Exception) {
        false
    }
}

private fun launchCallback(context: Context, callbackUrl: String, status: String) {
    try {
        val baseUri = Uri.parse(callbackUrl)
        val finalUri = baseUri.buildUpon()
            .appendQueryParameter("status", status)
            .appendQueryParameter("app_id", "taskmanagement")
            .build()

        val intent = Intent(Intent.ACTION_VIEW, finalUri).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}
