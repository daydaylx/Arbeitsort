package de.montagezeit.app.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp

enum class SecondaryButtonStyle {
    TONAL,
    OUTLINED
}

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

    Button(
        onClick = onClick,
        modifier = semanticsModifier
            .then(modifier)
            .heightIn(min = minHeight),
        enabled = enabled && !isLoading,
        shape = resolvedShape,
        colors = resolvedColors
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
    style: SecondaryButtonStyle = SecondaryButtonStyle.TONAL,
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
    val buttonContent: @Composable RowScope.() -> Unit = {
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

    when (style) {
        SecondaryButtonStyle.TONAL -> {
            val resolvedColors = colors ?: ButtonDefaults.filledTonalButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer,
                contentColor = MaterialTheme.colorScheme.onSecondaryContainer
            )
            FilledTonalButton(
                onClick = onClick,
                modifier = semanticsModifier
                    .then(modifier)
                    .heightIn(min = minHeight),
                enabled = enabled && !isLoading,
                shape = resolvedShape,
                colors = resolvedColors,
                content = buttonContent
            )
        }
        SecondaryButtonStyle.OUTLINED -> {
            val resolvedColors = colors ?: ButtonDefaults.outlinedButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
            OutlinedButton(
                onClick = onClick,
                modifier = semanticsModifier
                    .then(modifier)
                    .heightIn(min = minHeight),
                enabled = enabled && !isLoading,
                shape = resolvedShape,
                colors = resolvedColors,
                content = buttonContent
            )
        }
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
    val resolvedColors = colors ?: ButtonDefaults.textButtonColors(
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant
    )

    TextButton(
        onClick = onClick,
        modifier = semanticsModifier
            .then(modifier)
            .heightIn(min = minHeight),
        enabled = enabled && !isLoading,
        shape = resolvedShape,
        colors = resolvedColors
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
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = AccessibilityDefaults.PrimaryButtonHeight),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        ),
        content = content
    )
}
