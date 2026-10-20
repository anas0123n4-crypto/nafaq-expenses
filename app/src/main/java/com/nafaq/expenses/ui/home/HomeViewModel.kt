package com.nafaq.expenses.ui.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nafaq.expenses.data.AppContainer
import com.nafaq.expenses.data.db.CategoryEntity
import com.nafaq.expenses.data.db.ExpenseEntity
import com.nafaq.expenses.data.parser.ParsedDraft
import com.nafaq.expenses.util.Formatters
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface HomeEvent {
    data class ShowPreview(val draft: ParsedDraft) : HomeEvent
}

class HomeViewModel(private val container: AppContainer) : ViewModel() {

    private val repository = container.repository
    private val parser = container.parser
    private val ai = container.ai

    val settings = container.settings.state

    val entries: StateFlow<List<ExpenseEntity>> = repository.observeExpenses()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val categories: StateFlow<List<CategoryEntity>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayEntries: StateFlow<List<ExpenseEntity>> = entries
        .map { list -> list.filter { Formatters.isToday(it.createdAt) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val todayTotal: StateFlow<Double> = todayEntries
        .map { list -> list.sumOf { it.amount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0.0)

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy.asStateFlow()

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    fun process(text: String) {
        if (text.isBlank()) return
        viewModelScope.launch {
            _busy.value = true
            try {
                var draft = parser.parse(text)
                val current = settings.value
                if (current.useAi && current.hasKey) {
                    val knownCategories = categories.value
                    ai.parse(text, knownCategories).onSuccess { suggestion ->
                        val validCategory = suggestion.categoryId?.let { id ->
                            knownCategories.firstOrNull { it.id == id }?.id
                        }
                        draft = draft.copy(
                            itemName = suggestion.itemName?.takeIf { it.isNotBlank() } ?: draft.itemName,
                            categoryId = validCategory ?: draft.categoryId,
                            amount = draft.amount ?: suggestion.amount,
                            currency = draft.currency ?: suggestion.currency
                        )
                    }.onFailure { error ->
                        _message.value = "تعذّر الاتصال بالذكاء الاصطناعي: ${error.message.orEmpty()}"
                    }
                }
                if (draft.amount == null) draft = draft.copy(amount = 0.0)
                if (draft.currency == null) draft = draft.copy(currency = settings.value.currency)
                _events.send(HomeEvent.ShowPreview(draft))
            } finally {
                _busy.value = false
            }
        }
    }

    fun save(draft: ParsedDraft) {
        viewModelScope.launch {
            repository.add(
                ExpenseEntity(
                    itemName = draft.itemName,
                    categoryId = draft.categoryId ?: "other",
                    amount = draft.amount ?: 0.0,
                    currency = draft.currency ?: settings.value.currency,
                    rawText = draft.rawText,
                    createdAt = System.currentTimeMillis(),
                    source = draft.source
                )
            )
            _message.value = "تم تسجيل المصروف ✓"
        }
    }

    fun delete(entry: ExpenseEntity) {
        viewModelScope.launch {
            repository.remove(entry)
            _message.value = "تم حذف المصروف"
        }
    }

    fun clearMessage() {
        _message.value = null
    }
}
