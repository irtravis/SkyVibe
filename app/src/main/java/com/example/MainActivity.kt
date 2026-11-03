package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.ui.SkyWatchScreen
import com.example.ui.SkyWatchViewModel
import com.example.ui.theme.OledBlack
import com.example.ui.theme.SkyWatchTheme

class MainActivity : ComponentActivity() {

    private val viewModel: SkyWatchViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            SkyWatchTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = OledBlack
                ) {
                    SkyWatchScreen(
                        viewModel = viewModel,
                        targetTailFromIntent = intent.getStringExtra("EXTRA_TARGET_TAIL")
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return

        if (intent.getBooleanExtra("EXTRA_TOGGLE_VIEW", false)) {
            viewModel.toggleViewMode()
        }

        val targetTail = intent.getStringExtra("EXTRA_TARGET_TAIL")
        if (!targetTail.isNullOrBlank()) {
            val target = viewModel.aircraftList.value.find { it.tailNumber.equals(targetTail, ignoreCase = true) }
            if (target != null) {
                viewModel.selectAircraft(target)
                viewModel.setViewMode("MAP")
            }
        }
    }
}
