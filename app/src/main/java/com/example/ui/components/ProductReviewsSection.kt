package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.StarHalf
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.RateReview
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.model.ProductReview
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Locale

private val StarGold = Color(0xFFD97706)

@Composable
fun RatingStarsDisplay(
    rating: Double,
    starSize: Dp = 18.dp,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        for (i in 1..5) {
            val icon = when {
                rating >= i - 0.2 -> Icons.Filled.Star
                rating >= i - 0.7 -> Icons.AutoMirrored.Filled.StarHalf
                else -> Icons.Filled.StarBorder
            }
            val tint = if (rating >= i - 0.7) StarGold else MaterialTheme.colorScheme.outline
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(starSize)
            )
        }
    }
}

@Composable
fun ProductRatingPill(
    reviewsState: UiState<List<ProductReview>>,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val isLoading = reviewsState is UiState.Loading
    val reviews = (reviewsState as? UiState.Success)?.data.orEmpty()
    val count = reviews.size
    val average = if (count > 0) reviews.map { it.rating.toDouble() }.average() else 0.0

    val pillShape = RoundedCornerShape(50)
    Surface(
        shape = pillShape,
        color = MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.65f),
        onClick = onClick ?: {},
        enabled = onClick != null,
        modifier = modifier
            .defaultMinSize(minHeight = 40.dp)
            .testTag("product_detail_rating_pill")
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.Star,
                contentDescription = null,
                tint = StarGold,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = when {
                    isLoading -> "جاري تحميل التقييم..."
                    count > 0 -> "${String.format(Locale.US, "%.1f", average)} ($count تقييم)"
                    else -> "لا توجد تقييمات بعد"
                },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

@Composable
fun ProductReviewsSection(
    reviewsState: UiState<List<ProductReview>>,
    currentUserId: String,
    isBusy: Boolean,
    onSubmitReview: (rating: Int, comment: String) -> Unit,
    onDeleteReview: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = Icons.Filled.RateReview,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(20.dp)
            )
            Text(
                text = "التقييمات وآراء العملاء",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
        }

        when (reviewsState) {
            is UiState.Loading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(28.dp))
                }
            }

            is UiState.Error -> {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = reviewsState.message,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            is UiState.Success -> {
                val reviews = reviewsState.data
                val count = reviews.size
                val averageRating = if (count > 0) reviews.map { it.rating.toDouble() }.average() else 0.0
                val userReview = reviews.firstOrNull { it.userId == currentUserId }

                // Average Rating Summary Card
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.65f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reviews_summary_card")
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = if (count > 0) String.format(Locale.US, "%.1f", averageRating) else "0.0",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.testTag("reviews_average_score")
                            )
                            RatingStarsDisplay(rating = averageRating, starSize = 18.dp)
                            Text(
                                text = "$count تقييم",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }

                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            for (star in 5 downTo 1) {
                                val starCount = reviews.count { it.rating.toInt() == star }
                                val progress = if (count > 0) starCount.toFloat() / count.toFloat() else 0f
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "$star",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(12.dp)
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.Star,
                                        contentDescription = null,
                                        tint = StarGold,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    LinearProgressIndicator(
                                        progress = { progress },
                                        color = StarGold,
                                        trackColor = MaterialTheme.colorScheme.surfaceVariant,
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(6.dp)
                                            .clip(RoundedCornerShape(50))
                                    )
                                    Text(
                                        text = starCount.toString(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.width(18.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Review Submission / Edit Form Card
                ReviewSubmissionCard(
                    existingReview = userReview,
                    isBusy = isBusy,
                    onSubmitReview = onSubmitReview,
                    onDeleteReview = onDeleteReview
                )

                // Review List
                if (reviews.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "لا توجد مراجعات بعد. كن أول من يشارك تقييمه وتجربته لهذا المنتج!",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.testTag("reviews_list")
                    ) {
                        reviews.forEach { review ->
                            ReviewItemCard(
                                review = review,
                                isCurrentUser = review.userId == currentUserId
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewSubmissionCard(
    existingReview: ProductReview?,
    isBusy: Boolean,
    onSubmitReview: (rating: Int, comment: String) -> Unit,
    onDeleteReview: () -> Unit
) {
    var selectedRating by remember(existingReview?.id, existingReview?.rating) {
        mutableIntStateOf(existingReview?.rating?.toInt()?.coerceIn(1, 5) ?: 5)
    }
    var commentText by remember(existingReview?.id, existingReview?.comment) {
        mutableStateOf(existingReview?.comment ?: "")
    }
    var validationError by remember { mutableStateOf<String?>(null) }

    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("review_submission_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = if (existingReview == null) "أضف تقييمك ومراجعتك" else "تعديل تقييمك",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )

            // Interactive 5-Star Selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "تقييمك:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    for (star in 1..5) {
                        val isFilled = star <= selectedRating
                        IconButton(
                            onClick = { selectedRating = star },
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("review_star_$star")
                        ) {
                            Icon(
                                imageVector = if (isFilled) Icons.Filled.Star else Icons.Filled.StarBorder,
                                contentDescription = "$star نجوم",
                                tint = if (isFilled) StarGold else MaterialTheme.colorScheme.outline,
                                modifier = Modifier.size(30.dp)
                            )
                        }
                    }
                }
            }

            OutlinedTextField(
                value = commentText,
                onValueChange = { commentText = it.take(500); validationError = null },
                label = { Text("اكتب رأيك في المنتج *") },
                placeholder = { Text("مثال: جودة ممتازة وسرعة في التوصيل...") },
                minLines = 2,
                maxLines = 4,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("review_comment_input")
            )

            if (!validationError.isNullOrBlank()) {
                Text(
                    text = validationError!!,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        val cleaned = commentText.trim()
                        if (cleaned.isEmpty()) {
                            validationError = "يرجى كتابة نص التقييم قبل الإرسال"
                        } else {
                            validationError = null
                            onSubmitReview(selectedRating, cleaned)
                        }
                    },
                    enabled = !isBusy,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .weight(1f)
                        .defaultMinSize(minHeight = 48.dp)
                        .testTag("submit_review_button")
                ) {
                    if (isBusy) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Text(if (existingReview == null) "إرسال التقييم" else "تحديث التقييم")
                    }
                }

                if (existingReview != null) {
                    OutlinedButton(
                        onClick = onDeleteReview,
                        enabled = !isBusy,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        modifier = Modifier
                            .defaultMinSize(minHeight = 48.dp)
                            .testTag("delete_review_button")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.DeleteOutline,
                            contentDescription = "حذف تقييمي",
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ReviewItemCard(
    review: ProductReview,
    isCurrentUser: Boolean
) {
    val dateLabel = remember(review.updatedAt, review.createdAt) {
        val ts = review.updatedAt ?: review.createdAt
        if (ts == null) "" else SimpleDateFormat("dd MMM yyyy", Locale("ar")).format(ts.toDate())
    }
    val initial = review.reviewerName.trim().firstOrNull()?.toString() ?: "م"

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.6f)),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("review_item_${review.id}")
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.secondaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = initial,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = review.reviewerName,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (isCurrentUser) {
                            Surface(
                                color = MaterialTheme.colorScheme.primaryContainer,
                                shape = RoundedCornerShape(50)
                            ) {
                                Text(
                                    text = "تقييمك",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                    if (dateLabel.isNotBlank()) {
                        Text(
                            text = dateLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                RatingStarsDisplay(rating = review.rating.toDouble(), starSize = 16.dp)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f))

            Text(
                text = review.comment,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
