package de.montagezeit.app.ui.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

private val ActionMinHeight = 48.dp

@Composable
fun PrimaryActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = ActionMinHeight),
        enabled = enabled,
        content = content
    )
}

@Composable
fun SecondaryActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = ActionMinHeight),
        enabled = enabled,
        content = content
    )
}

@Composable
fun TertiaryActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    content: @Composable RowScope.() -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier.heightIn(min = ActionMinHeight),
        enabled = enabled,
        content = content
    )
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
