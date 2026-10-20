package com.nafaq.expenses.ui.home

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nafaq.expenses.data.parser.ParsedDraft
import com.nafaq.expenses.ui.NafaqViewModelFactory
import com.nafaq.expenses.ui.components.EmptyState
import com.nafaq.expenses.ui.components.ExpenseCard
import com.nafaq.expenses.ui.components.MicButton
import com.nafaq.expenses.ui.components.PreviewSheetContent
import com.nafaq.expenses.ui.components.SectionCard
import com.nafaq.expenses.ui.components.SectionTitle
import com.nafaq.expenses.util.Formatters
import com.nafaq.expenses.util.VoiceController
import com.nafaq.expenses.util.VoiceState
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(viewModel: HomeViewModel = viewModel(factory = NafaqViewModelFactory)) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val todayEntries by viewModel.todayEntries.collectAsStateWithLifecycle()
    val todayTotal by viewModel.todayTotal.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    val voice = remember { VoiceController(context) }
    val voiceState = voice.state
    var draft by remember { mutableStateOf<ParsedDraft?>(null) }

    DisposableEffect(Unit) {
        onDispose { voice.destroy() }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            voice.start()
        } else {
            scope.launch { snackbarHostState.showSnackbar("يحتاج التطبيق إذن الميكروفون") }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is HomeEvent.ShowPreview -> draft = event.draft
            }
        }
    }

    LaunchedEffect(voiceState) {
        val current = voiceState
        if (current is VoiceState.Final) {
            viewModel.process(current.text)
            voice.reset()
        }
    }

    LaunchedEffect(message) {
        val text = message ?: return@LaunchedEffect
        snackbarHostState.showSnackbar(text)
        viewModel.clearMessage()
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                HomeHeader(
                    total = todayTotal,
                    count = todayEntries.size,
                    currency = settings.currency
                )
            }
            item {
                MicHero(voiceState = voiceState, onMicClick = {
                    when (voiceState) {
                        is VoiceState.Listening, is VoiceState.Partial -> voice.stop()
                        else -> {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                voice.start()
                            } else {
                                permissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    }
                })
            }
            item {
                SectionTitle(text = "سجل اليوم", trailing = "${todayEntries.size} عملية")
            }
            if (todayEntries.isEmpty()) {
                item {
                    EmptyState(
                        emoji = "🧾",
                        title = "لا توجد مصروفات اليوم",
                        subtitle = "اضغط الميكروفون وتكلّم لتسجيل أول مصروف"
                    )
                }
            } else {
                items(items = todayEntries, key = { it.id }) { entry ->
                    ExpenseCard(
                        entry = entry,
                        category = categories.firstOrNull { it.id == entry.categoryId },
                        onDelete = { viewModel.delete(entry) }
                    )
                }
            }
        }

        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter)
        )
    }

    draft?.let { current ->
        ModalBottomSheet(
            onDismissRequest = { draft = null },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = MaterialTheme.colorScheme.surface,
            dragHandle = { BottomSheetDefaults.DragHandle(color = MaterialTheme.colorScheme.outline) }
        ) {
            PreviewSheetContent(
                draft = current,
                categories = categories,
                defaultCurrency = settings.currency,
                onSave = {
                    viewModel.save(it)
                    draft = null
                },
                onCancel = { draft = null },
                onReRecord = {
                    draft = null
                    voice.start()
                }
            )
        }
    }
}

@Composable
private fun HomeHeader(total: Double, count: Int, currency: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("نَفَق", style = MaterialTheme.typography.headlineMedium)
            Text(
                text = Formatters.dayLabel(System.currentTimeMillis()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(18.dp))
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f))
                .padding(horizontal = 16.dp, vertical = 10.dp),
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "مصروفات اليوم ($count)",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "${Formatters.amount(total)} $currency",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun MicHero(voiceState: VoiceState, onMicClick: () -> Unit) {
    val isListening = voiceState is VoiceState.Listening || voiceState is VoiceState.Partial
    SectionCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            MicButton(isListening = isListening, onClick = onMicClick)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = when {
                    isListening -> "أنا أستمع إليك…"
                    voiceState is VoiceState.Error -> "حدث خطأ"
                    else -> "اضغط وتكلّم"
                },
                style = MaterialTheme.typography.titleMedium,
                textAlign = TextAlign.Center
            )
            val transcript = when (voiceState) {
                is VoiceState.Partial -> voiceState.text
                is VoiceState.Final -> voiceState.text
                is VoiceState.Error -> voiceState.message
                else -> null
            }
            if (transcript != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "«$transcript»",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "مثال: اشتريت سدر كنافه بعشر دنانير",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
}
