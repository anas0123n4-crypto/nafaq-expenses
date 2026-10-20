package com.nafaq.expenses.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nafaq.expenses.data.AppContainer
import com.nafaq.expenses.data.db.CategoryEntity
import com.nafaq.expenses.data.db.ExpenseEntity
import com.nafaq.expenses.data.parser.ArabicNormalizer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.ZoneId

data class DayGroup(
    val label: String,
    val entries: List<ExpenseEntity>,
    val total: Double
)

data class ItemGroup(
    val name: String,
    val count: Int,
    val total: Double
)

data class SearchResults(
    val groups: List<DayGroup> = emptyList(),
    val items: List<ItemGroup> = emptyList(),
    val total: Double = 0.0,
    val count: Int = 0,
    val currency: String = "دينار"
)

data class SearchFilters(
    val query: String = "",
    val from: LocalDate = LocalDate.now().minusDays(30),
    val to: LocalDate = LocalDate.now(),
    val categoryIds: Set<String> = emptySet()
)

class SearchViewModel(container: AppContainer) : ViewModel() {

    private val repository = container.repository

    val categories: StateFlow<List<CategoryEntity>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _filters = MutableStateFlow(SearchFilters())
    val filters: StateFlow<SearchFilters> = _filters.asStateFlow()

    private val _results = MutableStateFlow(SearchResults())
    val results: StateFlow<SearchResults> = _results.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                repository.observeExpenses(),
                repository.observeCategories(),
                _filters
            ) { entries, categories, filters ->
                compute(entries, categories, filters)
            }.collect { computed ->
                _results.value = computed
            }
        }
    }

    fun setQuery(query: String) {
        _filters.value = _filters.value.copy(query = query)
    }

    fun setRange(from: LocalDate, to: LocalDate) {
        val safeFrom = if (from.isAfter(to)) to else from
        val safeTo = if (to.isBefore(safeFrom)) safeFrom else to
        _filters.value = _filters.value.copy(from = safeFrom, to = safeTo)
    }

    fun toggleCategory(id: String) {
        val current = _filters.value.categoryIds
        val updated = if (id in current) current - id else current + id
        _filters.value = _filters.value.copy(categoryIds = updated)
    }

    fun clearCategories() {
        _filters.value = _filters.value.copy(categoryIds = emptySet())
    }

    private fun compute(
        entries: List<ExpenseEntity>,
        categories: List<CategoryEntity>,
        filters: SearchFilters
    ): SearchResults {
        val zone = ZoneId.systemDefault()
        val fromMillis = filters.from.atStartOfDay(zone).toInstant().toEpochMilli()
        val toMillis = filters.to.plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val query = ArabicNormalizer.fold(filters.query).trim()

        val names = categories.associate { it.id to it.nameAr }

        val matched = entries.filter { entry ->
            if (entry.createdAt < fromMillis || entry.createdAt > toMillis) return@filter false
            if (filters.categoryIds.isNotEmpty() && entry.categoryId !in filters.categoryIds) {
                return@filter false
            }
            if (query.isEmpty()) return@filter true
            val haystack = buildString {
                append(ArabicNormalizer.fold(entry.itemName)).append(' ')
                append(ArabicNormalizer.fold(names[entry.categoryId] ?: "")).append(' ')
                append(ArabicNormalizer.fold(entry.rawText ?: ""))
            }
            haystack.contains(query)
        }

        val total = matched.sumOf { it.amount }
        val currency = matched.groupingBy { it.currency }.eachCount()
            .maxByOrNull { it.value }?.key ?: "دينار"

        val groups = matched
            .groupBy { dayKey(it.createdAt) }
            .toList()
            .sortedByDescending { it.first }
            .map { (key, list) ->
                DayGroup(
                    label = com.nafaq.expenses.util.Formatters.dayLabel(key),
                    entries = list.sortedByDescending { it.createdAt },
                    total = list.sumOf { it.amount }
                )
            }

        val items = matched
            .groupBy { it.itemName }
            .map { (name, list) ->
                ItemGroup(name = name, count = list.size, total = list.sumOf { it.amount })
            }
            .sortedByDescending { it.total }

        return SearchResults(
            groups = groups,
            items = items,
            total = total,
            count = matched.size,
            currency = currency
        )
    }

    private fun dayKey(millis: Long): Long {
        val calendar = java.util.Calendar.getInstance()
        calendar.timeInMillis = millis
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 0)
        calendar.set(java.util.Calendar.MINUTE, 0)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
