package com.nafaq.expenses.ui.stats

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nafaq.expenses.data.AppContainer
import com.nafaq.expenses.data.db.CategoryEntity
import com.nafaq.expenses.data.db.ExpenseEntity
import com.nafaq.expenses.ui.search.ItemGroup
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.YearMonth
import java.time.ZoneId

data class DayPoint(val day: Int, val total: Double)

data class CategoryPoint(
    val id: String,
    val name: String,
    val colorHex: String,
    val total: Double,
    val count: Int
)

data class MonthStats(
    val total: Double = 0.0,
    val count: Int = 0,
    val currency: String = "دينار",
    val daily: List<DayPoint> = emptyList(),
    val byCategory: List<CategoryPoint> = emptyList(),
    val topItems: List<ItemGroup> = emptyList(),
    val averagePerDay: Double = 0.0
)

class StatsViewModel(container: AppContainer) : ViewModel() {

    private val repository = container.repository

    private val _month = MutableStateFlow(YearMonth.now())
    val month: StateFlow<YearMonth> = _month.asStateFlow()

    private val _stats = MutableStateFlow(MonthStats())
    val stats: StateFlow<MonthStats> = _stats.asStateFlow()

    val categories: StateFlow<List<CategoryEntity>> = repository.observeCategories()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            combine(
                repository.observeExpenses(),
                repository.observeCategories(),
                _month
            ) { entries, categories, month ->
                compute(entries, categories, month)
            }.collect { computed ->
                _stats.value = computed
            }
        }
    }

    fun previousMonth() {
        _month.value = _month.value.minusMonths(1)
    }

    fun nextMonth() {
        _month.value = _month.value.plusMonths(1)
    }

    private fun compute(
        entries: List<ExpenseEntity>,
        categories: List<CategoryEntity>,
        month: YearMonth
    ): MonthStats {
        val zone = ZoneId.systemDefault()
        val start = month.atDay(1).atStartOfDay(zone).toInstant().toEpochMilli()
        val end = month.atDay(month.lengthOfMonth()).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() - 1
        val matched = entries.filter { it.createdAt in start..end }

        val names = categories.associate { it.id to it }
        val total = matched.sumOf { it.amount }
        val currency = matched.groupingBy { it.currency }.eachCount().maxByOrNull { it.value }?.key ?: "دينار"

        val dailyTotals = matched.groupBy { entry ->
            java.time.Instant.ofEpochMilli(entry.createdAt).atZone(zone).dayOfMonth
        }.mapValues { (_, list) -> list.sumOf { it.amount } }

        val daily = (1..month.lengthOfMonth()).map { day ->
            DayPoint(day = day, total = dailyTotals[day] ?: 0.0)
        }

        val byCategory = matched.groupBy { it.categoryId }
            .map { (id, list) ->
                val category = names[id]
                CategoryPoint(
                    id = id,
                    name = category?.nameAr ?: "أخرى",
                    colorHex = category?.colorHex ?: "#9A9186",
                    total = list.sumOf { it.amount },
                    count = list.size
                )
            }
            .sortedByDescending { it.total }

        val topItems = matched.groupBy { it.itemName }
            .map { (name, list) -> ItemGroup(name = name, count = list.size, total = list.sumOf { it.amount }) }
            .sortedByDescending { it.total }
            .take(8)

        return MonthStats(
            total = total,
            count = matched.size,
            currency = currency,
            daily = daily,
            byCategory = byCategory,
            topItems = topItems,
            averagePerDay = if (matched.isEmpty()) 0.0 else total / month.lengthOfMonth()
        )
    }
}
