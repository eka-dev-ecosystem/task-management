package cloud.eka_dev.task_hub.core.designsystem

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import cloud.eka_dev.task_hub.core.datastore.TokenManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Locale

object LanguageManager {
    private val _currentLanguage = MutableStateFlow("id")
    val currentLanguage: StateFlow<String> = _currentLanguage.asStateFlow()

    private var tokenManager: TokenManager? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    fun initialize(tokenMgr: TokenManager) {
        tokenManager = tokenMgr
        scope.launch {
            val saved = tokenMgr.getAppLanguage()
            _currentLanguage.value = saved
            applyLocale(saved)
        }
    }

    fun setLanguage(lang: String) {
        _currentLanguage.value = lang
        applyLocale(lang)
        tokenManager?.let { tm ->
            scope.launch {
                tm.saveAppLanguage(lang)
            }
        }
    }

    fun toggleLanguage() {
        if (_currentLanguage.value == "id" || _currentLanguage.value == "in") {
            setLanguage("en")
        } else {
            setLanguage("id")
        }
    }

    private fun applyLocale(lang: String) {
        val locale = if (lang.equals("id", ignoreCase = true) || lang.equals("in", ignoreCase = true)) {
            Locale("in", "ID")
        } else {
            Locale("en", "US")
        }
        Locale.setDefault(locale)
    }

    fun getLocale(): Locale {
        return if (_currentLanguage.value.equals("id", ignoreCase = true) || _currentLanguage.value.equals("in", ignoreCase = true)) {
            Locale("in", "ID")
        } else {
            Locale("en", "US")
        }
    }
}

@Composable
fun ProvideAppLocale(
    language: String,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val locale = remember(language) {
        if (language.equals("id", ignoreCase = true) || language.equals("in", ignoreCase = true)) {
            Locale("in", "ID")
        } else {
            Locale("en", "US")
        }
    }

    val configuration = LocalConfiguration.current
    val updatedConfig = remember(locale, configuration) {
        Configuration(configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
    }

    val localizedContext = remember(context, locale) {
        val conf = Configuration(context.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        context.createConfigurationContext(conf)
    }

    CompositionLocalProvider(
        LocalConfiguration provides updatedConfig,
        LocalContext provides localizedContext,
        content = content
    )
}
