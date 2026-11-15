package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch

@Composable
fun FullScreenImageViewerDialog(
    images: List<String?>,
    imagePreset: String,
    productTitle: String,
    initialPage: Int = 0,
    onDismiss: (lastPage: Int) -> Unit
) {
    if (images.isEmpty()) return

    val safeInitialPage = initialPage.coerceIn(0, images.lastIndex)
    val pagerState = rememberPagerState(
        initialPage = safeInitialPage,
        pageCount = { images.size }
    )
    val scope = rememberCoroutineScope()

    var currentPageScale by remember { mutableFloatStateOf(1f) }
    var resetSignal by remember { mutableStateOf(0) }
    var zoomStepDelta by remember { mutableStateOf<Pair<Int, Float>?>(null) }

    Dialog(
        onDismissRequest = { onDismiss(pagerState.currentPage) },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFF0A0D10))
                    .testTag("fullscreen_image_viewer")
            ) {
                // Zoomable Horizontal Carousel Gallery
                HorizontalPager(
                    state = pagerState,
                    userScrollEnabled = currentPageScale <= 1.03f,
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("fullscreen_viewer_pager")
                ) { page ->
                    ZoomableGalleryPage(
                        imageData = images[page],
                        imagePreset = imagePreset,
                        contentDescription = "$productTitle - صورة ${page + 1}",
                        isCurrentPage = pagerState.currentPage == page,
                        resetSignal = resetSignal,
                        zoomStepDelta = zoomStepDelta,
                        onConsumeZoomStep = { zoomStepDelta = null },
                        onScaleChanged = { updatedScale ->
                            if (pagerState.currentPage == page) {
                                currentPageScale = updatedScale
                            }
                        }
                    )
                }

                // Top Overlay Controls
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.TopCenter)
                        .background(Color.Black.copy(alpha = 0.45f))
                        .statusBarsPadding()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.14f)
                        ) {
                            IconButton(
                                onClick = { onDismiss(pagerState.currentPage) },
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("fullscreen_viewer_close_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = "إغلاق عارض الصور",
                                    tint = Color.White
                                )
                            }
                        }
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = productTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${pagerState.currentPage + 1} / ${images.size}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color.White.copy(alpha = 0.78f),
                                modifier = Modifier.testTag("fullscreen_viewer_counter")
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        AnimatedVisibility(
                            visible = currentPageScale > 1.05f,
                            enter = fadeIn(),
                            exit = fadeOut()
                        ) {
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.14f)
                            ) {
                                IconButton(
                                    onClick = { resetSignal++ },
                                    modifier = Modifier
                                        .size(48.dp)
                                        .testTag("fullscreen_viewer_reset_zoom_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.RestartAlt,
                                        contentDescription = "إعادة ضبط التكبير",
                                        tint = Color.White
                                    )
                                }
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.14f)
                        ) {
                            IconButton(
                                onClick = {
                                    zoomStepDelta = (zoomStepDelta?.first ?: 0) + 1 to -0.6f
                                },
                                enabled = currentPageScale > 1.05f,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("fullscreen_viewer_zoom_out_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ZoomOut,
                                    contentDescription = "تصغير",
                                    tint = if (currentPageScale > 1.05f) Color.White else Color.White.copy(alpha = 0.35f)
                                )
                            }
                        }

                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.14f)
                        ) {
                            IconButton(
                                onClick = {
                                    zoomStepDelta = (zoomStepDelta?.first ?: 0) + 1 to 0.6f
                                },
                                enabled = currentPageScale < 3.9f,
                                modifier = Modifier
                                    .size(48.dp)
                                    .testTag("fullscreen_viewer_zoom_in_button")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ZoomIn,
                                    contentDescription = "تكبير",
                                    tint = if (currentPageScale < 3.9f) Color.White else Color.White.copy(alpha = 0.35f)
                                )
                            }
                        }
                    }
                }

                // Bottom Overlay: Thumbnail Strip & Zoom Hint
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .background(Color.Black.copy(alpha = 0.50f))
                        .navigationBarsPadding()
                        .padding(vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (images.size > 1) {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.testTag("fullscreen_viewer_thumbnails")
                        ) {
                            itemsIndexed(images) { index, img ->
                                val selected = pagerState.currentPage == index
                                ProductThumbnail(
                                    imageData = img,
                                    imagePreset = imagePreset,
                                    contentDescription = "معاينة الصورة ${index + 1}",
                                    modifier = Modifier
                                        .size(58.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .border(
                                            width = if (selected) 2.5.dp else 1.dp,
                                            color = if (selected) MaterialTheme.colorScheme.primary else Color.White.copy(alpha = 0.35f),
                                            shape = RoundedCornerShape(12.dp)
                                        )
                                        .clickable {
                                            resetSignal++
                                            scope.launch { pagerState.animateScrollToPage(index) }
                                        }
                                        .testTag("fullscreen_viewer_thumbnail_$index")
                                )
                            }
                        }
                    }

                    Text(
                        text = if (currentPageScale > 1.05f) {
                            "تكبير ${String.format(java.util.Locale.ENGLISH, "%.1fx", currentPageScale)} • اسحب للتحريك أو انقر مرتين للإعادة"
                        } else if (images.size > 1) {
                            "اسحب للتنقل بين الصور • انقر مرتين أو قرّب أصابعك للتكبير"
                        } else {
                            "انقر مرتين أو قرّب أصابعك للتكبير"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.78f),
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun ZoomableGalleryPage(
    imageData: String?,
    imagePreset: String,
    contentDescription: String,
    isCurrentPage: Boolean,
    resetSignal: Int,
    zoomStepDelta: Pair<Int, Float>?,
    onConsumeZoomStep: () -> Unit,
    onScaleChanged: (Float) -> Unit
) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }
    var containerSize by remember { mutableStateOf(IntSize.Zero) }

    fun clampOffset(rawOffset: Offset, targetScale: Float): Offset {
        if (targetScale <= 1.02f || containerSize == IntSize.Zero) return Offset.Zero
        val maxX = (containerSize.width * (targetScale - 1f)) / 2f
        val maxY = (containerSize.height * (targetScale - 1f)) / 2f
        return Offset(
            x = rawOffset.x.coerceIn(-maxX, maxX),
            y = rawOffset.y.coerceIn(-maxY, maxY)
        )
    }

    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage && (scale != 1f || offset != Offset.Zero)) {
            scale = 1f
            offset = Offset.Zero
        } else if (isCurrentPage) {
            onScaleChanged(scale)
        }
    }

    LaunchedEffect(resetSignal) {
        if (isCurrentPage) {
            scale = 1f
            offset = Offset.Zero
            onScaleChanged(1f)
        }
    }

    LaunchedEffect(zoomStepDelta, isCurrentPage) {
        if (isCurrentPage && zoomStepDelta != null) {
            val newScale = (scale + zoomStepDelta.second).coerceIn(1f, 4f)
            scale = newScale
            offset = clampOffset(offset, newScale)
            onScaleChanged(newScale)
            onConsumeZoomStep()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .onSizeChanged { containerSize = it }
            .pointerInput(isCurrentPage) {
                detectTapGestures(
                    onDoubleTap = { tapOffset ->
                        if (scale > 1.2f) {
                            scale = 1f
                            offset = Offset.Zero
                        } else {
                            val targetScale = 2.5f
                            val center = Offset(size.width / 2f, size.height / 2f)
                            val desiredOffset = (center - tapOffset) * (targetScale - 1f)
                            scale = targetScale
                            offset = clampOffset(desiredOffset, targetScale)
                        }
                        onScaleChanged(scale)
                    }
                )
            }
            .pointerInput(isCurrentPage) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    do {
                        val event = awaitPointerEvent()
                        val canceled = event.changes.any { it.isConsumed }
                        if (!canceled) {
                            val pointerCount = event.changes.count { it.pressed }
                            val zoomChange = event.calculateZoom()
                            val panChange = event.calculatePan()
                            if (pointerCount > 1 || scale > 1.03f) {
                                val nextScale = (scale * zoomChange).coerceIn(1f, 4f)
                                val nextOffset = if (nextScale <= 1.03f) {
                                    Offset.Zero
                                } else {
                                    clampOffset(offset + panChange, nextScale)
                                }
                                scale = nextScale
                                offset = nextOffset
                                onScaleChanged(nextScale)
                                event.changes.forEach { if (it.positionChanged()) it.consume() }
                            }
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        ProductThumbnail(
            imageData = imageData,
            imagePreset = imagePreset,
            contentDescription = contentDescription,
            contentScale = ContentScale.Fit,
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                    translationX = offset.x
                    translationY = offset.y
                }
        )
    }
}
