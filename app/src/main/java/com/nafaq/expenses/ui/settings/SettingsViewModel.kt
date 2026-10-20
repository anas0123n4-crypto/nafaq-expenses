package com.nafaq.expenses.ui.settings

import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nafaq.expenses.data.AppContainer
import com.nafaq.expenses.data.ai.AiModel
import com.nafaq.expenses.data.ai.AiProviders
import com.nafaq.expenses.util.Formatters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SettingsViewModel(container: AppContainer) : ViewModel() {

    private val store = container.settings
    private val ai = container.ai
    private val repository = container.repository

    val settings = store.state

    private val _models = MutableStateFlow<List<AiModel>>(emptyList())
    val models: StateFlow<List<AiModel>> = _models.asStateFlow()

    private val _loading = MutableStateFlow(false)
    val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private val _status = MutableStateFlow<String?>(null)
    val status: StateFlow<String?> = _status.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    fun setProvider(id: String) = store.setProvider(id)

    fun setApiKey(key: String) = store.setApiKey(key)

    fun setModel(modelId: String) = store.setModel(modelId)

    fun setBaseUrl(url: String) = store.setCustomBaseUrl(url)

    fun setCurrency(currency: String) = store.setCurrency(currency)

    fun setUseAi(enabled: Boolean) = store.setUseAi(enabled)

    fun fetchModels() {
        if (settings.value.apiKey.isBlank()) {
            _status.value = "أدخل مفتاح API أولاً"
            return
        }
        viewModelScope.launch {
            _loading.value = true
            _status.value = null
            ai.listModels()
                .onSuccess { list ->
                    _models.value = list
                    _status.value = if (list.isEmpty()) {
                        "لم يتم العثور على نماذج — تأكد من المفتاح"
                    } else {
                        "تم جلب ${list.size} نموذج"
                    }
                    if (list.isNotEmpty() && settings.value.modelId.isBlank()) {
                        store.setModel(list.first().id)
                    }
                }
                .onFailure { error ->
                    _status.value = "تعذّر جلب النماذج: ${error.message.orEmpty()}"
                }
            _loading.value = false
        }
    }

    fun exportCsv(context: Context) {
        viewModelScope.launch {
            val entries = repository.observeExpenses()
            val all = withContext(Dispatchers.IO) { entries.first() }
            val categories = repository.categories()
            val names = categories.associate { it.id to it.nameAr }
            val builder = StringBuilder()
            builder.append("التاريخ,الوقت,الصنف,القسم,السعر,العملة,النص\n")
            all.forEach { entry ->
                builder.append(
                    listOf(
                        Formatters.fullDate(entry.createdAt),
                        Formatters.time(entry.createdAt),
                        entry.itemName,
                        names[entry.categoryId] ?: "",
                        Formatters.amount(entry.amount),
                        entry.currency,
                        entry.rawText ?: ""
                    ).joinToString(",") { field -> "\"${field.replace("\"", "\"\"")}\"" }
                )
                builder.append('\n')
            }
            withContext(Dispatchers.IO) {
                val file = File(context.cacheDir, "nafaq-expenses.csv")
                file.writeText(builder.toString())
                val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
                val intent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/csv"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    putExtra(Intent.EXTRA_SUBJECT, "تقرير مصروفات نَفَق")
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                context.startActivity(
                    Intent.createChooser(intent, "تصدير المصروفات")
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            _message.value = "تم تجهيز ملف التصدير"
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repository.clearAll()
            _message.value = "تم حذف جميع المصروفات"
        }
    }

    fun clearMessage() {
        _message.value = null
    }

    fun providerLabel(): String = AiProviders.label(settings.value.providerId)
}
