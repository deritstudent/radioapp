package de.radiolumen.hoererfunk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import de.radiolumen.hoererfunk.ui.LumenTheme
import de.radiolumen.hoererfunk.ui.LumenApp
import de.radiolumen.hoererfunk.ui.LumenViewModel
import de.radiolumen.hoererfunk.ui.Paper

class MainActivity : ComponentActivity() {
    private val viewModel: LumenViewModel by viewModels {
        LumenViewModel.factory((application as LumenApplication).graph)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val paper = Paper.toArgb()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.Transparent.toArgb(), paper),
            navigationBarStyle = SystemBarStyle.light(paper, paper),
        )
        setContent {
            LumenTheme {
                LumenApp(viewModel)
            }
        }
    }
}
