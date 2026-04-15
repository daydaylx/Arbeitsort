package de.montagezeit.app.ui.screen.settings

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import de.montagezeit.app.R

@Composable
fun SettingsDeveloperSection(
    onNavigateToRoute: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Text(
        text = stringResource(R.string.settings_section_advanced_release_hint),
        style = MaterialTheme.typography.bodySmall
    )
}
