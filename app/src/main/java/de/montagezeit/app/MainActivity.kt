package de.montagezeit.app

import android.os.Bundle
import android.content.Intent
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.runtime.mutableStateOf
import dagger.hilt.android.AndroidEntryPoint
import de.montagezeit.app.ui.theme.MontageZeitTheme
import de.montagezeit.app.ui.navigation.MontageZeitNavGraph
import de.montagezeit.app.notification.ReminderActions
import java.time.LocalDate

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    private val editRequestDate = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleEditIntent(intent)
        setContent {
            MontageZeitTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier
                ) {
                    MontageZeitNavGraph(
                        editRequestDate = editRequestDate.value,
                        onEditRequestConsumed = { editRequestDate.value = null }
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleEditIntent(intent)
    }

    private fun handleEditIntent(intent: Intent?) {
        val dateStr = when {
            intent == null -> null
            intent.action == ReminderActions.ACTION_EDIT_ENTRY -> intent.getStringExtra(ReminderActions.EXTRA_DATE)
            intent.hasExtra(ReminderActions.EXTRA_DATE) -> intent.getStringExtra(ReminderActions.EXTRA_DATE)
            else -> null
        }
        if (dateStr == null) {
            return
        }
        editRequestDate.value = runCatching { LocalDate.parse(dateStr).toString() }.getOrNull()
    }
}
