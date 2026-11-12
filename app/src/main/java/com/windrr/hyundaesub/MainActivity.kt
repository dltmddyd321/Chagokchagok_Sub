package com.windrr.hyundaesub

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.windrr.hyundaesub.measure.ArMeasureScreen
import com.windrr.hyundaesub.ui.main.MainIntent
import com.windrr.hyundaesub.ui.main.MainScreen
import com.windrr.hyundaesub.ui.main.MainViewModel
import com.windrr.hyundaesub.ui.theme.HyundaeSubTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HyundaeSubTheme {
                val state by viewModel.state.collectAsStateWithLifecycle()
                if (state.isMeasuring) {
                    ArMeasureScreen(onBack = { viewModel.onIntent(MainIntent.CloseMeasure) })
                } else {
                    MainScreen(state = state, onIntent = viewModel::onIntent)
                }
            }
        }
    }
}
