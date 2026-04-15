package de.montagezeit.app.ui.screen.settings

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import de.montagezeit.app.R
import de.montagezeit.app.ui.components.SecondaryActionButton
import de.montagezeit.app.ui.navigation.DEVELOPER_DIAGNOSTICS_ROUTE

@Composable
fun SettingsDeveloperSection(
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.developer_diagnostics_settings_body),
        style = MaterialTheme.typography.bodySmall
    )
    SecondaryActionButton(
        onClick = { onNavigateToRoute(DEVELOPER_DIAGNOSTICS_ROUTE) },
        modifier = modifier.fillMaxWidth()
    ) {
        Icon(imageVector = Icons.Default.BugReport, contentDescription = null)
        Text(text = stringResource(R.string.developer_diagnostics_open))
    }
}
