package com.nafaq.expenses.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nafaq.expenses.data.db.CategoryEntity
import com.nafaq.expenses.data.parser.ParsedDraft
import com.nafaq.expenses.util.Formatters

private val currencyOptions = listOf("دينار", "ريال", "دولار", "شيكل", "درهم", "ليرة", "جنيه")

@Composable
fun PreviewSheetContent(
    draft: ParsedDraft,
    categories: List<CategoryEntity>,
    defaultCurrency: String,
    onSave: (ParsedDraft) -> Unit,
    onCancel: () -> Unit,
    onReRecord: () -> Unit
) {
    var name by remember(draft) { mutableStateOf(draft.itemName) }
    var amount by remember(draft) {
        mutableStateOf(draft.amount?.let { Formatters.amount(it) } ?: "")
    }
    var currency by remember(draft) { mutableStateOf(draft.currency ?: defaultCurrency) }
    var categoryId by remember(draft) { mutableStateOf(draft.categoryId) }
    var showCategories by remember { mutableStateOf(false) }

    val selectedCategory = categories.firstOrNull { it.id == categoryId }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp)
            .padding(bottom = 28.dp)
    ) {
        Text("تأكيد المصروف", style = MaterialTheme.typography.headlineSmall)
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "«${draft.rawText}»",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("اسم المصروف") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = amount,
            onValueChange = { amount = it.filter { character -> character.isDigit() || character == '.' } },
            label = { Text("السعر") },
            singleLine = true,
            shape = MaterialTheme.shapes.medium,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(10.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            currencyOptions.forEach { option ->
                MiniChip(
                    text = option,
                    selected = currency == option,
                    onClick = { currency = option }
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))
        Text("القسم", style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(8.dp))

        if (selectedCategory != null) {
            CategoryChip(category = selectedCategory, selected = true) {
                showCategories = !showCategories
            }
        } else {
            Box(
                modifier = Modifier
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .clickable { showCategories = !showCategories }
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                Text(
                    text = "لم يتم التعرّف على القسم — اضغط للاختيار",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        if (showCategories) {
            Spacer(modifier = Modifier.height(12.dp))
            CategoryGrid(
                categories = categories,
                selectedId = categoryId,
                onSelect = { selected ->
                    categoryId = selected.id
                    showCategories = false
                }
            )
        }

        Spacer(modifier = Modifier.height(22.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            OutlinedButton(
                onClick = onReRecord,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.weight(1f)
            ) {
                Text("إعادة التسجيل")
            }
            Button(
                onClick = {
                    onSave(
                        draft.copy(
                            itemName = name.trim().ifBlank { "مصروف" },
                            amount = amount.toDoubleOrNull() ?: 0.0,
                            currency = currency,
                            categoryId = categoryId
                        )
                    )
                },
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ),
                modifier = Modifier.weight(1f)
            ) {
                Text("حفظ")
            }
        }
    }
}

@Composable
private fun MiniChip(
    text: String,
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
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
