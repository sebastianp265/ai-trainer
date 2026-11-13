package com.github.sebastianp265.aitrainer.shared.design

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.unit.dp

private val THUMB_WIDTH = 4.dp
private val THUMB_MIN_HEIGHT = 32.dp
private val EDGE_GAP = 3.dp
private const val THUMB_ALPHA = 0.5f
private const val FADE_IN_MS = 150
private const val FADE_OUT_MS = 400
private const val FADE_OUT_DELAY_MS = 600

internal fun Modifier.scrollbarWhileScrolling(state: LazyListState): Modifier = composed {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    val alpha by animateFloatAsState(
        targetValue = if (state.isScrollInProgress) 1f else 0f,
        animationSpec = if (state.isScrollInProgress) tween(FADE_IN_MS) else tween(FADE_OUT_MS, delayMillis = FADE_OUT_DELAY_MS),
        label = "scrollbar",
    )
    drawWithContent {
        drawContent()
        val visible = state.layoutInfo.visibleItemsInfo
        if (alpha == 0f || visible.isEmpty()) return@drawWithContent
        val averageItemHeight = visible.sumOf { it.size }.toFloat() / visible.size
        val contentHeight = averageItemHeight * state.layoutInfo.totalItemsCount
        val viewportHeight = size.height
        if (contentHeight <= viewportHeight) return@drawWithContent
        val scrolled = state.firstVisibleItemIndex * averageItemHeight + state.firstVisibleItemScrollOffset
        val thumbHeight = (viewportHeight * viewportHeight / contentHeight).coerceAtLeast(THUMB_MIN_HEIGHT.toPx())
        val progress = (scrolled / (contentHeight - viewportHeight)).coerceIn(0f, 1f)
        drawRoundRect(
            color = color.copy(alpha = THUMB_ALPHA * alpha),
            topLeft = Offset(size.width - THUMB_WIDTH.toPx() - EDGE_GAP.toPx(), progress * (viewportHeight - thumbHeight)),
            size = Size(THUMB_WIDTH.toPx(), thumbHeight),
            cornerRadius = CornerRadius(THUMB_WIDTH.toPx() / 2),
        )
    }
}
