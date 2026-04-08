package de.montagezeit.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Applies a staggered fade+slide-up entrance animation.
 * [index] determines the delay: index * [staggerMs].
 */
fun Modifier.staggeredAppear(
    index: Int,
    staggerMs: Long = 60L,
    durationMs: Int = 350
): Modifier = composed {
    val animatable = remember(index) { Animatable(0f) }
    LaunchedEffect(index, staggerMs, durationMs) {
        animatable.snapTo(0f)
        delay(index * staggerMs)
        animatable.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMs, easing = FastOutSlowInEasing)
        )
    }
    val value = animatable.value
    this then Modifier.graphicsLayer {
        alpha = value
        translationY = (1f - value) * 16.dp.toPx()
    }
}
