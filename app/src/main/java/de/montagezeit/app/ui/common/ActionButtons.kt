package de.montagezeit.app.ui.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.unit.dp

private val ActionMinHeight = 48.dp

@Composable
fun PrimaryActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    minHeight: Dp = ActionMinHeight,
    shape: Shape? = null,
    colors: androidx.compose.material3.ButtonColors? = null,
    content: @Composable RowScope.() -> Unit
) {
    val semanticsModifier = if (!contentDescription.isNullOrBlank()) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }
    val resolvedShape = shape ?: ButtonDefaults.shape
    val resolvedColors = colors ?: ButtonDefaults.buttonColors()

    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = minHeight)
            .then(semanticsModifier),
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
    minHeight: Dp = ActionMinHeight,
    shape: Shape? = null,
    colors: androidx.compose.material3.ButtonColors? = null,
    content: @Composable RowScope.() -> Unit
) {
    val semanticsModifier = if (!contentDescription.isNullOrBlank()) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }
    val resolvedShape = shape ?: ButtonDefaults.outlinedShape
    val resolvedColors = colors ?: ButtonDefaults.outlinedButtonColors()

    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = minHeight)
            .then(semanticsModifier),
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
fun TertiaryActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    minHeight: Dp = ActionMinHeight,
    shape: Shape? = null,
    colors: androidx.compose.material3.ButtonColors? = null,
    content: @Composable RowScope.() -> Unit
) {
    val semanticsModifier = if (!contentDescription.isNullOrBlank()) {
        Modifier.semantics { this.contentDescription = contentDescription }
    } else {
        Modifier
    }
    val resolvedShape = shape ?: ButtonDefaults.textShape
    val resolvedColors = colors ?: ButtonDefaults.textButtonColors()

    TextButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = minHeight)
            .then(semanticsModifier),
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
        modifier = modifier.heightIn(min = ActionMinHeight),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        ),
        content = content
    )
}
