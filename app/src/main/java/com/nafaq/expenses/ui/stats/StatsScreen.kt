package com.nafaq.expenses.ui.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nafaq.expenses.ui.NafaqViewModelFactory
import com.nafaq.expenses.ui.components.SectionCard
import com.nafaq.expenses.ui.components.SectionTitle
import com.nafaq.expenses.ui.theme.NafaqGold
import com.nafaq.expenses.ui.theme.hexToColor
import com.nafaq.expenses.util.Formatters

@Composable
fun StatsScreen(viewModel: StatsViewModel = viewModel(factory = NafaqViewModelFactory)) {
    val month by viewModel.month.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("الإحصائيات", style = MaterialTheme.typography.headlineMedium)
                        Text(
                            text = Formatters.monthLabel(month.year, month.monthValue - 1),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { viewModel.previousMonth() }) {
                        Icon(Icons.Rounded.ChevronRight, contentDescription = "الشهر السابق")
                    }
                    IconButton(onClick = { viewModel.nextMonth() }) {
                        Icon(Icons.Rounded.ChevronLeft, contentDescription = "الشهر التالي")
                    }
                }
            }
            item {
                SectionCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = "إجمالي الشهر",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        Text(
                            text = Formatters.amount(stats.total),
                            style = MaterialTheme.typography.displaySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = stats.currency,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                        StatPill(label = "عدد العمليات", value = "${stats.count}")
                        StatPill(label = "متوسط اليوم", value = Formatters.amount(stats.averagePerDay))
                    }
                }
            }
            item {
                SectionCard(modifier = Modifier.fillMaxWidth()) {
                    SectionTitle(text = "المصروف اليومي")
                    Spacer(modifier = Modifier.height(14.dp))
                    DailyChart(points = stats.daily)
                }
            }
            if (stats.byCategory.isNotEmpty()) {
                item {
                    SectionCard(modifier = Modifier.fillMaxWidth()) {
                        SectionTitle(text = "حسب القسم")
                        Spacer(modifier = Modifier.height(10.dp))
                        val max = stats.byCategory.maxOf { it.total }.coerceAtLeast(1.0)
                        stats.byCategory.forEach { point ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(hexToColor(point.colorHex))
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = point.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${Formatters.amount(point.total)} ${stats.currency}",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            LinearProgressIndicator(
                                progress = { (point.total / max).toFloat().coerceIn(0f, 1f) },
                                color = hexToColor(point.colorHex),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                        }
                    }
                }
            }
            if (stats.topItems.isNotEmpty()) {
                item {
                    SectionCard(modifier = Modifier.fillMaxWidth()) {
                        SectionTitle(text = "الأكثر شراءً")
                        Spacer(modifier = Modifier.height(8.dp))
                        stats.topItems.forEach { item ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = item.name,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    text = "${item.count} مرة",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Text(
                                    text = Formatters.amount(item.total),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StatPill(label: String, value: String) {
    Column(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant)
            .padding(horizontal = 14.dp, vertical = 10.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(text = value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun DailyChart(points: List<DayPoint>, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxWidth().height(140.dp)) {
        val max = points.maxOfOrNull { it.total } ?: 0.0
        if (points.isEmpty() || max <= 0.0) return@Canvas
        val slot = size.width / points.size
        val gap = slot * 0.3f
        points.forEachIndexed { index, point ->
            val ratio = (point.total / max).toFloat().coerceIn(0f, 1f)
            val height = size.height * ratio
            drawRoundRect(
                color = NafaqGold,
                topLeft = Offset(index * slot + gap / 2f, size.height - height),
                size = Size(slot - gap, height.coerceAtLeast(2f)),
                cornerRadius = CornerRadius(4f, 4f)
            )
        }
    }
}
