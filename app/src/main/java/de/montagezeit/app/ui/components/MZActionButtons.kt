package de.montagezeit.app.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

private const val BUTTON_PRESS_ANIMATION_MS = 120
private const val BUTTON_PRESSED_SCALE = 0.98f

@Composable
fun PrimaryActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    minHeight: Dp = AccessibilityDefaults.PrimaryButtonHeight,
    shape: Shape? = null,
    colors: ButtonColors? = null,
    content: @Composable RowScope.() -> Unit
) {
    val semanticsModifier = if (!contentDescription.isNullOrBlank()) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }
    val resolvedShape = shape ?: RoundedCornerShape(AccessibilityDefaults.ButtonCornerRadius)
    val resolvedColors = colors ?: ButtonDefaults.buttonColors(
        containerColor = MaterialTheme.colorScheme.primary,
        contentColor = MaterialTheme.colorScheme.onPrimary
    )
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) BUTTON_PRESSED_SCALE else 1f,
        animationSpec = tween(BUTTON_PRESS_ANIMATION_MS),
        label = "primaryButtonScale"
    )

    Button(
        onClick = onClick,
        modifier = semanticsModifier
            .then(modifier)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .heightIn(min = minHeight),
        enabled = enabled && !isLoading,
        shape = resolvedShape,
        colors = resolvedColors,
        interactionSource = interactionSource
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(18.dp)
                    .padding(end = 8.dp),
                strokeWidth = 2.dp,
                color = LocalContentColor.current
            )
        } else {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
        content()
    }
}

@Composable
fun SecondaryActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    minHeight: Dp = AccessibilityDefaults.SecondaryButtonHeight,
    shape: Shape? = null,
    colors: ButtonColors? = null,
    content: @Composable RowScope.() -> Unit
) {
    val semanticsModifier = if (!contentDescription.isNullOrBlank()) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }
    val resolvedShape = shape ?: RoundedCornerShape(AccessibilityDefaults.ButtonCornerRadius)
    val resolvedColors = colors ?: ButtonDefaults.outlinedButtonColors(
        contentColor = MaterialTheme.colorScheme.onSurface
    )
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) BUTTON_PRESSED_SCALE else 1f,
        animationSpec = tween(BUTTON_PRESS_ANIMATION_MS),
        label = "secondaryButtonScale"
    )

    OutlinedButton(
        onClick = onClick,
        modifier = semanticsModifier
            .then(modifier)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .heightIn(min = minHeight),
        enabled = enabled && !isLoading,
        shape = resolvedShape,
        colors = resolvedColors,
        interactionSource = interactionSource
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(18.dp)
                    .padding(end = 8.dp),
                strokeWidth = 2.dp
            )
        } else {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
        content()
    }
}

@Composable
fun TertiaryActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    minHeight: Dp = AccessibilityDefaults.TertiaryButtonHeight,
    shape: Shape? = null,
    colors: ButtonColors? = null,
    content: @Composable RowScope.() -> Unit
) {
    val semanticsModifier = if (!contentDescription.isNullOrBlank()) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }
    val resolvedShape = shape ?: RoundedCornerShape(AccessibilityDefaults.ButtonCornerRadius)
    val resolvedColors = colors ?: ButtonDefaults.textButtonColors()
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) BUTTON_PRESSED_SCALE else 1f,
        animationSpec = tween(BUTTON_PRESS_ANIMATION_MS),
        label = "tertiaryButtonScale"
    )

    TextButton(
        onClick = onClick,
        modifier = semanticsModifier
            .then(modifier)
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .heightIn(min = minHeight),
        enabled = enabled && !isLoading,
        shape = resolvedShape,
        colors = resolvedColors,
        interactionSource = interactionSource
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier
                    .size(18.dp)
                    .padding(end = 8.dp),
                strokeWidth = 2.dp
            )
        } else {
            icon?.let {
                Icon(
                    imageVector = it,
                    contentDescription = null,
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
        content()
    }
}

@Composable
fun DestructiveActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) BUTTON_PRESSED_SCALE else 1f,
        animationSpec = tween(BUTTON_PRESS_ANIMATION_MS),
        label = "destructiveButtonScale"
    )
    Button(
        onClick = onClick,
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .heightIn(min = AccessibilityDefaults.PrimaryButtonHeight),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        ),
        interactionSource = interactionSource,
        content = content
    )
}
