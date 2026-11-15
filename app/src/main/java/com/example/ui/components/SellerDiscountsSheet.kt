package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.LocalOffer
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.data.model.CurrencyOption
import com.example.data.model.Discount
import com.example.data.model.DiscountType
import com.example.ui.viewmodel.UiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerDiscountsBottomSheet(
    discountsState: UiState<List<Discount>>,
    defaultCurrency: CurrencyOption,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onCreateDiscount: (
        code: String,
        type: DiscountType,
        value: Double,
        minOrderAmount: Double,
        onSuccess: () -> Unit
    ) -> Unit,
    onToggleActive: (discountId: String, isActive: Boolean) -> Unit,
    onDeleteDiscount: (discountId: String) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var codeInput by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(DiscountType.PERCENTAGE) }
    var valueInput by remember { mutableStateOf("") }
    var minOrderInput by remember { mutableStateOf("") }
    var formError by remember { mutableStateOf<String?>(null) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.LocalOffer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = "إدارة أكواد الخصم",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
            }

            // Create new discount form
            Card(
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "إنشاء كود خصم جديد",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )

                    OutlinedTextField(
                        value = codeInput,
                        onValueChange = { raw ->
                            codeInput = raw.uppercase().filter { it.isLetterOrDigit() || it == '_' || it == '-' }.take(32)
                            formError = null
                        },
                        label = { Text("الكود (إنجليزي/أرقام، مثل SAVE20) *") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("create_discount_code_input")
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        DiscountType.entries.forEach { type ->
                            FilterChip(
                                selected = selectedType == type,
                                onClick = { selectedType = type; formError = null },
                                label = { Text(type.labelAr) }
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = valueInput,
                            onValueChange = { valueInput = it.take(10); formError = null },
                            label = {
                                Text(
                                    if (selectedType == DiscountType.PERCENTAGE) "النسبة (%) *"
                                    else "المبلغ (${defaultCurrency.symbolAr}) *"
                                )
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("create_discount_value_input")
                        )
                        OutlinedTextField(
                            value = minOrderInput,
                            onValueChange = { minOrderInput = it.take(10); formError = null },
                            label = { Text("الحد الأدنى للطلب") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            singleLine = true,
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("create_discount_min_input")
                        )
                    }

                    if (!formError.isNullOrBlank()) {
                        Text(
                            text = formError!!,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Button(
                        onClick = {
                            val code = codeInput.trim().uppercase()
                            val value = valueInput.trim().toDoubleOrNull()
                            val minOrder = minOrderInput.trim().toDoubleOrNull() ?: 0.0
                            when {
                                code.length < 3 ->
                                    formError = "أدخل كوداً من ٣ أحرف أو أرقام إنجليزية على الأقل"
                                value == null || value <= 0.0 ->
                                    formError = "يرجى إدخال قيمة خصم صحيحة"
                                selectedType == DiscountType.PERCENTAGE && value > 100.0 ->
                                    formError = "النسبة المئوية يجب أن لا تتجاوز 100%"
                                minOrder < 0.0 ->
                                    formError = "الحد الأدنى لا يمكن أن يكون سالباً"
                                else -> {
                                    formError = null
                                    onCreateDiscount(code, selectedType, value, minOrder) {
                                        codeInput = ""
                                        valueInput = ""
                                        minOrderInput = ""
                                    }
                                }
                            }
                        },
                        enabled = !isBusy,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("create_discount_submit_button")
                    ) {
                        if (isBusy) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("حفظ كود الخصم")
                        }
                    }
                }
            }

            HorizontalDivider()

            when (discountsState) {
                is UiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                is UiState.Error -> {
                    Text(
                        text = discountsState.message,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                is UiState.Success -> {
                    val discounts = discountsState.data
                    if (discounts.isEmpty()) {
                        Text(
                            text = "لم تقم بإنشاء أي أكواد خصم بعد.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 260.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(discounts, key = { it.id }) { discount ->
                                val dType = DiscountType.fromKey(discount.type)
                                Card(
                                    shape = RoundedCornerShape(14.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 12.dp, vertical = 10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Column(
                                            modifier = Modifier.weight(1f),
                                            verticalArrangement = Arrangement.spacedBy(2.dp)
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Surface(
                                                    color = MaterialTheme.colorScheme.primaryContainer,
                                                    shape = RoundedCornerShape(8.dp)
                                                ) {
                                                    Text(
                                                        text = discount.code,
                                                        style = MaterialTheme.typography.labelLarge,
                                                        fontWeight = FontWeight.Bold,
                                                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                                    )
                                                }
                                                Text(
                                                    text = if (dType == DiscountType.PERCENTAGE) {
                                                        "خصم ${discount.value.toInt()}%"
                                                    } else {
                                                        "خصم ${formatPrice(discount.value, defaultCurrency.code)}"
                                                    },
                                                    style = MaterialTheme.typography.titleSmall,
                                                    fontWeight = FontWeight.SemiBold
                                                )
                                            }
                                            val minLabel = if (discount.minOrderAmount > 0.0) {
                                                " • الحد الأدنى: ${formatPrice(discount.minOrderAmount, defaultCurrency.code)}"
                                            } else ""
                                            Text(
                                                text = "الاستخدامات: ${discount.usageCount}$minLabel",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        Switch(
                                            checked = discount.isActive,
                                            onCheckedChange = { onToggleActive(discount.id, it) }
                                        )
                                        IconButton(
                                            onClick = { onDeleteDiscount(discount.id) },
                                            modifier = Modifier.size(48.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.DeleteOutline,
                                                contentDescription = "حذف ${discount.code}",
                                                tint = MaterialTheme.colorScheme.error
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
