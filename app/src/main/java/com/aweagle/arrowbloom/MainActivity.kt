package com.aweagle.arrowbloom

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.lifecycle.viewmodel.compose.viewModel
import com.aweagle.arrowbloom.ui.GameScreen
import com.aweagle.arrowbloom.ui.GameViewModel
import com.aweagle.arrowbloom.ui.theme.ArrowBloomTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArrowBloomTheme {
                val viewModel: GameViewModel = viewModel()
                GameScreen(viewModel = viewModel)
            }
        }
    }
}
