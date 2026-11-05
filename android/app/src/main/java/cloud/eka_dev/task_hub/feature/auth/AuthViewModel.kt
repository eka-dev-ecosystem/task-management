package cloud.eka_dev.task_hub.feature.auth

import android.content.Context
import android.util.Log
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import cloud.eka_dev.task_hub.R
import cloud.eka_dev.task_hub.core.datastore.TokenManager
import cloud.eka_dev.task_hub.core.model.OAuthLoginRequest
import cloud.eka_dev.task_hub.core.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class AuthState {
    object Idle : AuthState()
    object Loading : AuthState()
    data class Authenticated(val userName: String) : AuthState()
    data class Error(
        val message: String? = null,
        val messageRes: Int? = null,
        val formatArgs: List<Any> = emptyList()
    ) : AuthState()
}

class AuthViewModel(
    private val apiClient: ApiClient,
    private val tokenManager: TokenManager
) : ViewModel() {

    private val _state = MutableStateFlow<AuthState>(AuthState.Idle)
    val state: StateFlow<AuthState> = _state.asStateFlow()

    init {
        checkExistingSession()
    }

    private fun checkExistingSession() {
        viewModelScope.launch {
            val token = tokenManager.getAccessToken()
            if (!token.isNullOrEmpty()) {
                val userName = tokenManager.getUserName().orEmpty()
                _state.value = AuthState.Authenticated(userName)
            }
        }
    }

    fun signInWithGoogle(context: Context, serverClientId: String) {
        viewModelScope.launch {
            _state.value = AuthState.Loading
            try {
                val credentialManager = CredentialManager.create(context)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(serverClientId)
                    .setAutoSelectEnabled(false)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(context, request)
                val credential = result.credential

                if (credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
                    val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
                    val idToken = googleIdTokenCredential.idToken

                    val response = apiClient.apiService.oauthLogin(
                        OAuthLoginRequest(
                            provider = "google",
                            idToken = idToken,
                            name = googleIdTokenCredential.displayName,
                            email = googleIdTokenCredential.id
                        )
                    )

                    if (response.success && response.data?.tokens != null) {
                        val tokens = response.data.tokens
                        val user = response.data.user
                        tokenManager.saveTokens(tokens.accessToken, tokens.refreshToken)
                        tokenManager.saveUserInfo(user.id, user.name, user.email)
                        _state.value = AuthState.Authenticated(user.name)
                    } else {
                        val err = response.message.ifEmpty { null }
                        if (err != null) {
                            _state.value = AuthState.Error(message = err)
                        } else {
                            _state.value = AuthState.Error(messageRes = R.string.auth_error_login_failed)
                        }
                    }
                } else {
                    _state.value = AuthState.Error(
                        messageRes = R.string.auth_error_unsupported_credential,
                        formatArgs = listOf(credential.type)
                    )
                }
            } catch (e: GetCredentialCancellationException) {
                // User intentionally cancelled or dismissed the Google account picker
                Log.d("AuthViewModel", "Google sign-in was cancelled by user: ${e.message}")
                _state.value = AuthState.Idle
            } catch (e: GetCredentialException) {
                Log.e("AuthViewModel", "Google credential error [${e.javaClass.simpleName}]: ${e.message}", e)
                val detail = e.localizedMessage ?: e.message.orEmpty()
                _state.value = AuthState.Error(
                    messageRes = R.string.auth_error_google_failed,
                    formatArgs = listOf(detail)
                )
            } catch (e: Exception) {
                Log.e("AuthViewModel", "Google sign-in unexpected error", e)
                val detail = e.localizedMessage ?: e.message.orEmpty()
                _state.value = AuthState.Error(
                    messageRes = R.string.auth_error_google_failed,
                    formatArgs = listOf(detail)
                )
            }
        }
    }

    fun signOut() {
        viewModelScope.launch {
            tokenManager.clearSession()
            _state.value = AuthState.Idle
        }
    }
}
