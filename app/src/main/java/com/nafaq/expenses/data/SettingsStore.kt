package com.nafaq.expenses.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class NafaqSettings(
    val providerId: String = PROVIDER_GEMINI,
    val apiKey: String = "",
    val modelId: String = "",
    val customBaseUrl: String = "https://api.openai.com/v1",
    val currency: String = "دينار",
    val useAi: Boolean = true,
    val lastCategoryId: String = ""
) {
    val hasKey: Boolean get() = apiKey.isNotBlank()

    companion object {
        const val PROVIDER_GEMINI = "gemini"
        const val PROVIDER_OPENROUTER = "openrouter"
        const val PROVIDER_CUSTOM = "custom"
    }
}

class SettingsStore(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("nafaq_settings", Context.MODE_PRIVATE)

    private val _state = MutableStateFlow(read())
    val state: StateFlow<NafaqSettings> = _state.asStateFlow()

    private fun read(): NafaqSettings = NafaqSettings(
        providerId = prefs.getString(KEY_PROVIDER, NafaqSettings.PROVIDER_GEMINI) ?: NafaqSettings.PROVIDER_GEMINI,
        apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
        modelId = prefs.getString(KEY_MODEL, "") ?: "",
        customBaseUrl = prefs.getString(KEY_BASE_URL, "https://api.openai.com/v1") ?: "https://api.openai.com/v1",
        currency = prefs.getString(KEY_CURRENCY, "دينار") ?: "دينار",
        useAi = prefs.getBoolean(KEY_USE_AI, true),
        lastCategoryId = prefs.getString(KEY_LAST_CATEGORY, "") ?: ""
    )

    private fun publish() {
        _state.value = read()
    }

    fun setProvider(id: String) {
        prefs.edit().putString(KEY_PROVIDER, id).apply()
        publish()
    }

    fun setApiKey(key: String) {
        prefs.edit().putString(KEY_API_KEY, key.trim()).apply()
        publish()
    }

    fun setModel(modelId: String) {
        prefs.edit().putString(KEY_MODEL, modelId).apply()
        publish()
    }

    fun setCustomBaseUrl(url: String) {
        prefs.edit().putString(KEY_BASE_URL, url.trim()).apply()
        publish()
    }

    fun setCurrency(currency: String) {
        prefs.edit().putString(KEY_CURRENCY, currency).apply()
        publish()
    }

    fun setUseAi(enabled: Boolean) {
        prefs.edit().putBoolean(KEY_USE_AI, enabled).apply()
        publish()
    }

    fun setLastCategory(id: String) {
        prefs.edit().putString(KEY_LAST_CATEGORY, id).apply()
        publish()
    }

    private companion object {
        const val KEY_PROVIDER = "provider"
        const val KEY_API_KEY = "api_key"
        const val KEY_MODEL = "model"
        const val KEY_BASE_URL = "base_url"
        const val KEY_CURRENCY = "currency"
        const val KEY_USE_AI = "use_ai"
        const val KEY_LAST_CATEGORY = "last_category"
    }
}
