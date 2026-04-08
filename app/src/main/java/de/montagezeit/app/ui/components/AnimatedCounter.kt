package de.montagezeit.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle

/**
 * Animates a numeric value smoothly from its current to target value.
 * The [formatter] converts the animated float into a display string.
 */
@Composable
fun AnimatedCounter(
    targetValue: Double,
    formatter: (Double) -> String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.displayLarge,
    color: Color = Color.Unspecified,
    durationMs: Int = 600
) {
    val animatedValue by animateFloatAsState(
        targetValue = targetValue.toFloat(),
        animationSpec = tween(durationMs),
        label = "counter"
    )
    Text(
        text = formatter(animatedValue.toDouble()),
        style = style,
        color = color,
        modifier = modifier
    )
}
