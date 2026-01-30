>
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/**
 * Minimale Höhe für Touch-Targets gemäß Material Design Guidelines
 * für barrierefreie Bedienung (48dp)
 */
private val AccessibleMinHeight = 48.dp

/**
 * Primärer Action-Button mit verbessertem Accessibility-Support
 */
@Composable
fun PrimaryActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = AccessibleMinHeight)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                }
            ),
        enabled = enabled,
        content = content
    )
}

/**
 * Sekundärer Action-Button mit verbessertem Accessibility-Support
 */
@Composable
fun SecondaryActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    content: @Composable RowScope.() -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = AccessibleMinHeight)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                }
            ),
        enabled = enabled,
        content = content
    )
}

/**
 * Tertiärer Action-Button mit verbessertem Accessibility-Support
 */
@Composable
fun TertiaryActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    content: @Composable RowScope.() -> Unit
) {
    TextButton(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = AccessibleMinHeight)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                }
            ),
        enabled = enabled,
        content = content
    )
}

/**
 * Destruktiver Action-Button (Löschen, etc.) mit verbessertem Accessibility-Support
 */
@Composable
fun DestructiveActionButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    contentDescription: String? = null,
    content: @Composable RowScope.() -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = AccessibleMinHeight)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                }
            ),
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        ),
        content = content
    )
}
