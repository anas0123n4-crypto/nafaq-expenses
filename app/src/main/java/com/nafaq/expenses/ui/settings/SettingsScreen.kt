package com.nafaq.expenses.ui.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DeleteForever
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.ExpandMore
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nafaq.expenses.data.ai.AiProviders
import com.nafaq.expenses.ui.NafaqViewModelFactory
import com.nafaq.expenses.ui.components.SectionCard
import com.nafaq.expenses.ui.components.SectionTitle

private val currencyOptions = listOf("دينار", "ريال", "دولار", "شيكل", "درهم", "ليرة", "جنيه")

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = viewModel(factory = NafaqViewModelFactory)) {
    val context = LocalContext.current
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val models by viewModel.models.collectAsStateWithLifecycle()
    val loading by viewModel.loading.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    var apiKey by remember { mutableStateOf(settings.apiKey) }
    var keyVisible by remember { mutableStateOf(false) }
    var baseUrl by remember { mutableStateOf(settings.customBaseUrl) }
    var modelMenuExpanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Text("الإعدادات", style = MaterialTheme.typography.headlineMedium)
            }
            item {
                SectionCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle(text = "الذكاء الاصطناعي")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "أدخل مفتاح API فقط وسيتعرّف التطبيق على كل الأصناف بلا قاموس",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(14.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            AiProviders.GEMINI to "Google Gemini",
                            AiProviders.OPENROUTER to "OpenRouter",
                            AiProviders.CUSTOM to "مزوّد مخصص"
                        ).forEach { (id, label) ->
                            OptionPill(
                                label = label,
                                selected = settings.providerId == id,
                                onClick = { viewModel.setProvider(id) }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = apiKey,
                        onValueChange = {
                            apiKey = it
                            viewModel.setApiKey(it)
                        },
                        label = { Text("مفتاح API") },
                        singleLine = true,
                        shape = MaterialTheme.shapes.medium,
                        visualTransformation = if (keyVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                        trailingIcon = {
                            Box(modifier = Modifier.clickable { keyVisible = !keyVisible }) {
                                Icon(
                                    imageVector = if (keyVisible) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = if (keyVisible) "إخفاء" else "إظهار",
                                    modifier = Modifier
                                        .padding(12.dp)
                                        .size(20.dp)
                                )
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (settings.providerId == AiProviders.CUSTOM) {
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = baseUrl,
                            onValueChange = {
                                baseUrl = it
                                viewModel.setBaseUrl(it)
                            },
                            label = { Text("عنوان الخادم (Base URL)") },
                            singleLine = true,
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedButton(
                        onClick = { viewModel.fetchModels() },
                        enabled = !loading,
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        if (loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جاري جلب النماذج…")
                        } else {
                            Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("جلب النماذج تلقائياً")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Box {
                        OutlinedButton(
                            onClick = { modelMenuExpanded = true },
                            shape = MaterialTheme.shapes.medium,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val selected = models.firstOrNull { it.id == settings.modelId }
                            Text(
                                text = selected?.label ?: settings.modelId.ifBlank { "اختر النموذج" },
                                modifier = Modifier.weight(1f),
                                maxLines = 1
                            )
                            Icon(Icons.Rounded.ExpandMore, contentDescription = null)
                        }
                        DropdownMenu(
                            expanded = modelMenuExpanded,
                            onDismissRequest = { modelMenuExpanded = false }
                        ) {
                            if (models.isEmpty()) {
                                DropdownMenuItem(
                                    text = { Text("اضغط «جلب النماذج» أولاً") },
                                    onClick = { modelMenuExpanded = false }
                                )
                            } else {
                                models.forEach { model ->
                                    DropdownMenuItem(
                                        text = { Text(model.label) },
                                        onClick = {
                                            viewModel.setModel(model.id)
                                            modelMenuExpanded = false
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (status != null) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = status.orEmpty(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text("تحليل ذكي عند التسجيل", style = MaterialTheme.typography.titleSmall)
                            Text(
                                text = "عند الإيقاف يعتمد التطبيق على القاموس المحلي فقط",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = settings.useAi,
                            onCheckedChange = { viewModel.setUseAi(it) }
                        )
                    }
                }
            }
            item {
                SectionCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle(text = "العملة الافتراضية")
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        currencyOptions.forEach { option ->
                            OptionPill(
                                label = option,
                                selected = settings.currency == option,
                                onClick = { viewModel.setCurrency(option) }
                            )
                        }
                    }
                }
            }
            item {
                SectionCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle(text = "البيانات")
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedButton(
                        onClick = { viewModel.exportCsv(context) },
                        shape = MaterialTheme.shapes.medium,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("تصدير كل المصروفات (CSV)")
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { confirmDelete = true },
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer,
                            contentColor = MaterialTheme.colorScheme.onErrorContainer
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Rounded.DeleteForever, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("حذف جميع المصروفات")
                    }
                }
            }
            item {
                SectionCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle(text = "عن التطبيق")
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "نَفَق — دفتر مصروفات ذكي يتكلّم لغتك.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "الإصدار 1.0.0 • يعمل بدون إنترنت، ومفتاح الذكاء الاصطناعي يبقى على جهازك.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("حذف جميع المصروفات؟") },
            text = { Text("لا يمكن التراجع عن هذه العملية.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    viewModel.clearAll()
                }) { Text("حذف") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDelete = false }) { Text("إلغاء") }
            }
        )
    }
}

@Composable
private fun OptionPill(
    label: String,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(percent = 50))
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.18f)
                else MaterialTheme.colorScheme.surfaceVariant
            )
            .clickable { onClick() }
            .padding(horizontal = 14.dp, vertical = 8.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
