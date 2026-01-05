package de.montagezeit.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import dagger.hilt.android.AndroidEntryPoint
import de.montagezeit.app.ui.theme.MontageZeitTheme
import de.montagezeit.app.ui.navigation.MontageZeitNavGraph

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MontageZeitTheme {
                Surface(
                    color = MaterialTheme.colorScheme.background,
                    modifier = Modifier
                ) {
                    MontageZeitNavGraph()
                }
            }
        }
    }
}
